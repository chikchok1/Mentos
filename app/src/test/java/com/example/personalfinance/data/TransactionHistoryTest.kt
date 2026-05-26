package com.example.personalfinance.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionHistoryTest {
    @Test
    fun transactionPersistencePolicy_allowsOnlyApprovedRecordedStatus() {
        assertTrue(TransactionPersistencePolicy.shouldPersist(TransactionStatus.APPROVED_RECORDED))

        listOf(
            "canceled",
            "parse_failed",
            "needs_review",
            "duplicate_ignored",
            "ignored_package",
            "ignored_non_payment",
            ""
        ).forEach { status ->
            assertFalse(TransactionPersistencePolicy.shouldPersist(status))
        }
    }

    @Test
    fun transactionJsonCodec_roundTripsPaymentDetails() {
        val transaction = Transaction(
            id = "sample|2026-05-17T12:34|5000|Starbucks",
            store = "Starbucks",
            date = "05/17 12:34",
            amount = 5_000L,
            category = ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE,
            status = TransactionStatus.APPROVED_RECORDED,
            source = TransactionSource.SAMPLE,
            occurredAt = "2026-05-17T12:34"
        )

        val decoded = TransactionJsonCodec.decode(
            TransactionJsonCodec.encode(listOf(transaction))
        )

        assertEquals(listOf(transaction), decoded)
    }

    @Test
    fun prependIfAbsent_ignoresDuplicateTransactionId() {
        val transaction = Transaction(
            id = "notification|100|3200|CU",
            store = "CU",
            date = "05/17 13:00",
            amount = 3_200L,
            category = ExpenseCategoryClassifier.CATEGORY_LIVING_MART,
            status = TransactionStatus.APPROVED_RECORDED,
            source = TransactionSource.NOTIFICATION,
            occurredAt = "2026-05-17T13:00"
        )

        val merged = TransactionHistory.prependIfAbsent(
            current = listOf(transaction),
            transaction = transaction
        )

        assertEquals(listOf(transaction), merged)
    }



    private fun transaction(
        id: String,
        store: String,
        amount: Long,
        category: String,
        date: String = "05/17 13:00",
        occurredAt: String = "2026-05-17T13:00",
        source: String = TransactionSource.NOTIFICATION,
        status: String = TransactionStatus.APPROVED_RECORDED
    ): Transaction =
        Transaction(
            id = id,
            store = store,
            date = date,
            amount = amount,
            category = category,
            status = status,
            source = source,
            occurredAt = occurredAt
        )
}
