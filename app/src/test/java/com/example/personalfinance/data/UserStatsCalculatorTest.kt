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
        assertEquals(1, UserStatsCalculator.calculateLevel(49))
        assertEquals(2, UserStatsCalculator.calculateLevel(50))
        assertEquals(3, UserStatsCalculator.calculateLevel(120))
        assertEquals(4, UserStatsCalculator.calculateLevel(220))
        assertEquals(5, UserStatsCalculator.calculateLevel(350))
        assertEquals(10, UserStatsCalculator.calculateLevel(1_300))
        assertEquals(50, UserStatsCalculator.calculateLevel(11_000))
    }

    @Test
    fun nextLevelThreshold_returnsNextRequiredTotalXP() {
        assertEquals(50, UserStatsCalculator.nextLevelThreshold(0))
        assertEquals(120, UserStatsCalculator.nextLevelThreshold(50))
        assertEquals(220, UserStatsCalculator.nextLevelThreshold(120))
        assertEquals(350, UserStatsCalculator.nextLevelThreshold(220))
        assertEquals(11_000, UserStatsCalculator.nextLevelThreshold(11_000))
    }
}
