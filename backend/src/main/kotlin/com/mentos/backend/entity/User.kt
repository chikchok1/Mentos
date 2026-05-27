package com.mentos.backend.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
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
    var lastAttendanceDate: java.time.LocalDate? = null,

    @Column(nullable = false)
    var coins: Int = 0,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_owned_items", joinColumns = [JoinColumn(name = "user_id")])
    @Column(name = "item_id")
    var ownedItems: MutableSet<String> = mutableSetOf()
) {
    // JPA 필수 no-arg 생성자
    protected constructor() : this(
        socialId  = "",
        provider  = "",
    )
}
