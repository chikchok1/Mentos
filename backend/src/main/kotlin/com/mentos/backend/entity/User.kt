package com.mentos.backend.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val socialId: String, // 구글의 경우 "sub", 카카오의 경우 회원번호(id) 등

    @Column(nullable = false)
    val provider: String, // "GOOGLE" or "KAKAO"

    @Column(nullable = true)
    var email: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = true)
    var lastAttendanceDate: java.time.LocalDate? = null
)
