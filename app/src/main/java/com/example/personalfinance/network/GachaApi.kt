package com.example.personalfinance.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

interface GachaApi {
    @GET("api/gacha/attendance/status")
    suspend fun getAttendanceStatus(): Response<Map<String, Boolean>>

    @POST("api/gacha/attendance")
    suspend fun performAttendanceGacha(): Response<Map<String, Any>>

    @GET("api/gacha/user-state")
    suspend fun getUserGachaState(): Response<Map<String, Any>>
}
