package com.mentos.backend.repository

import com.mentos.backend.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {
    fun findBySocialIdAndProvider(socialId: String, provider: String): User?

    @Query(
        value = """
            SELECT *
            FROM users
            WHERE id <> :currentUserId
              AND (
                LOWER(COALESCE(email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR CAST(id AS CHAR) LIKE CONCAT('%', :keyword, '%')
                OR LOWER(COALESCE(social_id, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY id DESC
            LIMIT 30
        """,
        nativeQuery = true
    )
    fun searchFriends(
        @Param("keyword") keyword: String,
        @Param("currentUserId") currentUserId: Long
    ): List<User>
}
