package com.mentos.backend.entity

import jakarta.persistence.*

@Entity
@Table(name = "category_cache")
data class CategoryCache(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val merchantName: String,

    @Column(nullable = false)
    val category: String
)
