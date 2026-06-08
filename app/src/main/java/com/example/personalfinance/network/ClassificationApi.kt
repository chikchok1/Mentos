package com.example.personalfinance.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ClassificationApi {
    @GET("api/classification/categorize")
    suspend fun categorizeMerchant(
        @Query("merchantName") merchantName: String,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("isOnline") isOnline: Boolean? = null
    ): Response<Map<String, String>>
}
