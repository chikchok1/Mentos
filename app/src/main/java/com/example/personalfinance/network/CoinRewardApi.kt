package com.example.personalfinance.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 코인 보상 관련 API.
 * 대응 백엔드: CoinRewardController (/api/coins)
 */
interface CoinRewardApi {

    /**
     * 출석 체크 + 코인 지급.
     * POST /api/coins/attendance
     *
     * Response 키:
     *   success          : Boolean
     *   alreadyChecked   : Boolean  — true이면 오늘 이미 출석 (코인 미지급)
     *   attendanceCoins  : Int      — 출석 보상 코인 (0 또는 20)
     *   budgetRewarded   : Boolean  — 월 예산 성공 보상 지급 여부
     *   budgetRewardCoins: Int      — 예산 성공 보상 코인 (0 또는 200)
     *   totalCoins       : Int      — 지급 후 보유 코인 합계
     */
    @POST("api/coins/attendance")
    suspend fun checkAttendance(): Response<Map<String, Any>>

    /**
     * 오늘 출석 여부 + 현재 보유 코인 조회 (코인 미지급).
     * GET /api/coins/attendance/status
     *
     * Response 키:
     *   checkedToday : Boolean
     *   totalCoins   : Int
     */
    @GET("api/coins/attendance/status")
    suspend fun getAttendanceStatus(): Response<Map<String, Any>>
}
