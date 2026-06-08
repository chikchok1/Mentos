package com.mentos.backend.service

// ── 아이템 등급 ────────────────────────────────────────────────────────────────

enum class GachaGrade(
    val displayName: String,
    val weight: Int,             // 가중치 (합계 100)
    val duplicateCoin: Int       // 중복 시 지급 코인
) {
    COMMON("Common", 55, 2),
    RARE("Rare", 30, 5),
    UNIQUE("Unique", 10, 10),
    LEGENDARY("Legendary", 5, 30)
}

// ── 가챠 아이템 ───────────────────────────────────────────────────────────────
// id 형식: "grade/categoryFolder/filename"  (상점/인벤토리와 동일)
// 예: "common/clothes/top3_red_check_shirt.png"

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

// ── 아이템 풀 (로컬 assets 기반, "grade/folder/filename" 형식) ───────────────

object GachaItemPool {

    val all: List<GachaItem> = listOf(
        // ── Common — 악세서리 ──────────────────────────────────────────────
        GachaItem("common/accessories/a1_hipster_sunglasses.png", "힙스터 선글라스", GachaGrade.COMMON),
        GachaItem("common/accessories/a5_blush.png",              "볼 터치",        GachaGrade.COMMON),
        GachaItem("common/accessories/a_band.png",                "헤어밴드",       GachaGrade.COMMON),
        GachaItem("common/accessories/a_blush.png",               "볼 홍조",        GachaGrade.COMMON),
        GachaItem("common/accessories/a_candy.png",               "사탕",           GachaGrade.COMMON),
        GachaItem("common/accessories/a_glasses.png",             "안경",           GachaGrade.COMMON),
        GachaItem("common/accessories/a_headset.png",             "헤드셋",         GachaGrade.COMMON),
        GachaItem("common/accessories/a_mask.png",                "마스크",         GachaGrade.COMMON),
        GachaItem("common/accessories/a_sunglasses.png",          "선글라스",       GachaGrade.COMMON),

        // ── Common — 의상 ──────────────────────────────────────────────────
        GachaItem("common/clothes/bot10_purple_training.png", "보라 트레이닝 하의", GachaGrade.COMMON),
        GachaItem("common/clothes/bot2_basic_jeans.png",      "기본 청바지",        GachaGrade.COMMON),
        GachaItem("common/clothes/bot4_gray_training.png",    "회색 트레이닝 하의", GachaGrade.COMMON),
        GachaItem("common/clothes/top3_red_check_shirt.png",  "빨간 체크 셔츠",     GachaGrade.COMMON),
        GachaItem("common/clothes/top4_green_sweater.png",    "초록 스웨터",        GachaGrade.COMMON),
        GachaItem("common/clothes/top5_blue_striped_tshirt.png", "파란 줄무늬 티",  GachaGrade.COMMON),
        GachaItem("common/clothes/top9_gray_cardigan.png",    "회색 가디건",        GachaGrade.COMMON),

        // ── Common — 얼굴 ──────────────────────────────────────────────────
        GachaItem("common/faces/f1_default_expression.png", "기본 표정",     GachaGrade.COMMON),
        GachaItem("common/faces/f2_smile_expression.png",   "미소 표정",     GachaGrade.COMMON),
        GachaItem("common/faces/f3_angry_expression.png",   "화난 표정",     GachaGrade.COMMON),
        GachaItem("common/faces/f5_tired_dark_circle.png",  "다크서클 표정", GachaGrade.COMMON),
        GachaItem("common/faces/f8_sparkling_eyes.png",     "반짝이는 눈",   GachaGrade.COMMON),
        GachaItem("common/faces/f_angry.png",               "화남",          GachaGrade.COMMON),
        GachaItem("common/faces/f_dead.png",                "멍한 표정",     GachaGrade.COMMON),
        GachaItem("common/faces/f_slacker.png",             "늘어진 표정",   GachaGrade.COMMON),
        GachaItem("common/faces/f_sparkle.png",             "반짝임",        GachaGrade.COMMON),

        // ── Common — 헤어 ──────────────────────────────────────────────────
        GachaItem("common/hairs/h1_purple_bob_hair.png",   "보라 단발",      GachaGrade.COMMON),
        GachaItem("common/hairs/h2_brown_bob_hair.png",    "갈색 단발",      GachaGrade.COMMON),
        GachaItem("common/hairs/h3_yellow_spiky_hair.png", "노란 뾰족 머리", GachaGrade.COMMON),
        GachaItem("common/hairs/h4_black_parted_hair.png", "검정 가르마",    GachaGrade.COMMON),
        GachaItem("common/hairs/h6_green_perm_hair.png",   "초록 파마",      GachaGrade.COMMON),
        GachaItem("common/hairs/h7_blue_ponytail_hair.png","파란 포니테일",  GachaGrade.COMMON),
        GachaItem("common/hairs/h_dandy.png",              "댄디 머리",      GachaGrade.COMMON),
        GachaItem("common/hairs/h_messy_brown.png",        "흐트러진 갈색",  GachaGrade.COMMON),
        GachaItem("common/hairs/h_short_silver.png",       "짧은 은발",      GachaGrade.COMMON),
        GachaItem("common/hairs/h_slick_pink.png",         "슬릭 핑크",      GachaGrade.COMMON),
        GachaItem("common/hairs/h_sports_red.png",         "스포츠 빨강",    GachaGrade.COMMON),
        GachaItem("common/hairs/h_twin_purple.png",        "보라 트윈테일",  GachaGrade.COMMON),

        // ── Common — 모자 ──────────────────────────────────────────────────
        GachaItem("common/hats/a11_wizard_hat.png",       "마법사 모자", GachaGrade.COMMON),
        GachaItem("common/hats/a12_cat_ear_headband.png", "고양이 귀",   GachaGrade.COMMON),
        GachaItem("common/hats/a6_cute_crown.png",        "귀여운 왕관", GachaGrade.COMMON),
        GachaItem("common/hats/a8_cook_hat.png",          "요리사 모자", GachaGrade.COMMON),

        // ── Rare — 악세서리 ───────────────────────────────────────────────
        GachaItem("rare/accessories/a3_sleep_bubble.png", "수면 말풍선", GachaGrade.RARE),
        GachaItem("rare/accessories/a_freckles.png",      "주근깨",      GachaGrade.RARE),
        GachaItem("rare/accessories/a_sleep.png",         "수면 표시",   GachaGrade.RARE),

        // ── Rare — 의상 ───────────────────────────────────────────────────
        GachaItem("rare/clothes/bot1_red_training.png",       "빨간 트레이닝 하의",  GachaGrade.RARE),
        GachaItem("rare/clothes/bot3_black_slacks.png",       "검정 슬랙스",         GachaGrade.RARE),
        GachaItem("rare/clothes/bot5_khaki_shorts.png",       "카키 반바지",         GachaGrade.RARE),
        GachaItem("rare/clothes/bot8_white_cargo_pants.png",  "흰 카고 바지",        GachaGrade.RARE),
        GachaItem("rare/clothes/top10_mint_logo_tshirt.png",  "민트 로고 티셔츠",    GachaGrade.RARE),
        GachaItem("rare/clothes/top1_yellow_star_tshirt.png", "노란 별 티셔츠",      GachaGrade.RARE),
        GachaItem("rare/clothes/top2_black_hoodie.png",       "검정 후디",           GachaGrade.RARE),
        GachaItem("rare/clothes/top6_white_shirt.png",        "흰 셔츠",             GachaGrade.RARE),
        GachaItem("rare/clothes/top7_purple_collar_tshirt.png","보라 카라 티셔츠",   GachaGrade.RARE),

        // ── Rare — 얼굴 ───────────────────────────────────────────────────
        GachaItem("rare/faces/f4_sad_expression.png", "슬픈 표정",   GachaGrade.RARE),
        GachaItem("rare/faces/f_cry.png",             "우는 표정",   GachaGrade.RARE),
        GachaItem("rare/faces/f_smirk.png",           "득의양양 표정", GachaGrade.RARE),
        GachaItem("rare/faces/f_surprised.png",       "놀란 표정",   GachaGrade.RARE),

        // ── Rare — 헤어 ───────────────────────────────────────────────────
        GachaItem("rare/hairs/h5_pink_bob_hair.png",  "핑크 단발",   GachaGrade.RARE),
        GachaItem("rare/hairs/h9_red_sports_hair.png","빨간 스포츠", GachaGrade.RARE),
        GachaItem("rare/hairs/h_long_blonde.png",     "긴 금발",     GachaGrade.RARE),

        // ── Rare — 모자 ───────────────────────────────────────────────────
        GachaItem("rare/hats/a10_headset.png",   "헤드셋 모자", GachaGrade.RARE),
        GachaItem("rare/hats/a13_blue_durag.png","파란 두래그", GachaGrade.RARE),
        GachaItem("rare/hats/a7_red_cap.png",    "빨간 캡",     GachaGrade.RARE),
        GachaItem("rare/hats/a9_black_beanie.png","검정 비니",  GachaGrade.RARE),

        // ── Unique — 악세서리 ─────────────────────────────────────────────
        GachaItem("unique/accessories/a2_nerd_glasses.png", "너드 안경",    GachaGrade.UNIQUE),
        GachaItem("unique/accessories/a4_white_mask.png",   "흰 마스크",    GachaGrade.UNIQUE),

        // ── Unique — 의상 ─────────────────────────────────────────────────
        GachaItem("unique/clothes/bot6_green_pajama_pants.png",  "초록 파자마 하의", GachaGrade.UNIQUE),
        GachaItem("unique/clothes/bot7_pink_shorts.png",         "핑크 반바지",      GachaGrade.UNIQUE),
        GachaItem("unique/clothes/bot9_autumn_cotton_pants.png", "가을 면 바지",     GachaGrade.UNIQUE),
        GachaItem("unique/clothes/top8_pink_pajama_top.png",     "핑크 파자마 상의", GachaGrade.UNIQUE),

        // ── Unique — 얼굴 ─────────────────────────────────────────────────
        GachaItem("unique/faces/f10_shy_expression.png",  "수줍은 표정", GachaGrade.UNIQUE),
        GachaItem("unique/faces/f7_wink_expression.png",  "윙크 표정",   GachaGrade.UNIQUE),
        GachaItem("unique/faces/f_smile.png",             "미소",        GachaGrade.UNIQUE),

        // ── Unique — 헤어 ─────────────────────────────────────────────────
        GachaItem("unique/hairs/h8_gray_bob_hair.png",  "회색 단발",     GachaGrade.UNIQUE),
        GachaItem("unique/hairs/h_half_brown.png",      "갈색 반묶음",   GachaGrade.UNIQUE),
        GachaItem("unique/hairs/h_pony_green.png",      "초록 포니테일", GachaGrade.UNIQUE),

        // ── Legendary — 악세서리 ─────────────────────────────────────────
        GachaItem("legendary/accessories/a_eyepatch.png", "안대", GachaGrade.LEGENDARY),

        // ── Legendary — 얼굴 ─────────────────────────────────────────────
        GachaItem("legendary/faces/f6_surprised_expression.png", "놀란 표정 (전설)", GachaGrade.LEGENDARY),
        GachaItem("legendary/faces/f9_sinister_smile.png",       "사악한 미소",      GachaGrade.LEGENDARY),
        GachaItem("legendary/faces/f_closed_smile.png",          "눈 감은 미소",     GachaGrade.LEGENDARY),
        GachaItem("legendary/faces/f_wink.png",                  "전설 윙크",        GachaGrade.LEGENDARY),

        // ── Legendary — 헤어 ─────────────────────────────────────────────
        GachaItem("legendary/hairs/h_afro_blue.png", "파란 아프로", GachaGrade.LEGENDARY),
    )

    private val byGrade: Map<GachaGrade, List<GachaItem>> =
        GachaGrade.entries.associateWith { grade -> all.filter { it.grade == grade } }

    fun findById(id: String): GachaItem? = all.firstOrNull { it.id == id }

    fun randomOf(grade: GachaGrade): GachaItem =
        byGrade[grade]!!.random()

    /** 구형 가챠 아이템 ID 패턴 (DB 정리 대상) */
    private val legacyPrefixes = listOf(
        "common_leather_",
        "rare_iron_",
        "unique_golden_",
        "legendary_diamond_"
    )

    fun isLegacyItem(itemId: String): Boolean =
        legacyPrefixes.any { itemId.startsWith(it) }
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
