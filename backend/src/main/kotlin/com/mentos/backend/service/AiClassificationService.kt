package com.mentos.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mentos.backend.entity.CategoryCache
import com.mentos.backend.repository.CategoryCacheRepository
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLEncoder

@Service
class AiClassificationService(
    @Value("\${app.oauth.kakao.rest-api-key}") private val kakaoApiKey: String,
    @Value("\${app.oauth.gemini.api-key}") private val geminiApiKey: String,
    private val cacheRepository: CategoryCacheRepository,
    private val objectMapper: ObjectMapper
) {
    private val client = OkHttpClient()

    @Transactional
    fun classifyMerchant(merchantName: String): String {
        val safeMerchantName = sanitizeClassificationInput(merchantName)
        if (safeMerchantName.isBlank()) {
            return DEFAULT_CATEGORY
        }

        // 1. DB 캐시 확인
        val cached = cacheRepository.findByMerchantName(safeMerchantName)
        if (cached != null) {
            return normalizeGeminiCategory(cached.category)
        }

        // 2. 카카오맵 API 호출
        val kakaoCategory = getKakaoCategory(safeMerchantName)

        // 3. 상호명 + 카카오 카테고리(있으면)를 함께 전달해 맥락 손실 방지
        val textToClassify = if (kakaoCategory != null) {
            "상호명: $safeMerchantName, 카카오 업종: $kakaoCategory"
        } else {
            "상호명: $safeMerchantName"
        }

        // 4. Gemini API로 7개 카테고리 중 하나로 분류
        val finalCategory = askGemini(textToClassify)

        // 5. DB 캐싱 (저장해두면 다음부터는 API 호출 없음)
        cacheRepository.save(CategoryCache(merchantName = safeMerchantName, category = finalCategory))
        
        return finalCategory
    }

    private fun getKakaoCategory(merchantName: String): String? {
        val encodedQuery = URLEncoder.encode(sanitizeClassificationInput(merchantName), "UTF-8")
        val request = Request.Builder()
            .url("https://dapi.kakao.com/v2/local/search/keyword.json?query=$encodedQuery")
            .addHeader("Authorization", "KakaoAK $kakaoApiKey")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val responseBody = response.body?.string() ?: return null
                val rootNode = objectMapper.readTree(responseBody)
                val documents = rootNode.path("documents")
                if (documents.isArray && !documents.isEmpty) {
                    // 상위 3개 결과의 category_name 빈도를 집계해 다수결로 선택
                    val votes = (0 until minOf(documents.size(), 3))
                        .map { sanitizeClassificationInput(documents[it].path("category_name").asText()) }
                        .filter { it.isNotBlank() }
                        .groupingBy { it }
                        .eachCount()
                    return votes.maxByOrNull { it.value }?.key
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun askGemini(rawText: String): String {
        val text = sanitizeClassificationInput(rawText)
        if (text.isBlank()) {
            return DEFAULT_CATEGORY
        }

        val prompt = """
            다음 장소의 업종 정보("$text")를 보고, 아래 7개의 지출 카테고리 중 가장 알맞은 단 1개만 선택해서 대답해줘. 
            부가적인 설명은 절대 하지 말고 오직 카테고리 이름 1개만 출력해.
            1. 식비/카페
            2. 생활/마트
            3. 쇼핑/온라인
            4. 문화/여가
            5. 고정비/구독
            6. 건강/의료
            7. 기타
        """.trimIndent()

        val jsonBody = """
            {
              "contents": [{
                "parts": [{"text": "${prompt.replace("\n", "\\n").replace("\"", "\\\"")} "}]
              }]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$geminiApiKey")
            .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "기타"
                val responseBody = response.body?.string() ?: return "기타"
                val rootNode = objectMapper.readTree(responseBody)
                val textOutput = rootNode.path("candidates").get(0)?.path("content")?.path("parts")?.get(0)?.path("text")?.asText() ?: "기타"
                
                val cleanOutput = normalizeGeminiCategory(textOutput)
                return cleanOutput
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "기타"
        }
    }

    private fun normalizeGeminiCategory(output: String): String {
        val sanitizedOutput = sanitizeClassificationInput(output)

        // 1단계: 정확 일치
        VALID_CATEGORIES.firstOrNull { sanitizedOutput == it }?.let { return it }

        // 2단계: 포함 일치 (Gemini가 번호나 부가 설명을 붙인 경우 대응)
        VALID_CATEGORIES.firstOrNull { sanitizedOutput.contains(it) }?.let { return it }

        // 3단계: 핵심 키워드 매핑
        val keywordMap = mapOf(
            "식비" to "식비/카페", "카페" to "식비/카페", "음식" to "식비/카페",
            "한식" to "식비/카페", "양식" to "식비/카페", "일식" to "식비/카페",
            "중식" to "식비/카페", "분식" to "식비/카페", "베이커리" to "식비/카페",
            "마트" to "생활/마트", "생활" to "생활/마트", "편의점" to "생활/마트",
            "슈퍼" to "생활/마트", "잡화" to "생활/마트",
            "쇼핑" to "쇼핑/온라인", "온라인" to "쇼핑/온라인", "배달" to "쇼핑/온라인",
            "문화" to "문화/여가", "여가" to "문화/여가", "영화" to "문화/여가",
            "여행" to "문화/여가", "스포츠" to "문화/여가", "레저" to "문화/여가",
            "고정" to "고정비/구독", "구독" to "고정비/구독", "통신" to "고정비/구독",
            "보험" to "고정비/구독", "공과금" to "고정비/구독",
            "건강" to "건강/의료", "의료" to "건강/의료", "병원" to "건강/의료",
            "약국" to "건강/의료", "헬스" to "건강/의료"
        )
        keywordMap.entries.firstOrNull { sanitizedOutput.contains(it.key) }?.let { return it.value }

        return DEFAULT_CATEGORY
    }

    /**
     * 사용자가 카테고리를 수동으로 수정했을 때 캐시를 동기화합니다.
     * TransactionService 등에서 updateCategory 호출 시 함께 호출하세요.
     */
    @Transactional
    fun updateCategoryCache(merchantName: String, newCategory: String) {
        val safeName = sanitizeClassificationInput(merchantName)
        if (safeName.isBlank()) return
        val cached = cacheRepository.findByMerchantName(safeName) ?: return
        if (cached.category != newCategory) {
            cacheRepository.save(cached.copy(category = newCategory))
        }
    }

    private fun sanitizeClassificationInput(value: String): String =
        CONTROL_OR_WHITESPACE_PATTERN
            .replace(value.map { char ->
                if (Character.isISOControl(char)) ' ' else char
            }.joinToString(separator = ""), " ")
            .trim()
            .take(MAX_CLASSIFICATION_INPUT_LENGTH)

    private companion object {
        private const val MAX_CLASSIFICATION_INPUT_LENGTH = 120
        private val CONTROL_OR_WHITESPACE_PATTERN = Regex("""\s+""")
        private val VALID_CATEGORIES = listOf(
            "\uC2DD\uBE44/\uCE74\uD398",
            "\uC0DD\uD65C/\uB9C8\uD2B8",
            "\uC1FC\uD551/\uC628\uB77C\uC778",
            "\uBB38\uD654/\uC5EC\uAC00",
            "\uACE0\uC815\uBE44/\uAD6C\uB3C5",
            "\uAC74\uAC15/\uC758\uB8CC",
            "\uAE30\uD0C0"
        )
        private val DEFAULT_CATEGORY = VALID_CATEGORIES.last()
    }
}
