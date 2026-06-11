package com.mentos.backend.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "transactions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_transactions_user_client_transaction",
            columnNames = ["userId", "clientTransactionId"]
        )
    ],
    indexes = [
        Index(name = "idx_transactions_user_occurred", columnList = "userId, occurredAt")
    ]
)
class Transaction(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val amount: Long,

    @Column(nullable = false)
    var merchantName: String,

    @Column(nullable = false)
    var category: String,

    /** 실제 결제 발생 시각 */
    @Column(nullable = false)
    val occurredAt: LocalDateTime,

    @Column(nullable = false)
    val source: String = "MANUAL",

    @Column(nullable = true, length = 512)
    val clientTransactionId: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

) {
    protected constructor() : this(
        userId       = 0,
        amount       = 0,
        merchantName = "",
        category     = "",
        occurredAt   = LocalDateTime.now()
    )
}
