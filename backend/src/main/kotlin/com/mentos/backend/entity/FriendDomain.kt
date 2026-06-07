package com.mentos.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELED
}

enum class VisibilityScope {
    PRIVATE,
    FRIENDS
}

@Entity
@Table(
    name = "friend_requests",
    indexes = [
        Index(name = "idx_friend_requests_receiver_status", columnList = "receiver_id, status"),
        Index(name = "idx_friend_requests_requester_status", columnList = "requester_id, status")
    ]
)
class FriendRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "requester_id", nullable = false)
    val requesterId: Long,

    @Column(name = "receiver_id", nullable = false)
    val receiverId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: FriendRequestStatus = FriendRequestStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "responded_at")
    var respondedAt: LocalDateTime? = null
) {
    protected constructor() : this(
        requesterId = 0,
        receiverId = 0
    )
}

@Entity
@Table(
    name = "friends",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_friends_user_friend",
            columnNames = ["user_id", "friend_id"]
        )
    ],
    indexes = [
        Index(name = "idx_friends_user_id", columnList = "user_id"),
        Index(name = "idx_friends_friend_id", columnList = "friend_id")
    ]
)
class Friend(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "friend_id", nullable = false)
    val friendId: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    protected constructor() : this(
        userId = 0,
        friendId = 0
    )
}
