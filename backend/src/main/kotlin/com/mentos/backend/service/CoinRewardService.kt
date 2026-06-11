package com.mentos.backend.service

import com.mentos.backend.repository.TransactionRepository
import com.mentos.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

/**
 * 코인 보상 지급 서비스.
 *
 * 지급 종류:
 *  1. 출석 체크  — 하루 1회, 20코인
 *  2. 레벨업     — 레벨 상승 수 × 30코인 (UserStatsService에서 호출)
 *  3. 월 예산 성공 — 지난달 지출 ≤ 지난달 예산인 경우, 이번 달 1회, 200코인
 *
 * 보상 철학:
 *  - 소비 금액이 클수록 코인을 많이 주는 구조 금지
 *  - 기록 습관(출석), 레벨업(XP 활동), 예산 절약(지출 억제) 보상
 */
@Service
class CoinRewardService(
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository
) {
    companion object {
        const val ATTENDANCE_REWARD  = 20   // 출석 체크 1회 보상
        const val LEVEL_UP_REWARD    = 30   // 레벨 1단계 상승당 보상
        const val BUDGET_SUCCESS_REWARD = 200 // 월 예산 성공 보상
    }

    // ── 1. 출석 체크 보상 ─────────────────────────────────────────────────────

    /**
     * 출석 체크 + 코인 지급.
     * - 오늘 이미 출석한 경우 `alreadyChecked = true` 반환
     * - 월 예산 성공 보상 조건도 함께 확인하여 지급
     *
     * @return AttendanceResult
     */
    @Transactional
    fun checkAttendance(userId: Long): AttendanceResult {
        val user  = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }
        val today = LocalDate.now()

        if (user.lastAttendanceDate == today) {
            return AttendanceResult(
                alreadyChecked    = true,
                attendanceCoins   = 0,
                budgetRewardCoins = 0,
                totalCoins        = user.coins,
                budgetRewarded    = false,
            )
        }

        // 출석 코인 지급
        user.coins             += ATTENDANCE_REWARD
        user.lastAttendanceDate = today

        // 월 예산 성공 보상 — 출석 체크 시 함께 확인
        val (budgetRewarded, budgetCoins) = tryGrantBudgetReward(user)

        userRepository.save(user)

        return AttendanceResult(
            alreadyChecked    = false,
            attendanceCoins   = ATTENDANCE_REWARD,
            budgetRewardCoins = budgetCoins,
            totalCoins        = user.coins,
            budgetRewarded    = budgetRewarded,
        )
    }

    /**
     * 출석 여부만 조회 (코인 미지급).
     */
    @Transactional(readOnly = true)
    fun getAttendanceStatus(userId: Long): AttendanceStatus {
        val user  = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }
        val today = LocalDate.now()
        return AttendanceStatus(
            checkedToday = user.lastAttendanceDate == today,
            totalCoins   = user.coins,
        )
    }

    // ── 2. 레벨업 보상 ────────────────────────────────────────────────────────

    /**
     * 레벨업 코인 지급 (UserStatsService.applyTransaction 내부에서 호출).
     * previousLevel < newLevel 인 경우에만 동작.
     * 여러 레벨이 한 번에 오른 경우 상승 단계 수 × LEVEL_UP_REWARD 지급.
     *
     * @return 지급된 코인 (0이면 레벨업 없음)
     */
    @Transactional
    fun grantLevelUpReward(userId: Long, previousLevel: Int, newLevel: Int): Int {
        if (newLevel <= previousLevel) return 0
        val levelsGained = newLevel - previousLevel
        val reward       = levelsGained * LEVEL_UP_REWARD

        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }
        user.coins += reward
        userRepository.save(user)
        return reward
    }

    // ── 3. 월 예산 성공 보상 (내부용) ─────────────────────────────────────────

    /**
     * 지난달 지출 ≤ 지난달 예산이고, 이번 달 보상을 아직 받지 않은 경우 지급.
     * User 객체를 직접 수정하므로 호출부에서 save() 해야 함.
     *
     * @return Pair(지급 여부, 지급 코인)
     */
    private fun tryGrantBudgetReward(user: com.mentos.backend.entity.User): Pair<Boolean, Int> {
        val thisMonth = YearMonth.now()
        val lastMonth = thisMonth.minusMonths(1)

        // 이번 달 이미 받았으면 스킵
        if (user.lastBudgetRewardMonth == thisMonth.toString()) {
            return false to 0
        }

        // 지난달 지출 합계 계산
        val lastMonthStart = lastMonth.atDay(1).atStartOfDay()
        val lastMonthEnd   = thisMonth.atDay(1).atStartOfDay()
        val lastMonthSpending = transactionRepository
            .sumAmountInRange(user.id, lastMonthStart, lastMonthEnd)

        // 지난달 예산 — User.monthlyBudget (현재 설정 기준; 지난달 기준 별도 저장은 없으므로 현재값 사용)
        val budget = user.monthlyBudget

        // 지출이 예산 이하인 경우 보상 지급 (지출이 0이면 제외 — 데이터 없는 신규 유저 방어)
        if (lastMonthSpending <= 0L || lastMonthSpending > budget) {
            return false to 0
        }

        user.coins                 += BUDGET_SUCCESS_REWARD
        user.lastBudgetRewardMonth  = thisMonth.toString()
        return true to BUDGET_SUCCESS_REWARD
    }
}

// ── 결과 DTO ──────────────────────────────────────────────────────────────────

data class AttendanceResult(
    val alreadyChecked: Boolean,
    val attendanceCoins: Int,
    val budgetRewardCoins: Int,
    val totalCoins: Int,
    val budgetRewarded: Boolean,
)

data class AttendanceStatus(
    val checkedToday: Boolean,
    val totalCoins: Int,
)
