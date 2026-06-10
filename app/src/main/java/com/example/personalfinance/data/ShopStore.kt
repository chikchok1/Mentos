package com.example.personalfinance.data

import android.content.Context
import android.util.Log
import com.example.personalfinance.network.ApiClient
import com.example.personalfinance.network.PurchaseItemRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 상점 구매 결과를 나타내는 sealed class.
 */
sealed class PurchaseResult {
    data class Success(val displayName: String) : PurchaseResult()
    object AlreadyOwned : PurchaseResult()
    object InsufficientCoins : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}

class ShopStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("shop_store", Context.MODE_PRIVATE)
    private val tokenManager = TokenManager(appContext)

    // ── 코인 ──────────────────────────────────────────────────────────────────
    private val _coins = MutableStateFlow(prefs.getInt("coins", 1200))
    val coins: StateFlow<Int> = _coins.asStateFlow()

    fun spendCoins(amount: Int): Boolean {
        if (_coins.value < amount) return false
        val newVal = _coins.value - amount
        prefs.edit().putInt("coins", newVal).apply()
        _coins.value = newVal
        return true
    }

    fun addCoins(amount: Int) {
        val newVal = _coins.value + amount
        prefs.edit().putInt("coins", newVal).apply()
        _coins.value = newVal
    }

    private fun setCoins(amount: Int) {
        prefs.edit().putInt("coins", amount).apply()
        _coins.value = amount
    }

    // ── 보유 아이템 ("folder/filename" 형식) ──────────────────────────────────
    private fun loadOwned(): MutableSet<String> =
        prefs.getStringSet("owned_items", emptySet())!!.toMutableSet()

    private val _ownedItems = MutableStateFlow(loadOwned())
    val ownedItems: StateFlow<Set<String>> = _ownedItems.asStateFlow()

    fun isOwned(folder: String, filename: String) =
        _ownedItems.value.contains("$folder/$filename")

    fun addOwned(folder: String, filename: String) {
        val updated = _ownedItems.value.toMutableSet().apply { add("$folder/$filename") }
        prefs.edit().putStringSet("owned_items", updated).apply()
        _ownedItems.value = updated
    }

    private fun setOwnedItems(items: Collection<String>) {
        // 구형 가챠 아이템(leather/iron/golden/diamond 접두사)은 저장에서 제외
        val filtered = items.filterNot { isLegacyGachaItem(it) }.toMutableSet()
        prefs.edit().putStringSet("owned_items", filtered).apply()
        _ownedItems.value = filtered
    }

    /** 구형 GachaItemPool 아이템 ID 판별 (common_leather_, rare_iron_ 등) */
    private fun isLegacyGachaItem(itemId: String): Boolean {
        val legacyPrefixes = listOf(
            "common_leather_", "rare_iron_", "unique_golden_", "legendary_diamond_"
        )
        return legacyPrefixes.any { itemId.startsWith(it) }
    }

    // ── 서버 연동 구매 ────────────────────────────────────────────────────────
    /**
     * 서버에 구매 요청을 보내고 성공 시 로컬 상태도 동기화한다.
     * @return [PurchaseResult] — UI 계층에서 분기 처리
     */
    suspend fun purchaseWithServer(
        folder: String,
        filename: String,
        price: Int
    ): PurchaseResult {
        val itemId = "$folder/$filename"

        // 클라이언트 사전 검증 (로컬 보유 여부 / 코인 잔액)
        if (_ownedItems.value.contains(itemId)) return PurchaseResult.AlreadyOwned
        if (_coins.value < price) return PurchaseResult.InsufficientCoins

        return try {
            val api = ApiClient.getShopApi(appContext, tokenManager)
            val resp = api.purchaseItem(PurchaseItemRequest(itemId = itemId, price = price))

            when {
                resp.isSuccessful -> {
                    val body = resp.body()
                    if (body != null) {
                        // 서버 응답으로 코인 및 보유 목록 동기화
                        setCoins(body.coins)
                        setOwnedItems(body.ownedItems)
                    } else {
                        // 서버 응답 바디가 없을 경우 로컬 처리 fallback
                        val updated = _ownedItems.value.toMutableSet().apply { add(itemId) }
                        setOwnedItems(updated)
                        val newCoins = (_coins.value - price).coerceAtLeast(0)
                        setCoins(newCoins)
                    }
                    Log.d(TAG, "서버 구매 완료: $itemId")
                    PurchaseResult.Success(filename)
                }
                resp.code() == 409 -> {
                    // 서버에서 이미 보유 중 → 로컬에도 반영
                    val updated = _ownedItems.value.toMutableSet().apply { add(itemId) }
                    setOwnedItems(updated)
                    Log.w(TAG, "이미 보유 중(서버 확인): $itemId")
                    PurchaseResult.AlreadyOwned
                }
                resp.code() == 402 -> {
                    Log.w(TAG, "코인 부족(서버 확인): $itemId")
                    PurchaseResult.InsufficientCoins
                }
                else -> {
                    Log.w(TAG, "구매 실패 HTTP ${resp.code()}: $itemId")
                    PurchaseResult.Error("구매 중 오류가 발생했어요 (${resp.code()})")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "서버 구매 요청 실패 (오프라인?): ${e.message}")
            PurchaseResult.Error("네트워크 오류가 발생했어요")
        }
    }

    // ── 서버 보유 목록 복원 (로그인 후 호출) ──────────────────────────────────
    /**
     * 서버에서 코인 잔액과 보유 아이템 목록을 가져와 로컬에 동기화한다.
     * 로그인 완료 후 또는 화면 진입 시 호출한다.
     */
    suspend fun restoreFromServer() {
        try {
            val api = ApiClient.getShopApi(appContext, tokenManager)
            val resp = api.getShopState()
            if (resp.isSuccessful) {
                val body = resp.body() ?: return
                setCoins(body.coins)
                setOwnedItems(body.ownedItems)
                Log.i(TAG, "상점 상태 복원 완료: 코인=${body.coins}, 보유=${body.ownedItems.size}개")
            } else {
                Log.w(TAG, "상점 상태 복원 실패 (HTTP ${resp.code()})")
            }
        } catch (e: Exception) {
            Log.w(TAG, "상점 상태 복원 예외 (오프라인?): ${e.message}")
        }
    }

    // ── 로그아웃 시 초기화 ────────────────────────────────────────────────────
    fun clearForLogout() {
        prefs.edit()
            .remove("coins")
            .remove("owned_items")
            .apply()
        _coins.value = 0
        _ownedItems.value = mutableSetOf()
    }

    // ── 신상 아이템 ───────────────────────────────────────────────────────────
    val newItems: Set<String> = setOf(
        "t_cat_ears.png",
        "top10_mint_logo_tshirt.png",
        "h_long_blonde.png",
        "a_headset.png",
        "bot7_pink_shorts.png"
    )

    fun isNew(filename: String) = newItems.contains(filename)

    // ── 아이템 가격 (등급별 고정 가격) ────────────────────────────────────────
    fun priceOf(grade: String): Int = when (grade) {
        "legendary" -> 1000
        "unique"    -> 500
        "rare"      -> 300
        "common"    -> 100
        else        -> 150
    }

    companion object {
        private const val TAG = "ShopStore"

        @Volatile private var instance: ShopStore? = null
        fun getInstance(context: Context): ShopStore =
            instance ?: synchronized(this) {
                instance ?: ShopStore(context.applicationContext).also { instance = it }
            }
    }
}
