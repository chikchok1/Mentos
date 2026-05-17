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

    @Test
    fun reclassifyOtherApprovedTransactions_updatesOnlyOtherApprovedTransactions() {
        val cu = transaction(
            id = "notification|1|3200|씨유 개금금강원룸점",
            store = "씨유 개금금강원룸점",
            amount = 3_200L,
            category = ExpenseCategoryClassifier.CATEGORY_OTHER,
            occurredAt = "2026-05-17T13:00"
        )
        val butcher = transaction(
            id = "notification|2|24000|청춘정육",
            store = "청춘정육",
            amount = 24_000L,
            category = ExpenseCategoryClassifier.CATEGORY_OTHER,
            occurredAt = "2026-05-17T14:00"
        )
        val cafe = transaction(
            id = "notification|3|5000|스타벅스",
            store = "스타벅스",
            amount = 5_000L,
            category = ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE,
            occurredAt = "2026-05-17T15:00"
        )

        val result = StoredTransactionReclassifier.reclassifyOtherApprovedTransactions(
            listOf(cu, butcher, cafe)
        )

        assertEquals(2, result.reclassifiedCount)
        assertEquals(ExpenseCategoryClassifier.CATEGORY_LIVING_MART, result.transactions[0].category)
        assertEquals(ExpenseCategoryClassifier.CATEGORY_LIVING_MART, result.transactions[1].category)
        assertEquals(cafe, result.transactions[2])
    }

    @Test
    fun reclassifyOtherApprovedTransactions_preservesTransactionDetails() {
        val original = transaction(
            id = "notification|1|3200|씨유 개금금강원룸점",
            store = "씨유 개금금강원룸점",
            date = "05/17 13:00",
            amount = 3_200L,
            category = ExpenseCategoryClassifier.CATEGORY_OTHER,
            occurredAt = "2026-05-17T13:00",
            source = TransactionSource.NOTIFICATION,
            status = TransactionStatus.APPROVED_RECORDED
        )

        val reclassified = StoredTransactionReclassifier
            .reclassifyOtherApprovedTransactions(listOf(original))
            .transactions
            .single()

        assertEquals(original.id, reclassified.id)
        assertEquals(original.store, reclassified.store)
        assertEquals(original.date, reclassified.date)
        assertEquals(original.amount, reclassified.amount)
        assertEquals(original.occurredAt, reclassified.occurredAt)
        assertEquals(original.source, reclassified.source)
        assertEquals(original.status, reclassified.status)
        assertEquals(ExpenseCategoryClassifier.CATEGORY_LIVING_MART, reclassified.category)
    }

    @Test
    fun calculateCategorySpending_usesReclassifiedTransactionCategories() {
        val transactions = StoredTransactionReclassifier.reclassifyOtherApprovedTransactions(
            listOf(
                transaction(
                    id = "notification|1|3200|씨유 개금금강원룸점",
                    store = "씨유 개금금강원룸점",
                    amount = 3_200L,
                    category = ExpenseCategoryClassifier.CATEGORY_OTHER
                ),
                transaction(
                    id = "notification|2|24000|청춘정육",
                    store = "청춘정육",
                    amount = 24_000L,
                    category = ExpenseCategoryClassifier.CATEGORY_OTHER
                ),
                transaction(
                    id = "notification|3|5000|스타벅스",
                    store = "스타벅스",
                    amount = 5_000L,
                    category = ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE
                )
            )
        ).transactions

        val spending = StoredTransactionReclassifier.calculateCategorySpending(transactions)

        assertEquals(27_200L, spending[ExpenseCategoryClassifier.CATEGORY_LIVING_MART])
        assertEquals(5_000L, spending[ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE])
        assertEquals(0L, spending[ExpenseCategoryClassifier.CATEGORY_OTHER])
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
