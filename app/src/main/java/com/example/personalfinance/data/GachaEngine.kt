package com.example.personalfinance.data

import com.example.personalfinance.R

// ── 아이템 등급 ────────────────────────────────────────────────────────────────

enum class GachaGrade(
    val displayName: String,
    val weight: Int,             // 가중치 (합계 100)
    val duplicateCoin: Int,      // 중복 시 지급 코인
    val gradientColors: List<Long>,
) {
    COMMON(
        displayName     = "Common",
        weight          = 70,
        duplicateCoin   = 2,
        gradientColors  = listOf(0xFF9E9E9E, 0xFFBDBDBD),
    ),
    RARE(
        displayName     = "Rare",
        weight          = 15,
        duplicateCoin   = 5,
        gradientColors  = listOf(0xFF1565C0, 0xFF42A5F5),
    ),
    UNIQUE(
        displayName     = "Unique",
        weight          = 4,
        duplicateCoin   = 10,
        gradientColors  = listOf(0xFF6A1B9A, 0xFFCE93D8),
    ),
    LEGENDARY(
        displayName     = "Legendary",
        weight          = 1,
        duplicateCoin   = 30,
        gradientColors  = listOf(0xFFE65100, 0xFFFFD54F),
    ),
}

// ── 가챠 아이템 ───────────────────────────────────────────────────────────────

data class GachaItem(
    val id: String,
    val name: String,
    val grade: GachaGrade,
    val drawableResId: Int,           // R.drawable.xxx
    val imageUrl: String = "",        // 추후 Google Cloud Storage URL 사용
)

// ── 가챠 결과 ────────────────────────────────────────────────────────────────

sealed class GachaResult {
    /** 신규 아이템 획득 */
    data class NewItem(val item: GachaItem) : GachaResult()

    /** 중복 아이템 → 코인 보상 */
    data class DuplicateCoin(val item: GachaItem, val coins: Int) : GachaResult()
}

// ── 아이템 풀 ────────────────────────────────────────────────────────────────

object GachaItemPool {

    val all: List<GachaItem> = listOf(
        // ── Common (가죽 장비) ──────────────────────────────────────────────
        GachaItem("common_leather_boots",  "가죽 부츠",  GachaGrade.COMMON,    R.drawable.common_leather_boots),
        GachaItem("common_leather_cap",    "가죽 모자",  GachaGrade.COMMON,    R.drawable.common_leather_cap),
        GachaItem("common_leather_pants",  "가죽 바지",  GachaGrade.COMMON,    R.drawable.common_leather_pants),
        GachaItem("common_leather_tunic",  "가죽 상의",  GachaGrade.COMMON,    R.drawable.common_leather_tunic),

        // ── Rare (철 장비) ─────────────────────────────────────────────────
        GachaItem("rare_iron_boots",       "철 부츠",    GachaGrade.RARE,      R.drawable.rare_iron_boots),
        GachaItem("rare_iron_chestplate",  "철 흉갑",    GachaGrade.RARE,      R.drawable.rare_iron_chestplate),
        GachaItem("rare_iron_helmet",      "철 헬멧",    GachaGrade.RARE,      R.drawable.rare_iron_helmet),
        GachaItem("rare_iron_leggings",    "철 레깅스",  GachaGrade.RARE,      R.drawable.rare_iron_leggings),

        // ── Unique (황금 장비) ─────────────────────────────────────────────
        GachaItem("unique_golden_boots",      "황금 부츠",   GachaGrade.UNIQUE,  R.drawable.unique_golden_boots),
        GachaItem("unique_golden_chestplate", "황금 흉갑",   GachaGrade.UNIQUE,  R.drawable.unique_golden_chestplate),
        GachaItem("unique_golden_helmet",     "황금 헬멧",   GachaGrade.UNIQUE,  R.drawable.unique_golden_helmet),
        GachaItem("unique_golden_leggings",   "황금 레깅스", GachaGrade.UNIQUE,  R.drawable.unique_golden_leggings),

        // ── Legendary (다이아몬드 장비) ────────────────────────────────────
        GachaItem("legendary_diamond_boots",      "다이아몬드 부츠",   GachaGrade.LEGENDARY, R.drawable.legendary_diamond_boots),
        GachaItem("legendary_diamond_chestplate", "다이아몬드 흉갑",   GachaGrade.LEGENDARY, R.drawable.legendary_diamond_chestplate),
        GachaItem("legendary_diamond_helmet",     "다이아몬드 헬멧",   GachaGrade.LEGENDARY, R.drawable.legendary_diamond_helmet),
        GachaItem("legendary_diamond_leggings",   "다이아몬드 레깅스", GachaGrade.LEGENDARY, R.drawable.legendary_diamond_leggings),
    )

    /** 등급별 아이템 목록 */
    private val byGrade: Map<GachaGrade, List<GachaItem>> =
        GachaGrade.entries.associateWith { grade -> all.filter { it.grade == grade } }

    /** ID → 아이템 조회 */
    fun findById(id: String): GachaItem? = all.firstOrNull { it.id == id }

    /** 등급 내 무작위 아이템 1개 */
    fun randomOf(grade: GachaGrade): GachaItem =
        byGrade[grade]!!.random()
}

// ── 가챠 엔진 ─────────────────────────────────────────────────────────────────

// (로컬 확률 계산 및 뽑기 로직은 백엔드로 이전되었습니다. 여기서는 데이터 정의만 제공합니다.)
