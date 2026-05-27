package com.example.personalfinance.data

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 가챠 상태를 로컬에 저장/조회하는 저장소.
 *
 * - 출석 가챠: 하루 1회 (자정 00:00 기준 초기화)
 * - 보유 아이템: 획득한 아이템 ID를 쉼표 구분 문자열로 저장
 * - 코인: 코인 캡슐머신에서 사용할 수 있는 코인 수
 * - 사용자 식별: TokenManager.getUserId() 사용 (로그인 시 명시 저장된 값)
 *   → JWT 런타임 파싱 없이 항상 안정적으로 동일 키를 보장
 */
class GachaStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gacha_prefs", Context.MODE_PRIVATE)

    private val tokenManager = TokenManager(context)

    // ── 사용자 고유 키 ────────────────────────────────────────────────────────

    /**
     * 로그인된 사용자의 userId를 기반으로 각 키를 반환.
     * userId가 없으면(비로그인) "anonymous" 사용.
     */
    private fun userId() = tokenManager.getUserId() ?: "anonymous"

    private fun getAttendanceKey() = "${userId()}_attendance_last_used_date"
    private fun getOwnedItemsKey() = "${userId()}_owned_item_ids"
    private fun getCoinKey()       = "${userId()}_gacha_coins"

    // ── 남은 시간 유틸 ────────────────────────────────────────────────────────

    /**
     * 다음 00시까지 남은 시간을 "HH시간 MM분 SS초" 형식 문자열로 반환.
     */
    fun timeUntilMidnight(): String {
        val now         = LocalDateTime.now(ZoneId.systemDefault())
        val midnight    = now.toLocalDate().plusDays(1).atStartOfDay()
        val secondsLeft = ChronoUnit.SECONDS.between(now, midnight).coerceAtLeast(0)

        val h = secondsLeft / 3600
        val m = (secondsLeft % 3600) / 60
        val s = secondsLeft % 60
        return "%02d시간 %02d분 %02d초".format(h, m, s)
    }
}
