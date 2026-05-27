package com.mentos.backend.service

// ── 아이템 등급 ────────────────────────────────────────────────────────────────

enum class GachaGrade(
    val displayName: String,
    val weight: Int,             // 가중치 (합계 100)
    val duplicateCoin: Int       // 중복 시 지급 코인
) {
    COMMON("Common", 70, 2),
    RARE("Rare", 15, 5),
    UNIQUE("Unique", 4, 10),
    LEGENDARY("Legendary", 1, 30)
}

// ── 가챠 아이템 ───────────────────────────────────────────────────────────────

data class GachaItem(
    val id: String,
    val name: String,
    val grade: GachaGrade
)

// ── 가챠 결과 ────────────────────────────────────────────────────────────────

sealed class GachaResult {
    data class NewItem(val item: GachaItem) : GachaResult()
    data class DuplicateCoin(val item: GachaItem, val coins: Int) : GachaResult()
}

// ── 아이템 풀 ────────────────────────────────────────────────────────────────

object GachaItemPool {

    val all: List<GachaItem> = listOf(
        // Common
        GachaItem("common_leather_boots",  "가죽 부츠",  GachaGrade.COMMON),
        GachaItem("common_leather_cap",    "가죽 모자",  GachaGrade.COMMON),
        GachaItem("common_leather_pants",  "가죽 바지",  GachaGrade.COMMON),
        GachaItem("common_leather_tunic",  "가죽 상의",  GachaGrade.COMMON),
        // Rare
        GachaItem("rare_iron_boots",       "철 부츠",    GachaGrade.RARE),
        GachaItem("rare_iron_chestplate",  "철 흉갑",    GachaGrade.RARE),
        GachaItem("rare_iron_helmet",      "철 헬멧",    GachaGrade.RARE),
        GachaItem("rare_iron_leggings",    "철 레깅스",  GachaGrade.RARE),
        // Unique
        GachaItem("unique_golden_boots",      "황금 부츠",   GachaGrade.UNIQUE),
        GachaItem("unique_golden_chestplate", "황금 흉갑",   GachaGrade.UNIQUE),
        GachaItem("unique_golden_helmet",     "황금 헬멧",   GachaGrade.UNIQUE),
        GachaItem("unique_golden_leggings",   "황금 레깅스", GachaGrade.UNIQUE),
        // Legendary
        GachaItem("legendary_diamond_boots",      "다이아몬드 부츠",   GachaGrade.LEGENDARY),
        GachaItem("legendary_diamond_chestplate", "다이아몬드 흉갑",   GachaGrade.LEGENDARY),
        GachaItem("legendary_diamond_helmet",     "다이아몬드 헬멧",   GachaGrade.LEGENDARY),
        GachaItem("legendary_diamond_leggings",   "다이아몬드 레깅스", GachaGrade.LEGENDARY),
    )

    private val byGrade: Map<GachaGrade, List<GachaItem>> =
        GachaGrade.entries.associateWith { grade -> all.filter { it.grade == grade } }

    fun findById(id: String): GachaItem? = all.firstOrNull { it.id == id }

    fun randomOf(grade: GachaGrade): GachaItem =
        byGrade[grade]!!.random()
}

// ── 가챠 엔진 ─────────────────────────────────────────────────────────────────

object GachaEngine {

    fun rollGrade(): GachaGrade {
        val roll = (1..100).random()
        var cumulative = 0
        for (grade in GachaGrade.entries) {
            cumulative += grade.weight
            if (roll <= cumulative) return grade
        }
        return GachaGrade.COMMON
    }

    fun roll(ownedItemIds: Set<String>): GachaResult {
        val grade = rollGrade()
        val item  = GachaItemPool.randomOf(grade)

        return if (item.id in ownedItemIds) {
            GachaResult.DuplicateCoin(item, grade.duplicateCoin)
        } else {
            GachaResult.NewItem(item)
        }
    }
}
