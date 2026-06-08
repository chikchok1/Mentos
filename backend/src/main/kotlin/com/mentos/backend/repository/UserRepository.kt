package com.mentos.backend.repository

import com.mentos.backend.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {
    fun findBySocialIdAndProvider(socialId: String, provider: String): User?

    fun existsByFriendCode(friendCode: String): Boolean

    fun findByFriendCodeAndIdNot(friendCode: String, currentUserId: Long): User?

    fun findByNicknameIgnoreCaseAndFriendCodeAndIdNot(
        nickname: String,
        friendCode: String,
        currentUserId: Long
    ): List<User>

    @Query(
        """
            SELECT u
            FROM User u
            WHERE u.id = :userId
              AND u.id <> :currentUserId
        """
    )
    fun findOtherById(
        @Param("userId") userId: Long,
        @Param("currentUserId") currentUserId: Long
    ): User?

    @Query(
        value = """
            SELECT *
            FROM users
            WHERE id <> :currentUserId
              AND LOWER(COALESCE(email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY id DESC
            LIMIT 30
        """,
        nativeQuery = true
    )
    fun searchByEmailKeyword(
        @Param("keyword") keyword: String,
        @Param("currentUserId") currentUserId: Long
    ): List<User>

    @Query(
        value = """
            SELECT *
            FROM users
            WHERE id <> :currentUserId
              AND LOWER(COALESCE(nickname, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY id DESC
            LIMIT 30
        """,
        nativeQuery = true
    )
    fun searchByNicknameKeyword(
        @Param("keyword") keyword: String,
        @Param("currentUserId") currentUserId: Long
    ): List<User>
}
