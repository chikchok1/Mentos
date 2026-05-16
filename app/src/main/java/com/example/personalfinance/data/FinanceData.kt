package com.example.personalfinance.data

import androidx.compose.ui.graphics.Color
import com.example.personalfinance.ui.theme.*

// ── Domain Models ─────────────────────────────────────────────────────────────

data class CategoryData(
    val name: String,
    val value: Int,
    val count: Int,
    val percentage: Int,
    val color: Color
)

data class Transaction(
    val store: String,
    val date: String,
    val amount: Int,
    val category: String
)

data class MonthlyData(
    val month: String,
    val amount: Int
)

// ── Sample Data ───────────────────────────────────────────────────────────────

object SampleData {
    val categories = listOf(
        CategoryData("음식",   450_000, 32, 36, CategoryFood),
        CategoryData("쇼핑",   320_000, 18, 26, CategoryShopping),
        CategoryData("게임",   180_000,  8, 14, CategoryGame),
        CategoryData("문화",   150_000, 12, 12, CategoryCulture),
        CategoryData("뷰티",   100_000,  6,  8, CategoryBeauty),
        CategoryData("기타",    48_000,  5,  4, CategoryOther),
    )

    val transactions = listOf(
        Transaction("스타벅스",  "4월 16일",  6_500, "음식"),
        Transaction("GS25",     "4월 16일",  8_200, "음식"),
        Transaction("올리브영", "4월 15일", 42_000, "뷰티"),
        Transaction("Steam",    "4월 14일", 35_000, "게임"),
        Transaction("CGV",      "4월 13일", 15_000, "문화"),
        Transaction("무신사",   "4월 12일", 89_000, "쇼핑"),
        Transaction("교촌치킨", "4월 11일", 23_000, "음식"),
        Transaction("쿠팡",     "4월 10일", 67_000, "쇼핑"),
    )

    val monthly = listOf(
        MonthlyData("1월",   980_000),
        MonthlyData("2월", 1_120_000),
        MonthlyData("3월", 1_420_000),
        MonthlyData("4월", 1_248_000),
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun categoryEmoji(name: String): String = when (name) {
    // 기존 더미 데이터용 이름
    "음식" -> "🍽️"
    "쇼핑" -> "🛍️"
    "게임" -> "🎮"
    "문화" -> "🎬"
    "뷰티" -> "✨"
    // ExpenseCategoryClassifier 카테고리 표시명
    "식비/카페"    -> "🍽️"
    "생활/마트"    -> "🛒"
    "쇼핑/온라인"  -> "🛍️"
    "문화/여가"    -> "🎬"
    "고정비/구독"  -> "📱"
    "건강/의료"    -> "💊"
    else           -> "📦"
}

fun formatWon(amount: Int): String = "₩${String.format("%,d", amount)}"
