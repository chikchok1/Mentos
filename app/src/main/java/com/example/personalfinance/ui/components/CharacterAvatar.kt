package com.example.personalfinance.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.personalfinance.data.ExpenseCategoryClassifier

// ── 캐릭터 파츠 enum (assets 경로 기반) ───────────────────────────────────────

enum class CharacterFace(val assetPath: String) {
    NORMAL("character_layers/faces/f_smile.png"),
    HAPPY("character_layers/faces/f_closed_smile.png"),
}

enum class CharacterOutfit(val assetPath: String?) {
    CHEF(null),        // 아직 assets에 없으므로 null → 레이어 스킵
    MERCHANT(null),
    EXPLORER(null),
}

enum class CharacterItem(val assetPath: String?) {
    NONE(null),
    PAN_RIGHT(null),
    SHOPPING_BAG_LEFT(null),
}

enum class CharacterEffect(val assetPath: String?) {
    NONE(null),
    SPARKLE(null),
}

// ── 캐릭터 상태 데이터 클래스 ─────────────────────────────────────────────────

data class CharacterState(
    val face: CharacterFace = CharacterFace.NORMAL,
    val outfit: CharacterOutfit = CharacterOutfit.EXPLORER,
    val item: CharacterItem = CharacterItem.NONE,
    val effect: CharacterEffect = CharacterEffect.NONE,
)

// ── 카테고리 → 캐릭터 상태 매핑 ──────────────────────────────────────────────

fun characterStateForCategory(category: String, happy: Boolean = false): CharacterState {
    val face = if (happy) CharacterFace.HAPPY else CharacterFace.NORMAL
    return when (category) {
        ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE ->
            CharacterState(face, CharacterOutfit.CHEF, CharacterItem.PAN_RIGHT, CharacterEffect.NONE)
        ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE ->
            CharacterState(face, CharacterOutfit.MERCHANT, CharacterItem.SHOPPING_BAG_LEFT, CharacterEffect.SPARKLE)
        ExpenseCategoryClassifier.CATEGORY_LIVING_MART ->
            CharacterState(face, CharacterOutfit.MERCHANT, CharacterItem.SHOPPING_BAG_LEFT, CharacterEffect.NONE)
        ExpenseCategoryClassifier.CATEGORY_CULTURE_LEISURE ->
            CharacterState(face, CharacterOutfit.EXPLORER, CharacterItem.NONE, CharacterEffect.SPARKLE)
        ExpenseCategoryClassifier.CATEGORY_FIXED_SUBSCRIPTION ->
            CharacterState(face, CharacterOutfit.EXPLORER, CharacterItem.NONE, CharacterEffect.NONE)
        else ->
            CharacterState(face, CharacterOutfit.EXPLORER, CharacterItem.NONE, CharacterEffect.NONE)
    }
}

// ── CharacterAvatar Composable (assets 기반) ──────────────────────────────────

/**
 * 레이어 방식 캐릭터 아바타 — assets/character_layers/ 폴더 기반.
 *
 * 레이어 순서: 몸통 → 표정 → 의상 → 아이템 → 이펙트
 * assetPath 가 null 인 파츠는 자동으로 스킵됩니다.
 */
@Composable
fun CharacterAvatar(
    face: CharacterFace = CharacterFace.NORMAL,
    outfit: CharacterOutfit = CharacterOutfit.EXPLORER,
    item: CharacterItem = CharacterItem.NONE,
    effect: CharacterEffect = CharacterEffect.NONE,
    size: Dp = 128.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // 레이어 경로 목록 (null 제거)
    val layerPaths = remember(face, outfit, item, effect) {
        listOfNotNull(
            "character_layers/base/base_body.png",
            face.assetPath,
            outfit.assetPath,
            item.assetPath,
            effect.assetPath,
        )
    }

    // 비트맵 로드
    val bitmaps = remember(layerPaths) {
        mutableStateListOf<androidx.compose.ui.graphics.ImageBitmap?>(*arrayOfNulls(layerPaths.size))
    }

    LaunchedEffect(layerPaths) {
        layerPaths.forEachIndexed { index, path ->
            bitmaps[index] = loadAssetBitmap(context, path)
        }
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier.size(size),
    ) {
        bitmaps.forEach { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** CharacterState 오버로드 */
@Composable
fun CharacterAvatar(
    state: CharacterState,
    size: Dp = 128.dp,
    modifier: Modifier = Modifier,
) = CharacterAvatar(
    face   = state.face,
    outfit = state.outfit,
    item   = state.item,
    effect = state.effect,
    size   = size,
    modifier = modifier,
)

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PreviewChef() {
    CharacterAvatar(
        face   = CharacterFace.HAPPY,
        outfit = CharacterOutfit.CHEF,
        item   = CharacterItem.PAN_RIGHT,
        effect = CharacterEffect.SPARKLE,
        size   = 200.dp,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PreviewMerchant() {
    CharacterAvatar(
        face   = CharacterFace.NORMAL,
        outfit = CharacterOutfit.MERCHANT,
        item   = CharacterItem.SHOPPING_BAG_LEFT,
        effect = CharacterEffect.NONE,
        size   = 200.dp,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PreviewExplorer() {
    CharacterAvatar(
        face   = CharacterFace.NORMAL,
        outfit = CharacterOutfit.EXPLORER,
        size   = 200.dp,
    )
}
