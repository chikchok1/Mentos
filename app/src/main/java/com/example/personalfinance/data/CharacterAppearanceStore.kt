package com.example.personalfinance.data

import android.content.Context
import android.util.Log
import com.example.personalfinance.network.ApiClient
import com.example.personalfinance.network.CharacterAppearanceResponse
import com.example.personalfinance.network.EquippedItemDto
import com.example.personalfinance.network.UpdateCharacterRequest
import com.example.personalfinance.ui.components.CharacterLayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 캐릭터 외형(레이어 선택값)을 SharedPreferences에 저장하고
 * StateFlow로 노출하는 싱글턴 스토어.
 */
class CharacterAppearanceStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("character_appearance", Context.MODE_PRIVATE)

    private val _appearanceFlow = MutableStateFlow(load())
    val appearanceFlow: StateFlow<CharacterLayerState> = _appearanceFlow.asStateFlow()

    // ── 저장 ─────────────────────────────────────────────────────────────────

    fun save(state: CharacterLayerState) {
        prefs.edit().apply {
            putStringOrNull("face",       state.face)
            putStringOrNull("hair",       state.hair)
            putStringOrNull("hat",        state.hat)
            putStringOrNull("accessory",  state.accessory)
            putStringOrNull("topClothes", state.topClothes)
            putStringOrNull("botClothes", state.botClothes)
        }.apply()
        _appearanceFlow.value = state
    }

    suspend fun saveWithServer(
        state: CharacterLayerState,
        ownedItems: Set<String>
    ): Boolean {
        save(state)
        return try {
            val api = ApiClient.getUserApi(appContext, TokenManager(appContext))
            val response = api.updateCharacter(UpdateCharacterRequest(state.toEquippedItems(ownedItems)))
            if (response.isSuccessful) {
                response.body()?.let { save(it.toLayerState()) }
                true
            } else {
                Log.w(TAG, "Character sync failed HTTP ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Character sync exception: ${e.message}")
            false
        }
    }

    suspend fun restoreFromServer(): Boolean {
        return try {
            val api = ApiClient.getUserApi(appContext, TokenManager(appContext))
            val response = api.getCharacter()
            if (response.isSuccessful) {
                response.body()?.let { save(it.toLayerState()) }
                true
            } else {
                Log.w(TAG, "Character restore failed HTTP ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Character restore exception: ${e.message}")
            false
        }
    }

    suspend fun syncCurrentWithServer(ownedItems: Set<String>): Boolean =
        saveWithServer(_appearanceFlow.value, ownedItems)

    fun clearForLogout() {
        prefs.edit().clear().apply()
        _appearanceFlow.value = CharacterLayerState()
    }

    // ── 불러오기 ──────────────────────────────────────────────────────────────

    private fun load() = CharacterLayerState(
        face       = prefs.getString("face",       null),
        hair       = prefs.getString("hair",       null),
        hat        = prefs.getString("hat",        null),
        accessory  = prefs.getString("accessory",  null),
        topClothes = prefs.getString("topClothes", null),
        botClothes = prefs.getString("botClothes", null),
    )

    // ── 싱글턴 ───────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "CharacterAppearanceStore"

        @Volatile private var instance: CharacterAppearanceStore? = null
        fun getInstance(context: Context): CharacterAppearanceStore =
            instance ?: synchronized(this) {
                instance ?: CharacterAppearanceStore(context.applicationContext).also { instance = it }
            }
    }
}

private fun CharacterLayerState.toEquippedItems(ownedItems: Set<String>): List<EquippedItemDto> {
    fun findItemId(folder: String, filename: String?): String? {
        if (filename.isNullOrBlank()) return null
        return ownedItems.sorted().firstOrNull { it.endsWith("/$folder/$filename") }
    }

    return listOfNotNull(
        findItemId("clothes", botClothes)?.let { EquippedItemDto("BOT", it, 10) },
        findItemId("clothes", topClothes)?.let { EquippedItemDto("TOP", it, 20) },
        findItemId("hairs", hair)?.let { EquippedItemDto("HAIR", it, 30) },
        findItemId("hats", hat)?.let { EquippedItemDto("HAT", it, 40) },
        findItemId("faces", face)?.let { EquippedItemDto("FACE", it, 50) },
        findItemId("accessories", accessory)?.let { EquippedItemDto("ACCESSORY", it, 60) },
    )
}

fun CharacterAppearanceResponse?.toLayerState(): CharacterLayerState {
    fun filename(itemId: String?): String? =
        itemId?.substringAfterLast("/")?.takeIf { it.isNotBlank() }

    var state = CharacterLayerState()
    this?.equippedItems.orEmpty()
        .sortedWith(compareBy<EquippedItemDto> { it.layerOrder ?: 0 }.thenBy { it.slot.orEmpty() })
        .forEach { item ->
            val file = filename(item.itemId) ?: return@forEach
            state = when (item.slot?.uppercase()) {
                "FACE" -> state.copy(face = file)
                "HAIR" -> state.copy(hair = file)
                "HAT" -> state.copy(hat = file)
                "TOP" -> state.copy(topClothes = file)
                "BOT" -> state.copy(botClothes = file)
                "ACCESSORY", "ACC" -> state.copy(accessory = file)
                else -> state
            }
        }
    return state
}

// SharedPreferences 확장 — null 이면 키를 지움
private fun android.content.SharedPreferences.Editor.putStringOrNull(key: String, value: String?) {
    if (value != null) putString(key, value) else remove(key)
}
