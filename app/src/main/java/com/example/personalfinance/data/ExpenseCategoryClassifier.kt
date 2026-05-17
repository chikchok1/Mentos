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

    private val rules = listOf(
        CATEGORY_FOOD_CAFE to listOf(
            "스타벅스",
            "메가커피",
            "이디야",
            "투썸",
            "컴포즈커피",
            "빽다방",
            "배민",
            "배달의민족",
            "요기요",
            "쿠팡이츠",
            "맥도날드",
            "버거킹",
            "롯데리아",
            "식당",
            "카페"
        ),
        CATEGORY_LIVING_MART to listOf(
            "CU",
            "GS25",
            "세븐일레븐",
            "이마트",
            "홈플러스",
            "롯데마트",
            "다이소",
            "마트",
            "편의점"
        ),
        CATEGORY_SHOPPING_ONLINE to listOf(
            "쿠팡",
            "네이버페이",
            "네이버파이낸셜",
            "11번가",
            "G마켓",
            "옥션",
            "무신사",
            "지그재그",
            "에이블리",
            "오늘의집",
            "쇼핑"
        ),
        CATEGORY_CULTURE_LEISURE to listOf(
            "CGV",
            "롯데시네마",
            "메가박스",
            "넷플릭스",
            "티빙",
            "웨이브",
            "멜론",
            "스포티파이",
            "Steam",
            "스팀",
            "PC방",
            "노래방",
            "게임"
        ),
        CATEGORY_FIXED_SUBSCRIPTION to listOf(
            "SKT",
            "KT",
            "LGU",
            "LG U+",
            "통신요금",
            "정기결제",
            "자동납부",
            "보험",
            "관리비",
            "구독"
        ),
        CATEGORY_HEALTH_MEDICAL to listOf(
            "약국",
            "병원",
            "의원",
            "치과",
            "한의원",
            "올리브영",
            "헬스",
            "건강"
        )
    )

    fun classify(merchantName: String, rawText: String): String {
        val normalizedInput = normalize("$merchantName $rawText")
        return rules.firstOrNull { (_, keywords) ->
            keywords.any { keyword -> normalizedInput.contains(normalize(keyword)) }
        }?.first ?: CATEGORY_OTHER
    }

    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT).filterNot { it.isWhitespace() }
}
