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
    @Value("\${app.kakao.rest-api-key}") private val kakaoApiKey: String,
    @Value("\${app.gemini.api-key}") private val geminiApiKey: String,
    private val cacheRepository: CategoryCacheRepository,
    private val objectMapper: ObjectMapper
) {
    private val client = OkHttpClient()

    @Transactional
    fun classifyMerchant(merchantName: String): String {
        // 1. DB 캐시 확인
        val cached = cacheRepository.findByMerchantName(merchantName)
        if (cached != null) {
            return cached.category
        }

        // 2. 카카오맵 API 호출
        val kakaoCategory = getKakaoCategory(merchantName)

        // 3. 카테고리를 찾지 못했으면 원본 텍스트로 AI 요청
        val textToClassify = kakaoCategory ?: merchantName

        // 4. Gemini API로 7개 카테고리 중 하나로 분류
        val finalCategory = askGemini(textToClassify)

        // 5. DB 캐싱 (저장해두면 다음부터는 API 호출 없음)
        cacheRepository.save(CategoryCache(merchantName = merchantName, category = finalCategory))
        
        return finalCategory
    }

    private fun getKakaoCategory(merchantName: String): String? {
        val encodedQuery = URLEncoder.encode(merchantName, "UTF-8")
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
                    return documents[0].path("category_name").asText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun askGemini(text: String): String {
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
                
                val cleanOutput = textOutput.trim()
                val validCategories = listOf("식비/카페", "생활/마트", "쇼핑/온라인", "문화/여가", "고정비/구독", "건강/의료", "기타")
                return validCategories.firstOrNull { cleanOutput.contains(it) } ?: "기타"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "기타"
        }
    }
}
