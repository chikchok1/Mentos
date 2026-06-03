package com.example.personalfinance.ui.components

import android.content.Context
import android.graphics.BitmapFactory
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

/**
 * assets 폴더에서 PNG를 읽어 ImageBitmap으로 반환.
 * 실패 시 null 반환.
 */
fun loadAssetBitmap(context: Context, assetPath: String): ImageBitmap? {
    return try {
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    } catch (e: Exception) {
        null
    }
}

// ── CharacterLayerPreview ─────────────────────────────────────────────────────

/**
 * assets/character_layers/ 폴더의 PNG 파일을 레이어 순서대로 겹쳐 그리는 테스트 컴포넌트.
 *
 * 레이어 순서 (아래 → 위):
 *   1. base/base_body.png
 *   2. faces/f_closed_smile.png  (또는 외부에서 지정한 facePath)
 *
 * 이후 옷·머리·모자·악세사리 레이어도 [extraLayers]에 경로만 추가하면 됩니다.
 *
 * @param facePath     faces/ 폴더 기준 파일명 (기본값: "f_closed_smile.png")
 * @param extraLayers  추가 레이어 asset 경로 목록 (위에서부터 순서대로 쌓임)
 * @param size         정사각형 캔버스 크기
 */
@Composable
fun CharacterLayerPreview(
    facePath: String = "faces/f_closed_smile.png",
    extraLayers: List<String> = emptyList(),
    size: Dp = 256.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // 레이어 경로 목록 (순서 = 렌더링 순서)
    val layerPaths = remember(facePath, extraLayers) {
        buildList {
            add("character_layers/base/base_body.png")
            add("character_layers/$facePath")
            extraLayers.forEach { add("character_layers/$it") }
        }
    }

    // 각 레이어를 비동기로 로드
    val bitmaps = remember(layerPaths) {
        mutableStateListOf<ImageBitmap?>(*arrayOfNulls(layerPaths.size))
    }

    LaunchedEffect(layerPaths) {
        layerPaths.forEachIndexed { index, path ->
            bitmaps[index] = loadAssetBitmap(context, path)
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        bitmaps.forEach { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,   // 비율 유지, 크기 맞춤
                    modifier = Modifier.fillMaxSize(), // 항상 동일한 (0,0) 기준
                )
            }
        }
    }
}

// ── Preview (Android Studio 미리보기) ─────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFEEEEEE)
@Composable
fun PreviewCharacterLayerDefault() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp),
    ) {
        Text(
            text = "레이어 합성 테스트",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray,
        )
        Spacer(modifier = Modifier.height(12.dp))
        CharacterLayerPreview(size = 256.dp)
    }
}
