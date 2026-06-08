package com.example.personalfinance.data

import java.util.Locale

object ExpenseCategoryClassifier {
    const val CATEGORY_FOOD_CAFE = "식비/카페"
    const val CATEGORY_LIVING_MART = "생활/마트"
    const val CATEGORY_SHOPPING_ONLINE = "쇼핑/온라인"
    const val CATEGORY_CULTURE_LEISURE = "문화/여가"
    const val CATEGORY_FIXED_SUBSCRIPTION = "고정비/구독"
    const val CATEGORY_HEALTH_MEDICAL = "건강/의료"
    const val CATEGORY_OTHER = "기타"

    val categories = listOf(
        CATEGORY_FOOD_CAFE,
        CATEGORY_LIVING_MART,
        CATEGORY_SHOPPING_ONLINE,
        CATEGORY_CULTURE_LEISURE,
        CATEGORY_FIXED_SUBSCRIPTION,
        CATEGORY_HEALTH_MEDICAL,
        CATEGORY_OTHER
    )

    fun classifyMerchant(merchantName: String): String {
        val normalized = merchantName.normalizeForCategory()
        if (normalized.isBlank()) return CATEGORY_OTHER

        return when {
            foodCafeKeywords.any { normalized.contains(it.normalizeForCategory()) } ->
                CATEGORY_FOOD_CAFE
            livingMartKeywords.any { normalized.contains(it.normalizeForCategory()) } ->
                CATEGORY_LIVING_MART
            shoppingOnlineKeywords.any { normalized.contains(it.normalizeForCategory()) } ->
                CATEGORY_SHOPPING_ONLINE
            cultureLeisureKeywords.any { normalized.contains(it.normalizeForCategory()) } ->
                CATEGORY_CULTURE_LEISURE
            fixedSubscriptionKeywords.any { normalized.contains(it.normalizeForCategory()) } ->
                CATEGORY_FIXED_SUBSCRIPTION
            healthMedicalKeywords.any { normalized.contains(it.normalizeForCategory()) } ->
                CATEGORY_HEALTH_MEDICAL
            else -> CATEGORY_OTHER
        }
    }

    private fun String.normalizeForCategory(): String =
        lowercase(Locale.KOREAN)
            .filterNot { it.isWhitespace() || it == '-' || it == '_' }

    private val foodCafeKeywords = listOf(
        "스타벅스", "starbucks", "커피", "카페", "cafe", "투썸", "이디야", "메가커피",
        "컴포즈", "빽다방", "식당", "김밥", "치킨", "피자", "버거", "맥도날드",
        "롯데리아", "배달의민족", "요기요"
    )

    private val livingMartKeywords = listOf(
        "cu", "gs25", "세븐일레븐", "이마트", "홈플러스", "롯데마트", "마트",
        "편의점", "다이소", "올리브영", "생활"
    )

    private val shoppingOnlineKeywords = listOf(
        "쿠팡", "coupang", "네이버파이낸셜", "네이버페이", "naverpay", "11번가",
        "g마켓", "옥션", "무신사", "지그재그", "에이블리", "온라인", "쇼핑"
    )

    private val cultureLeisureKeywords = listOf(
        "넷플릭스", "netflix", "cgv", "롯데시네마", "메가박스", "영화", "공연",
        "전시", "교보문고", "예스24", "steam", "스팀", "게임", "문화"
    )

    private val fixedSubscriptionKeywords = listOf(
        "정기결제", "구독", "통신요금", "관리비", "보험료", "렌탈", "월정액"
    )

    private val healthMedicalKeywords = listOf(
        "병원", "의원", "약국", "치과", "한의원", "의료", "건강"
    )
}
