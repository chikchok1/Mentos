package com.mentos.backend.entity

import jakarta.persistence.*
import java.time.YearMonth
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

    @Column(nullable = true, length = 30)
    var nickname: String? = null,

    @Column(name = "friend_code", nullable = true, unique = true, length = 10)
    var friendCode: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = true)
    var lastAttendanceDate: java.time.LocalDate? = null,

    @Column(nullable = false)
    var coins: Int = 0,

    @Column(nullable = false)
    var totalXp: Int = 0,

    @Column(nullable = false)
    var level: Int = 1,

    @Column(nullable = false)
    var currentXp: Int = 0,

    @Column(nullable = false)
    var nextLevelXp: Int = 50,

    @Column(nullable = false)
    var monthlyBudget: Long = 1_500_000L,

    @Column(nullable = false, length = 64)
    var job: String = "beginner",

    @Column(nullable = false, length = 512)
    var jobReason: String = "이번 달 지출 내역이 없어 모험가로 시작했어요.",

    @Column(nullable = false, length = 7)
    var jobMonth: String = YearMonth.now().toString(),

    /**
     * 월 예산 성공 보상을 마지막으로 받은 연월 (예: "2025-06").
     * null 이면 아직 한 번도 받지 않은 상태.
     * 같은 연월에 중복 지급을 방지하기 위해 사용.
     */
    @Column(nullable = true, length = 7)
    var lastBudgetRewardMonth: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var spendingVisibility: VisibilityScope = VisibilityScope.PRIVATE,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var characterVisibility: VisibilityScope = VisibilityScope.FRIENDS,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_owned_items",
        joinColumns = [JoinColumn(name = "user_id")],
        uniqueConstraints = [
            UniqueConstraint(
                name = "uk_user_owned_items_user_item",
                columnNames = ["user_id", "item_id"]
            )
        ]
    )
    @Column(name = "item_id")
    var ownedItems: MutableSet<String> = mutableSetOf()
) {
    // JPA 필수 no-arg 생성자
    protected constructor() : this(
        socialId  = "",
        provider  = "",
    )
}
