package com.mentos.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "notification_parse_logs",
    indexes = [
        Index(name = "idx_notification_parse_logs_user_created", columnList = "user_id, created_at"),
        Index(name = "idx_notification_parse_logs_user_received", columnList = "user_id, received_at"),
        Index(name = "idx_notification_parse_logs_status", columnList = "status")
    ]
)
class NotificationParseLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "diagnostic_id", nullable = false, length = 512)
    val diagnosticId: String,

    @Column(name = "package_name", nullable = false, length = 128)
    val packageName: String,

    @Column(nullable = true, length = 512)
    val title: String? = null,

    @Lob
    @Column(nullable = true)
    val rawText: String? = null,

    @Column(nullable = false, length = 40)
    val status: String,

    @Column(name = "failure_reason", nullable = true, length = 512)
    val failureReason: String? = null,

    @Column(name = "parsed_amount", nullable = true)
    val parsedAmount: Long? = null,

    @Column(name = "parsed_merchant", nullable = true, length = 255)
    val parsedMerchant: String? = null,

    @Column(name = "parsed_occurred_at", nullable = true)
    val parsedOccurredAt: LocalDateTime? = null,

    @Column(name = "client_transaction_id", nullable = true, length = 512)
    val clientTransactionId: String? = null,

    @Column(name = "received_at", nullable = false)
    val receivedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    protected constructor() : this(
        userId = 0,
        diagnosticId = "",
        packageName = "",
        status = "RECEIVED"
    )
}
