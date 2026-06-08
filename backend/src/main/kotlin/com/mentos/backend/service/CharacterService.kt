package com.mentos.backend.service

import com.mentos.backend.dto.CharacterAppearanceResponse
import com.mentos.backend.dto.EquippedItemDto
import com.mentos.backend.dto.UpdateCharacterRequest
import com.mentos.backend.entity.UserEquippedItem
import com.mentos.backend.repository.UserEquippedItemRepository
import com.mentos.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CharacterService(
    private val userRepository: UserRepository,
    private val equippedItemRepository: UserEquippedItemRepository
) {
    @Transactional(readOnly = true)
    fun getCharacter(userId: Long): CharacterAppearanceResponse =
        appearanceFor(userId)

    @Transactional
    fun updateCharacter(userId: Long, request: UpdateCharacterRequest): CharacterAppearanceResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }

        val normalized = request.equippedItems.map { item ->
            EquippedItemDto(
                slot = normalizeSlot(item.slot),
                itemId = item.itemId.trim(),
                layerOrder = item.layerOrder
            )
        }.filter { it.slot.isNotBlank() && it.itemId.isNotBlank() }

        require(normalized.map { it.slot }.distinct().size == normalized.size) {
            "같은 slot을 중복해서 장착할 수 없습니다."
        }
        normalized.forEach { item ->
            require(user.ownedItems.contains(item.itemId)) {
                "보유하지 않은 아이템은 장착할 수 없습니다: ${item.itemId}"
            }
            require(item.slot in allowedSlots && itemMatchesSlot(item.itemId, item.slot)) {
                "아이템과 장착 slot이 일치하지 않습니다: ${item.slot}, ${item.itemId}"
            }
        }

        equippedItemRepository.deleteByUserId(userId)
        equippedItemRepository.saveAll(
            normalized.map { item ->
                UserEquippedItem(
                    userId = userId,
                    slot = item.slot,
                    itemId = item.itemId,
                    layerOrder = item.layerOrder
                )
            }
        )

        return CharacterAppearanceResponse(equippedItems = normalized.sortedWith(itemOrder))
    }

    @Transactional(readOnly = true)
    fun appearanceFor(userId: Long): CharacterAppearanceResponse =
        CharacterAppearanceResponse(
            equippedItems = equippedItemRepository
                .findByUserIdOrderByLayerOrderAscSlotAsc(userId)
                .map { EquippedItemDto(it.slot, it.itemId, it.layerOrder) }
        )

    private fun normalizeSlot(slot: String): String =
        slot.trim().uppercase()

    private fun itemMatchesSlot(itemId: String, slot: String): Boolean {
        val parts = itemId.split("/")
        if (parts.size != 3) return false
        val category = parts[1]
        val filename = parts[2]
        return when (slot) {
            "FACE" -> category == "faces"
            "HAIR" -> category == "hairs"
            "HAT" -> category == "hats"
            "ACCESSORY", "ACC" -> category == "accessories"
            "TOP" -> category == "clothes" && filename.startsWith("top")
            "BOT" -> category == "clothes" && filename.startsWith("bot")
            else -> false
        }
    }

    private val itemOrder = compareBy<EquippedItemDto> { it.layerOrder }.thenBy { it.slot }

    private val allowedSlots = setOf("FACE", "HAIR", "HAT", "ACCESSORY", "ACC", "TOP", "BOT")
}
