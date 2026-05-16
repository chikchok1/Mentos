package com.example.personalfinance.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserStats(
    val currentLevel: Int = 1,
    val currentXP: Int = 0,
    val nextLevelXP: Int = 1000,
    val thisMonthSpending: Long = 0L
)

class UserStatsStore private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _statsFlow = MutableStateFlow(loadStats())
    val statsFlow: StateFlow<UserStats> = _statsFlow.asStateFlow()

    private fun loadStats(): UserStats {
        return UserStats(
            currentLevel = prefs.getInt(KEY_LEVEL, 1),
            currentXP = prefs.getInt(KEY_XP, 0),
            nextLevelXP = prefs.getInt(KEY_NEXT_XP, 1000),
            thisMonthSpending = prefs.getLong(KEY_SPENDING, 0L)
        )
    }

    fun addExpense(amount: Long) {
        val current = _statsFlow.value
        val newSpending = current.thisMonthSpending + amount
        
        // 0.1% as XP
        val earnedXP = (amount * 0.001).toInt()
        var newXP = current.currentXP + earnedXP
        var newLevel = current.currentLevel
        var newNextXP = current.nextLevelXP

        // Level up logic
        while (newXP >= newNextXP) {
            newXP -= newNextXP
            newLevel++
            newNextXP = (newNextXP * 1.5).toInt() // Next level needs 50% more XP
        }

        val newStats = UserStats(
            currentLevel = newLevel,
            currentXP = newXP,
            nextLevelXP = newNextXP,
            thisMonthSpending = newSpending
        )

        saveStats(newStats)
    }

    private fun saveStats(stats: UserStats) {
        prefs.edit()
            .putInt(KEY_LEVEL, stats.currentLevel)
            .putInt(KEY_XP, stats.currentXP)
            .putInt(KEY_NEXT_XP, stats.nextLevelXP)
            .putLong(KEY_SPENDING, stats.thisMonthSpending)
            .apply()
        
        _statsFlow.value = stats
    }

    companion object {
        private const val PREFS_NAME = "user_stats_prefs"
        private const val KEY_LEVEL = "key_level"
        private const val KEY_XP = "key_xp"
        private const val KEY_NEXT_XP = "key_next_xp"
        private const val KEY_SPENDING = "key_spending"

        @Volatile
        private var instance: UserStatsStore? = null

        fun getInstance(context: Context): UserStatsStore {
            return instance ?: synchronized(this) {
                instance ?: UserStatsStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
