package com.example.personalfinance.ui.main

/**
 * 파일명 → 한글 표시명 매핑 테이블.
 * ShopScreen / CharacterLayerTestScreen 양쪽에서 공유.
 */
object ItemNames {

    private val map: Map<String, String> = mapOf(

        // ── 얼굴 ──────────────────────────────────────────────────────────────
        "f1_default_expression.png"  to "기본 표정",
        "f2_smile_expression.png"    to "미소 표정",
        "f3_angry_expression.png"    to "화난 표정",
        "f4_sad_expression.png"      to "슬픈 표정",
        "f5_tired_dark_circle.png"   to "다크서클",
        "f6_surprised_expression.png" to "놀란 표정",
        "f7_wink_expression.png"     to "윙크",
        "f8_sparkling_eyes.png"      to "반짝이는 눈",
        "f9_sinister_smile.png"      to "음흉한 미소",
        "f10_shy_expression.png"     to "수줍은 표정",
        "f_angry.png"                to "화남",
        "f_closed_smile.png"         to "눈감은 미소",
        "f_cry.png"                  to "울음",
        "f_dead.png"                 to "멍한 표정",
        "f_slacker.png"              to "게으른 표정",
        "f_smile.png"                to "미소",
        "f_smirk.png"                to "능글맞은 표정",
        "f_sparkle.png"              to "반짝 눈",
        "f_surprised.png"            to "깜짝 놀람",
        "f_wink.png"                 to "윙크",

        // ── 헤어 ──────────────────────────────────────────────────────────────
        "h1_purple_bob_hair.png"     to "보라 단발",
        "h2_brown_bob_hair.png"      to "갈색 단발",
        "h3_yellow_spiky_hair.png"   to "노랑 뾰족머리",
        "h4_black_parted_hair.png"   to "검정 가르마",
        "h5_pink_bob_hair.png"       to "분홍 단발",
        "h6_green_perm_hair.png"     to "초록 파마",
        "h7_blue_ponytail_hair.png"  to "파랑 포니테일",
        "h8_gray_bob_hair.png"       to "회색 단발",
        "h9_red_sports_hair.png"     to "빨강 스포츠컷",
        "h_afro_blue.png"            to "파랑 아프로",
        "h_dandy.png"                to "댄디 머리",
        "h_half_brown.png"           to "갈색 하프업",
        "h_long_blonde.png"          to "금발 긴머리",
        "h_messy_brown.png"          to "갈색 헝클어진 머리",
        "h_pony_green.png"           to "초록 포니테일",
        "h_short_silver.png"         to "은색 단발",
        "h_slick_pink.png"           to "분홍 올백",
        "h_sports_red.png"           to "빨강 스포츠컷",
        "h_twin_purple.png"          to "보라 양갈래",

        // ── 모자 ──────────────────────────────────────────────────────────────
        "a6_cute_crown.png"          to "귀여운 왕관",
        "a7_red_cap.png"             to "빨강 캡모자",
        "a8_cook_hat.png"            to "요리사 모자",
        "a9_black_beanie.png"        to "검정 비니",
        "a10_headset.png"            to "헤드셋",
        "a11_wizard_hat.png"         to "마법사 모자",
        "a12_cat_ear_headband.png"   to "고양이 귀 머리띠",
        "a13_blue_durag.png"         to "파랑 두랙",

        // ── 상의 ──────────────────────────────────────────────────────────────
        "top1_yellow_star_tshirt.png"   to "노랑 별 티셔츠",
        "top2_black_hoodie.png"         to "검정 후드티",
        "top3_red_check_shirt.png"      to "빨강 체크 셔츠",
        "top4_green_sweater.png"        to "초록 스웨터",
        "top5_blue_striped_tshirt.png"  to "파랑 스트라이프 티셔츠",
        "top6_white_shirt.png"          to "흰 셔츠",
        "top7_purple_collar_tshirt.png" to "보라 카라 티셔츠",
        "top8_pink_pajama_top.png"      to "분홍 파자마 상의",
        "top9_gray_cardigan.png"        to "회색 가디건",
        "top10_mint_logo_tshirt.png"    to "민트 로고 티셔츠",

        // ── 하의 ──────────────────────────────────────────────────────────────
        "bot1_red_training.png"         to "빨강 트레이닝",
        "bot2_basic_jeans.png"          to "기본 청바지",
        "bot3_black_slacks.png"         to "검정 슬랙스",
        "bot4_gray_training.png"        to "회색 트레이닝",
        "bot5_khaki_shorts.png"         to "카키 반바지",
        "bot6_green_pajama_pants.png"   to "초록 파자마 바지",
        "bot7_pink_shorts.png"          to "분홍 반바지",
        "bot8_white_cargo_pants.png"    to "흰 카고 바지",
        "bot9_autumn_cotton_pants.png"  to "가을 면 바지",
        "bot10_purple_training.png"     to "보라 트레이닝",

        // ── 악세서리 ──────────────────────────────────────────────────────────
        "a1_hipster_sunglasses.png"  to "힙스터 선글라스",
        "a2_nerd_glasses.png"        to "너드 안경",
        "a3_sleep_bubble.png"        to "수면 말풍선",
        "a4_white_mask.png"          to "흰 마스크",
        "a5_blush.png"               to "볼터치",
        "a_band.png"                 to "머리띠",
        "a_blush.png"                to "볼터치",
        "a_candy.png"                to "캔디",
        "a_eyepatch.png"             to "안대",
        "a_freckles.png"             to "주근깨",
        "a_glasses.png"              to "안경",
        "a_headset.png"              to "헤드셋",
        "a_mask.png"                 to "마스크",
        "a_sleep.png"                to "수면 말풍선",
        "a_sunglasses.png"           to "선글라스",
    )

    /** 파일명(확장자 포함)을 한글 이름으로 변환. 매핑 없으면 null 반환. */
    fun of(filename: String?): String? = filename?.let { map[it] }

    /**
     * 파일명 → 한글 이름 변환. 매핑 없으면 기존 영문 파싱 결과로 폴백.
     * null(없음 선택)이면 "없음" 반환.
     */
    fun display(filename: String?): String {
        if (filename == null) return "없음"
        return map[filename] ?: fallback(filename)
    }

    /** 매핑 테이블에 없는 파일명을 위한 폴백 — 기존 로직 유지 */
    private fun fallback(filename: String): String =
        filename
            .removeSuffix(".png")
            .replace(Regex("^(top|bot|h|a|f)\\d+_"), "")
            .replace(Regex("^[a-z]_"), "")
            .replace("_", " ")
            .replaceFirstChar { it.uppercase() }
            .ifBlank { filename.removeSuffix(".png") }
}
