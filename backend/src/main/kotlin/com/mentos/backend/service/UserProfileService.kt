package com.mentos.backend.service

import com.mentos.backend.dto.UpdateUserProfileRequest
import com.mentos.backend.dto.UserProfileResponse
import com.mentos.backend.entity.User
import com.mentos.backend.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Service
class UserProfileService(
    private val userRepository: UserRepository
) {
    private val random = SecureRandom()

    @Transactional
    fun getProfile(userId: Long): UserProfileResponse {
        val user = ensureFriendCode(findUser(userId))
        return user.toProfileResponse()
    }

    @Transactional
    fun updateProfile(userId: Long, request: UpdateUserProfileRequest): UserProfileResponse {
        val user = ensureFriendCode(findUser(userId))
        user.nickname = normalizeNickname(request.nickname)
        return userRepository.save(user).toProfileResponse()
    }

    @Transactional
    fun ensureFriendCode(user: User): User {
        if (!user.friendCode.isNullOrBlank()) return user

        repeat(MAX_GENERATION_ATTEMPTS) { attempt ->
            val candidate = generateCandidate(if (attempt < SHORT_CODE_ATTEMPTS) 4 else 6)
            if (userRepository.existsByFriendCode(candidate)) return@repeat

            user.friendCode = candidate
            try {
                return userRepository.saveAndFlush(user)
            } catch (e: DataIntegrityViolationException) {
                user.friendCode = null
            }
        }
        throw IllegalStateException("friendCode 생성에 실패했습니다.")
    }

    fun displayName(user: User?): String {
        if (user == null) return "사용자"
        val nickname = user.nickname?.trim()?.takeIf { it.isNotBlank() }
        val friendCode = user.friendCode?.trim()?.takeIf { it.isNotBlank() }
        if (nickname != null && friendCode != null) return "$nickname#$friendCode"
        if (nickname != null) return nickname

        val email = user.email?.trim()?.takeIf { it.isNotBlank() }
        val emailPrefix = email?.substringBefore("@")?.takeIf { it.isNotBlank() }
        return emailPrefix ?: email ?: "사용자 ${user.id}"
    }

    private fun findUser(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }

    private fun normalizeNickname(raw: String?): String? {
        val value = raw?.trim() ?: return null
        require(value.isNotBlank()) { "닉네임을 입력해 주세요." }
        require(value.length in 2..12) { "닉네임은 2~12자로 입력해 주세요." }
        require(!value.contains("#")) { "닉네임에는 # 문자를 사용할 수 없습니다." }
        return value
    }

    private fun generateCandidate(length: Int): String {
        val bound = when (length) {
            4 -> 10_000
            else -> 1_000_000
        }
        return random.nextInt(bound).toString().padStart(length, '0')
    }

    private fun User.toProfileResponse(): UserProfileResponse =
        UserProfileResponse(
            id = id,
            email = email,
            nickname = nickname,
            friendCode = friendCode,
            displayName = displayName(this)
        )

    private companion object {
        const val SHORT_CODE_ATTEMPTS = 50
        const val MAX_GENERATION_ATTEMPTS = 100
    }
}
