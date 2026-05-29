package com.mentos.backend.repository

import com.mentos.backend.entity.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional

interface TransactionRepository : JpaRepository<Transaction, Long> {

    /** 특정 유저의 월별 거래 목록 (최신순) */
    fun findByUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(
        userId: Long,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<Transaction>

    /** 특정 유저의 전체 거래 목록 (최신순) */
    fun findByUserIdOrderByOccurredAtDesc(userId: Long): List<Transaction>

    /** userId + clientTransactionId 중복 체크 */
    fun existsByUserIdAndClientTransactionId(userId: Long, clientTransactionId: String): Boolean

    /** userId + clientTransactionId로 단건 조회 */
    fun findByUserIdAndClientTransactionId(userId: Long, clientTransactionId: String): Optional<Transaction>

    /** 월별 카테고리별 합계 — (category, sum) 쌍으로 반환 */
    @Query("""
        SELECT t.category, SUM(t.amount)
        FROM Transaction t
        WHERE t.userId = :userId
          AND t.occurredAt >= :start
          AND t.occurredAt < :end
        GROUP BY t.category
    """)
    fun sumAmountByCategoryInRange(
        @Param("userId") userId: Long,
        @Param("start")  start: LocalDateTime,
        @Param("end")    end: LocalDateTime
    ): List<Array<Any>>

    /** 월별 일자별 합계 — (day, sum) 쌍으로 반환 */
    @Query("""
        SELECT DAY(t.occurredAt), SUM(t.amount)
        FROM Transaction t
        WHERE t.userId = :userId
          AND t.occurredAt >= :start
          AND t.occurredAt < :end
        GROUP BY DAY(t.occurredAt)
        ORDER BY DAY(t.occurredAt)
    """)
    fun sumAmountByDayInRange(
        @Param("userId") userId: Long,
        @Param("start")  start: LocalDateTime,
        @Param("end")    end: LocalDateTime
    ): List<Array<Any>>

    /** 최근 N개월 월별 합계 — (year, month, sum) */
    @Query("""
        SELECT YEAR(t.occurredAt), MONTH(t.occurredAt), SUM(t.amount)
        FROM Transaction t
        WHERE t.userId = :userId
          AND t.occurredAt >= :since
        GROUP BY YEAR(t.occurredAt), MONTH(t.occurredAt)
        ORDER BY YEAR(t.occurredAt), MONTH(t.occurredAt)
    """)
    fun sumAmountByMonthSince(
        @Param("userId") userId: Long,
        @Param("since")  since: LocalDateTime
    ): List<Array<Any>>
}
