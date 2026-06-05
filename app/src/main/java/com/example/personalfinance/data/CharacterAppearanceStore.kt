package com.example.personalfinance.data

import android.content.Context
import com.example.personalfinance.ui.components.CharacterLayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 캐릭터 외형(레이어 선택값)을 SharedPreferences에 저장하고
 * StateFlow로 노출하는 싱글턴 스토어.
 */
class CharacterAppearanceStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("character_appearance", Context.MODE_PRIVATE)

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
        @Volatile private var instance: CharacterAppearanceStore? = null
        fun getInstance(context: Context): CharacterAppearanceStore =
            instance ?: synchronized(this) {
                instance ?: CharacterAppearanceStore(context.applicationContext).also { instance = it }
            }
    }
}

// SharedPreferences 확장 — null 이면 키를 지움
private fun android.content.SharedPreferences.Editor.putStringOrNull(key: String, value: String?) {
    if (value != null) putString(key, value) else remove(key)
}
