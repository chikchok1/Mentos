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


}
