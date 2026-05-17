package com.example.personalfinance.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseCategoryClassifierTest {
    @Test
    fun classify_returnsFoodCafeForStarbucks() {
        assertEquals(
            ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE,
            ExpenseCategoryClassifier.classify(merchantName = "스타벅스", rawText = "")
        )
    }

    @Test
    fun classify_returnsLivingMartForCU() {
        assertEquals(
            ExpenseCategoryClassifier.CATEGORY_LIVING_MART,
            ExpenseCategoryClassifier.classify(merchantName = "CU", rawText = "")
        )
    }

    @Test
    fun classify_returnsLivingMartForConvenienceStoreBranches() {
        listOf(
            "씨유 개금금강원룸점",
            "CU 부산대점",
            "GS25 동의대점",
            "이마트24 서면점"
        ).forEach { merchantName ->
            assertEquals(
                merchantName,
                ExpenseCategoryClassifier.CATEGORY_LIVING_MART,
                ExpenseCategoryClassifier.classify(merchantName = merchantName, rawText = "")
            )
        }
    }

    @Test
    fun classify_returnsLivingMartForButcherAndGroceryMerchants() {
        listOf(
            "청춘정육",
            "한우정육점",
            "동네축산"
        ).forEach { merchantName ->
            assertEquals(
                merchantName,
                ExpenseCategoryClassifier.CATEGORY_LIVING_MART,
                ExpenseCategoryClassifier.classify(merchantName = merchantName, rawText = "")
            )
        }
    }

    @Test
    fun classify_returnsShoppingOnlineForCoupang() {
        assertEquals(
            ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE,
            ExpenseCategoryClassifier.classify(merchantName = "쿠팡", rawText = "")
        )
    }

    @Test
    fun classify_returnsShoppingOnlineForNaverFinancial() {
        assertEquals(
            ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE,
            ExpenseCategoryClassifier.classify(merchantName = "네이버파이낸셜", rawText = "")
        )
    }

    @Test
    fun classify_returnsCultureLeisureForNetflix() {
        assertEquals(
            ExpenseCategoryClassifier.CATEGORY_CULTURE_LEISURE,
            ExpenseCategoryClassifier.classify(merchantName = "넷플릭스", rawText = "")
        )
    }

    @Test
    fun classify_returnsFixedSubscriptionForTelecomBill() {
        assertEquals(
            ExpenseCategoryClassifier.CATEGORY_FIXED_SUBSCRIPTION,
            ExpenseCategoryClassifier.classify(merchantName = "KT통신요금", rawText = "")
        )
    }

    @Test
    fun classify_returnsHealthMedicalForPharmacy() {
        assertEquals(
            ExpenseCategoryClassifier.CATEGORY_HEALTH_MEDICAL,
            ExpenseCategoryClassifier.classify(merchantName = "약국", rawText = "")
        )
    }

    @Test
    fun classify_returnsOtherForUnknownMerchant() {
        assertEquals(
            ExpenseCategoryClassifier.CATEGORY_OTHER,
            ExpenseCategoryClassifier.classify(merchantName = "알 수 없는 점포", rawText = "")
        )
    }

    @Test
    fun classify_ignoresCaseAndWhitespace() {
        assertEquals(
            ExpenseCategoryClassifier.CATEGORY_CULTURE_LEISURE,
            ExpenseCategoryClassifier.classify(merchantName = "steam", rawText = "")
        )
        assertEquals(
            ExpenseCategoryClassifier.CATEGORY_FIXED_SUBSCRIPTION,
            ExpenseCategoryClassifier.classify(merchantName = "LG U+", rawText = "")
        )
    }
}
