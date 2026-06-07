package com.mentos.backend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDateTime

data class UserEquippedItemId(
    var userId: Long = 0,
    var slot: String = ""
) : Serializable

@Entity
@Table(name = "user_equipped_items")
@IdClass(UserEquippedItemId::class)
class UserEquippedItem(
    @Id
    @Column(name = "user_id", nullable = false)
    var userId: Long = 0,

    @Id
    @Column(nullable = false, length = 50)
    var slot: String = "",

    @Column(name = "item_id", nullable = false, length = 255)
    var itemId: String = "",

    @Column(name = "layer_order", nullable = false)
    var layerOrder: Int = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    @PreUpdate
    fun touch() {
        updatedAt = LocalDateTime.now()
    }
}
