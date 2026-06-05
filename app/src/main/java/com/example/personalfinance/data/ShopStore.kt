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

    // 아이템 가격 (등급별 고정 가격)
    fun priceOf(grade: String): Int = when (grade) {
        "legendary" -> 1000
        "unique"    -> 500
        "rare"      -> 300
        "common"    -> 100
        else        -> 150
    }

    companion object {
        @Volatile private var instance: ShopStore? = null
        fun getInstance(context: Context): ShopStore =
            instance ?: synchronized(this) {
                instance ?: ShopStore(context.applicationContext).also { instance = it }
            }
    }
}
