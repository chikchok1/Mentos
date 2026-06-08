package com.mentos.backend.dto

data class EquippedItemDto(
    val slot: String,
    val itemId: String,
    val layerOrder: Int
)

data class CharacterAppearanceResponse(
    val baseCharacter: String = "base_body",
    val equippedItems: List<EquippedItemDto> = emptyList()
)

data class UpdateCharacterRequest(
    val equippedItems: List<EquippedItemDto> = emptyList()
)
