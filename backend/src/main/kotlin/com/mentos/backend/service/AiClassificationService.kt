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

        // 3. 카테고리를 찾지 못했으면 원본 텍스트로 AI 요청
        val textToClassify = kakaoCategory ?: safeMerchantName

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
                    return sanitizeClassificationInput(documents[0].path("category_name").asText())
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
        return VALID_CATEGORIES.firstOrNull { sanitizedOutput == it } ?: DEFAULT_CATEGORY
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
