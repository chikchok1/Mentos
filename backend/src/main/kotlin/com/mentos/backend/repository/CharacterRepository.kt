package com.mentos.backend.repository

import com.mentos.backend.entity.UserEquippedItem
import com.mentos.backend.entity.UserEquippedItemId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying

interface UserEquippedItemRepository : JpaRepository<UserEquippedItem, UserEquippedItemId> {
    fun findByUserIdOrderByLayerOrderAscSlotAsc(userId: Long): List<UserEquippedItem>

    @Modifying
    fun deleteByUserId(userId: Long): Int
}
