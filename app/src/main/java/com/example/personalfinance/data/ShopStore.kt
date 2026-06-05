package com.example.personalfinance.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShopStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("shop_store", Context.MODE_PRIVATE)

    // 코인
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

    // 보유 아이템 ("folder/filename" 형식)
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

    // 신상 아이템
    val newItems: Set<String> = setOf(
        "t_cat_ears.png",
        "top10_mint_logo_tshirt.png",
        "h_long_blonde.png",
        "a_headset.png",
        "bot7_pink_shorts.png"
    )

    fun isNew(filename: String) = newItems.contains(filename)

    // 아이템 가격
    private val priceMap: Map<String, Int> = mapOf(
        "h_afro_blue.png" to 120,
        "h_dandy.png" to 150,
        "h_half_brown.png" to 140,
        "h_long_blonde.png" to 220,
        "h_messy_brown.png" to 130,
        "h_pony_green.png" to 160,
        "h_short_silver.png" to 180,
        "h_slick_pink.png" to 170,
        "h_sports_red.png" to 150,
        "h_twin_purple.png" to 200,
        "t_beanie.png" to 120,
        "t_cap.png" to 100,
        "t_cat_ears.png" to 200,
        "t_crown.png" to 500,
        "t_halo.png" to 300,
        "t_hard_hat.png" to 150,
        "t_headband_blue.png" to 130,
        "t_ribbon_red.png" to 140,
        "t_straw_hat.png" to 160,
        "t_wizard_hat.png" to 250,
        "a_band.png" to 100,
        "a_blush.png" to 80,
        "a_candy.png" to 90,
        "a_eyepatch.png" to 150,
        "a_freckles.png" to 110,
        "a_glasses.png" to 120,
        "a_headset.png" to 200,
        "a_mask.png" to 130,
        "a_sleep.png" to 90,
        "a_sunglasses.png" to 140
    )

    fun priceOf(filename: String): Int =
        priceMap[filename] ?: when {
            filename.startsWith("top") -> 180
            filename.startsWith("bot") -> 130
            else -> 150
        }

    companion object {
        @Volatile private var instance: ShopStore? = null
        fun getInstance(context: Context): ShopStore =
            instance ?: synchronized(this) {
                instance ?: ShopStore(context.applicationContext).also { instance = it }
            }
    }
}
