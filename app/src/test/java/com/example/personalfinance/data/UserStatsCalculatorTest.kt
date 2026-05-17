package com.example.personalfinance.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UserStatsCalculatorTest {
    @Test
    fun calculateEarnedXP_appliesBaseAmountAndCap() {
        assertEquals(10, UserStatsCalculator.calculateEarnedXP(5_000L))
        assertEquals(20, UserStatsCalculator.calculateEarnedXP(10_000L))
        assertEquals(110, UserStatsCalculator.calculateEarnedXP(120_000L))
    }

    @Test
    fun calculateLevel_usesTotalXPThresholds() {
        assertEquals(1, UserStatsCalculator.calculateLevel(0))
        assertEquals(1, UserStatsCalculator.calculateLevel(99))
        assertEquals(2, UserStatsCalculator.calculateLevel(100))
        assertEquals(3, UserStatsCalculator.calculateLevel(300))
        assertEquals(4, UserStatsCalculator.calculateLevel(700))
        assertEquals(5, UserStatsCalculator.calculateLevel(1_200))
    }

    @Test
    fun nextLevelThreshold_returnsNextRequiredTotalXP() {
        assertEquals(100, UserStatsCalculator.nextLevelThreshold(0))
        assertEquals(300, UserStatsCalculator.nextLevelThreshold(100))
        assertEquals(700, UserStatsCalculator.nextLevelThreshold(300))
        assertEquals(1_200, UserStatsCalculator.nextLevelThreshold(700))
        assertEquals(1_200, UserStatsCalculator.nextLevelThreshold(1_200))
    }
}
