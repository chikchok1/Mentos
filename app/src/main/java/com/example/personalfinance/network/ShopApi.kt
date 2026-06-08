package com.example.personalfinance.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class PurchaseItemRequest(
    val itemId: String,  // "grade/categoryFolder/filename" 형식
    val price: Int
)

data class ShopStateResponse(
    val coins: Int,
    val ownedItems: List<String>
)

interface ShopApi {
    /** 아이템 구매 — POST /api/shop/purchase */
    @POST("api/shop/purchase")
    suspend fun purchaseItem(
        @Body req: PurchaseItemRequest
    ): Response<ShopStateResponse>

    /** 보유 아이템 및 코인 잔액 조회 — GET /api/shop/state */
    @GET("api/shop/state")
    suspend fun getShopState(): Response<ShopStateResponse>
}
