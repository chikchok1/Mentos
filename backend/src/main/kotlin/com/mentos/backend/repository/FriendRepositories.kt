package com.mentos.backend.repository

import com.mentos.backend.entity.Friend
import com.mentos.backend.entity.FriendRequest
import com.mentos.backend.entity.FriendRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FriendRequestRepository : JpaRepository<FriendRequest, Long> {
    fun findByReceiverIdAndStatusOrderByCreatedAtDesc(
        receiverId: Long,
        status: FriendRequestStatus
    ): List<FriendRequest>

    fun findByRequesterIdOrderByCreatedAtDesc(requesterId: Long): List<FriendRequest>

    @Query(
        """
        SELECT fr FROM FriendRequest fr
        WHERE fr.status = :status
          AND (
            (fr.requesterId = :userId AND fr.receiverId = :otherUserId)
            OR (fr.requesterId = :otherUserId AND fr.receiverId = :userId)
          )
        """
    )
    fun findPendingBetween(
        @Param("userId") userId: Long,
        @Param("otherUserId") otherUserId: Long,
        @Param("status") status: FriendRequestStatus
    ): List<FriendRequest>
}

interface FriendRepository : JpaRepository<Friend, Long> {
    fun existsByUserIdAndFriendId(userId: Long, friendId: Long): Boolean

    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<Friend>

    @Modifying
    fun deleteByUserIdAndFriendId(userId: Long, friendId: Long): Int
}
