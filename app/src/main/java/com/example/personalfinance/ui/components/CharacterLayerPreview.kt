package com.example.personalfinance.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Assets에서 Bitmap 로드 유틸 ───────────────────────────────────────────────

fun loadAssetBitmap(context: Context, assetPath: String): ImageBitmap? {
    return try {
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    } catch (e: Exception) {
        null
    }
}

fun loadAssetBitmapRaw(context: Context, assetPath: String): Bitmap? {
    return try {
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (e: Exception) {
        null
    }
}

// ── 레이어 합성 후 투명 여백 crop ────────────────────────────────────────────

/**
 * 여러 레이어 Bitmap을 하나로 합성한 뒤
 * 사방의 완전 투명(alpha == 0) 여백을 제거하고 반환.
 * padding 파라미터로 잘린 가장자리에 약간의 여유를 줄 수 있음.
 */
fun mergeAndCropLayers(layers: List<Bitmap>, padding: Int = 2): Bitmap? {
    if (layers.isEmpty()) return null

    val width  = layers.maxOf { it.width }
    val height = layers.maxOf { it.height }

    // 1) 전체 레이어를 하나의 캔버스에 합성
    val merged = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(merged)
    val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
    layers.forEach { bmp ->
        canvas.drawBitmap(bmp, 0f, 0f, paint)
    }

    // 2) 픽셀 배열 분석 → bounding box 계산
    val pixels = IntArray(width * height)
    merged.getPixels(pixels, 0, width, 0, 0, width, height)

    var minX = width;  var maxX = 0
    var minY = height; var maxY = 0

    for (y in 0 until height) {
        for (x in 0 until width) {
            val alpha = (pixels[y * width + x] ushr 24) and 0xFF
            if (alpha > 0) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }

    // 픽셀이 하나도 없는 경우 원본 반환
    if (minX > maxX || minY > maxY) return merged

    // 3) padding 추가 후 경계 클램프
    minX = (minX - padding).coerceAtLeast(0)
    minY = (minY - padding).coerceAtLeast(0)
    maxX = (maxX + padding).coerceAtMost(width  - 1)
    maxY = (maxY + padding).coerceAtMost(height - 1)

    // 4) crop
    return Bitmap.createBitmap(merged, minX, minY, maxX - minX + 1, maxY - minY + 1)
}

// ── 전체 레이어 상태 ──────────────────────────────────────────────────────────

/**
 * 캐릭터를 구성하는 모든 레이어 선택값.
 * null = 해당 카테고리 없음(스킵).
 *
 * 레이어 합성 순서:
 *   base_body → botClothes → topClothes → hair → hat → face → accessory
 */
data class CharacterLayerState(
    val face: String?       = null,   // character_layers/faces/ 기준 파일명
    val hair: String?       = null,   // character_layers/hairs/ 기준 파일명
    val hat: String?        = null,   // character_layers/hats/ 기준 파일명
    val accessory: String?  = null,   // character_layers/accessories/ 기준 파일명
    val topClothes: String? = null,   // character_layers/clothes/ 기준 파일명
    val botClothes: String? = null,   // character_layers/clothes/ 기준 파일명
)

// ── CharacterLayerPreview (전체 레이어 지원) ──────────────────────────────────

/**
 * CharacterLayerState 기반으로 모든 레이어를 합성 + 투명 여백 crop 후 표시.
 * 레이어 간 위치 관계는 동일 캔버스에서 합성하므로 완벽히 유지됨.
 */
@Composable
fun CharacterLayerPreview(
    layerState: CharacterLayerState = CharacterLayerState(),
    size: Dp = 256.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val layerPaths = remember(layerState) {
        val grades = listOf("common", "rare", "unique", "legendary")
        fun resolve(category: String, file: String): String {
            for (g in grades) {
                val path = "character_layers/$g/$category/$file"
                val items = runCatching { context.assets.list("character_layers/$g/$category") }.getOrNull()
                if (items?.contains(file) == true) return path
            }
            return "character_layers/$category/$file"
        }

        listOfNotNull(
            "character_layers/base/base_body.png",
            layerState.botClothes?.let  { resolve("clothes", it) },
            layerState.topClothes?.let  { resolve("clothes", it) },
            layerState.hair?.let        { resolve("hairs", it) },
            layerState.hat?.let         { resolve("hats", it) },
            layerState.face?.let        { resolve("faces", it) },
            layerState.accessory?.let   { resolve("accessories", it) },
        )
    }

    var croppedBitmap by remember(layerPaths) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(layerPaths) {
        val rawLayers = layerPaths.mapNotNull { loadAssetBitmapRaw(context, it) }
        croppedBitmap = mergeAndCropLayers(rawLayers)?.asImageBitmap()
    }

    Box(
        modifier         = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        croppedBitmap?.let { bmp ->
            Image(
                bitmap             = bmp,
                contentDescription = null,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * 기존 facePath + extraLayers 방식 오버로드 — 하위 호환용.
 */
@Composable
fun CharacterLayerPreview(
    facePath: String = "faces/f_closed_smile.png",
    extraLayers: List<String> = emptyList(),
    size: Dp = 256.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val layerPaths = remember(facePath, extraLayers) {
        val grades = listOf("common", "rare", "unique", "legendary")
        fun resolve(path: String): String {
            val parts = path.split("/")
            if (parts.size != 2) return "character_layers/$path"
            val (cat, file) = parts
            for (g in grades) {
                val items = runCatching { context.assets.list("character_layers/$g/$cat") }.getOrNull()
                if (items?.contains(file) == true) return "character_layers/$g/$cat/$file"
            }
            return "character_layers/$path"
        }

        buildList {
            add("character_layers/base/base_body.png")
            add(resolve(facePath))
            extraLayers.forEach { add(resolve(it)) }
        }
    }

    var croppedBitmap by remember(layerPaths) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(layerPaths) {
        val rawLayers = layerPaths.mapNotNull { loadAssetBitmapRaw(context, it) }
        croppedBitmap = mergeAndCropLayers(rawLayers)?.asImageBitmap()
    }

    Box(
        modifier         = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        croppedBitmap?.let { bmp ->
            Image(
                bitmap             = bmp,
                contentDescription = null,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxSize(),
            )
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFEEEEEE)
@Composable
fun PreviewCharacterLayerDefault() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp),
    ) {
        Text(
            text       = "레이어 합성 테스트",
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.DarkGray,
        )
        Spacer(modifier = Modifier.height(12.dp))
        CharacterLayerPreview(size = 256.dp)
    }
}
