package com.mentos.backend.service

import com.mentos.backend.dto.FriendRequestCreateRequest
import com.mentos.backend.dto.FriendRequestResponse
import com.mentos.backend.entity.Friend
import com.mentos.backend.entity.FriendRequest
import com.mentos.backend.entity.FriendRequestStatus
import com.mentos.backend.entity.VisibilityScope
import com.mentos.backend.repository.FriendRepository
import com.mentos.backend.repository.FriendRequestRepository
import com.mentos.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class FriendRequestService(
    private val userRepository: UserRepository,
    private val friendRepository: FriendRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val characterService: CharacterService
) {
    @Transactional
    fun create(userId: Long, req: FriendRequestCreateRequest): FriendRequestResponse {
        val receiverId = req.receiverId
        require(userId != receiverId) { "자기 자신에게 친구 요청을 보낼 수 없습니다." }
        require(userRepository.existsById(receiverId)) { "요청 대상 사용자를 찾을 수 없습니다." }
        require(!friendRepository.existsByUserIdAndFriendId(userId, receiverId)) { "이미 친구입니다." }

        val pending = friendRequestRepository.findPendingBetween(
            userId,
            receiverId,
            FriendRequestStatus.PENDING
        )
        if (pending.isNotEmpty()) {
            val reverse = pending.firstOrNull { it.requesterId == receiverId && it.receiverId == userId }
            if (reverse != null) {
                acceptInternal(reverse, userId)
                return reverse.toResponse()
            }
            throw IllegalStateException("이미 대기 중인 친구 요청이 있습니다.")
        }

        val saved = friendRequestRepository.save(
            FriendRequest(
                requesterId = userId,
                receiverId = receiverId
            )
        )
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun received(userId: Long): List<FriendRequestResponse> =
        friendRequestRepository
            .findByReceiverIdAndStatusOrderByCreatedAtDesc(userId, FriendRequestStatus.PENDING)
            .map { it.toResponse() }

    @Transactional(readOnly = true)
    fun sent(userId: Long): List<FriendRequestResponse> =
        friendRequestRepository
            .findByRequesterIdOrderByCreatedAtDesc(userId)
            .map { it.toResponse() }

    @Transactional
    fun accept(userId: Long, requestId: Long): FriendRequestResponse {
        val request = friendRequestRepository.findById(requestId)
            .orElseThrow { NoSuchElementException("친구 요청을 찾을 수 없습니다.") }
        acceptInternal(request, userId)
        return request.toResponse()
    }

    @Transactional
    fun reject(userId: Long, requestId: Long): FriendRequestResponse {
        val request = friendRequestRepository.findById(requestId)
            .orElseThrow { NoSuchElementException("친구 요청을 찾을 수 없습니다.") }
        require(request.receiverId == userId) { "받은 요청만 거절할 수 있습니다." }
        require(request.status == FriendRequestStatus.PENDING) { "대기 중인 요청만 거절할 수 있습니다." }

        request.status = FriendRequestStatus.REJECTED
        request.respondedAt = LocalDateTime.now()
        return request.toResponse()
    }

    private fun acceptInternal(request: FriendRequest, receiverUserId: Long) {
        require(request.receiverId == receiverUserId) { "받은 요청만 수락할 수 있습니다." }
        require(request.status == FriendRequestStatus.PENDING) { "대기 중인 요청만 수락할 수 있습니다." }

        request.status = FriendRequestStatus.ACCEPTED
        request.respondedAt = LocalDateTime.now()
        createFriendPair(request.requesterId, request.receiverId)
    }

    private fun createFriendPair(userA: Long, userB: Long) {
        if (!friendRepository.existsByUserIdAndFriendId(userA, userB)) {
            friendRepository.save(Friend(userId = userA, friendId = userB))
        }
        if (!friendRepository.existsByUserIdAndFriendId(userB, userA)) {
            friendRepository.save(Friend(userId = userB, friendId = userA))
        }
    }

    private fun FriendRequest.toResponse(): FriendRequestResponse {
        val users = userRepository.findAllById(listOf(requesterId, receiverId)).associateBy { it.id }
        val requester = users[requesterId]
        val receiver = users[receiverId]
        val requesterVisible = requester?.characterVisibility == VisibilityScope.FRIENDS &&
            friendRepository.existsByUserIdAndFriendId(receiverId, requesterId)
        val receiverVisible = receiver?.characterVisibility == VisibilityScope.FRIENDS &&
            friendRepository.existsByUserIdAndFriendId(requesterId, receiverId)
        return FriendRequestResponse(
            id = id,
            requesterId = requesterId,
            requesterEmail = requester?.email,
            requesterNickname = null,
            receiverId = receiverId,
            receiverEmail = receiver?.email,
            receiverNickname = null,
            requesterCharacterVisible = requesterVisible,
            requesterCharacterAppearance = if (requesterVisible) characterService.appearanceFor(requesterId) else null,
            receiverCharacterVisible = receiverVisible,
            receiverCharacterAppearance = if (receiverVisible) characterService.appearanceFor(receiverId) else null,
            status = status.name,
            createdAt = createdAt.toString(),
            respondedAt = respondedAt?.toString()
        )
    }
}
