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
        Index(name = "idx_notification_parse_logs_user_created", columnList = "userId, createdAt"),
        Index(name = "idx_notification_parse_logs_status", columnList = "status")
    ]
)
class NotificationParseLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false, length = 512)
    val diagnosticId: String,

    @Column(nullable = false, length = 128)
    val packageName: String,

    @Column(nullable = true, length = 512)
    val title: String? = null,

    @Lob
    @Column(nullable = true)
    val rawText: String? = null,

    @Column(nullable = false, length = 40)
    val status: String,

    @Column(nullable = true, length = 512)
    val failureReason: String? = null,

    @Column(nullable = true)
    val amount: Long? = null,

    @Column(nullable = true, length = 255)
    val merchantName: String? = null,

    @Column(nullable = true)
    val occurredAt: LocalDateTime? = null,

    @Column(nullable = true, length = 512)
    val clientTransactionId: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    protected constructor() : this(
        userId = 0,
        diagnosticId = "",
        packageName = "",
        status = "RECEIVED"
    )
}
