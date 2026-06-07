package com.example.personalfinance.data

// ── 아이템 등급 ────────────────────────────────────────────────────────────────

enum class GachaGrade(
    val displayName: String,
    val weight: Int,             // 가중치 (합계 100)
    val duplicateCoin: Int,      // 중복 시 지급 코인
    val gradientColors: List<Long>,
) {
    COMMON(
        displayName    = "Common",
        weight         = 55,
        duplicateCoin  = 2,
        gradientColors = listOf(0xFF9E9E9E, 0xFFBDBDBD),
    ),
    RARE(
        displayName    = "Rare",
        weight         = 30,
        duplicateCoin  = 5,
        gradientColors = listOf(0xFF1565C0, 0xFF42A5F5),
    ),
    UNIQUE(
        displayName    = "Unique",
        weight         = 10,
        duplicateCoin  = 10,
        gradientColors = listOf(0xFF6A1B9A, 0xFFCE93D8),
    ),
    LEGENDARY(
        displayName    = "Legendary",
        weight         = 5,
        duplicateCoin  = 30,
        gradientColors = listOf(0xFFE65100, 0xFFFFD54F),
    ),
}

// ── 가챠 아이템 ───────────────────────────────────────────────────────────────
// id 형식: "grade/categoryFolder/filename"  (상점/인벤토리와 동일)
// 예: "common/clothes/top3_red_check_shirt.png"
// 이미지는 assets 경로 "character_layers/{id}" 로 렌더링됩니다.

data class GachaItem(
    val id: String,
    val name: String,
    val grade: GachaGrade,
)

// ── 가챠 결과 ────────────────────────────────────────────────────────────────

sealed class GachaResult {
    /** 신규 아이템 획득 */
    data class NewItem(val item: GachaItem) : GachaResult()

    /** 중복 아이템 → 코인 보상 */
    data class DuplicateCoin(val item: GachaItem, val coins: Int) : GachaResult()
}

// ── 아이템 등급 파싱 유틸 ────────────────────────────────────────────────────

/**
 * 아이템 ID("grade/folder/filename")에서 GachaGrade를 파싱한다.
 * 알 수 없는 등급이면 COMMON을 반환한다.
 */
fun gradeFromItemId(itemId: String): GachaGrade {
    val prefix = itemId.substringBefore("/")
    return when (prefix) {
        "legendary" -> GachaGrade.LEGENDARY
        "unique"    -> GachaGrade.UNIQUE
        "rare"      -> GachaGrade.RARE
        else        -> GachaGrade.COMMON
    }
}

// ── GachaItemPool은 서버에서 관리합니다. ──────────────────────────────────────
// 앱은 서버 응답으로 받은 itemId와 grade를 활용하여 결과를 표시하고,
// ShopStore에 아이템을 추가합니다.
// 아이템 이미지: assets/character_layers/{itemId} 경로로 CharacterLayerPreview 사용.
