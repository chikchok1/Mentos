package com.mentos.backend.repository

import com.mentos.backend.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findBySocialIdAndProvider(socialId: String, provider: String): User?
}
