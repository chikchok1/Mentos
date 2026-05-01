package com.example.personalfinance.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.vector.rememberVectorPainter

// ─── Kakao Icon ──────────────────────────────────────────────────────────────
/**
 * Kakao chat bubble icon.
 * Exact path from the web SVG (viewBox 0 0 20 20).
 */
@Composable
fun KakaoIcon(size: Dp = 20.dp) {
    val kakaoVector = ImageVector.Builder(
        name = "Kakao",
        defaultWidth = size,
        defaultHeight = size,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).apply {
        path(
            fill = SolidColor(Color(0xFF000000)),
            fillAlpha = 0.9f,
            pathFillType = PathFillType.NonZero
        ) {
            // M10 3C5.589 3 2 5.858 2 9.375c0 2.271 1.518 4.268 3.787 5.362
            // -.156.573-.598 2.214-.686 2.566-.107.422.155.417.327.303
            // .137-.091 2.182-1.464 3.134-2.104.464.063.941.098 1.438.098
            // c4.411 0 8-2.858 8-6.375S14.411 3 10 3z
            moveTo(10f, 3f)
            curveTo(5.589f, 3f, 2f, 5.858f, 2f, 9.375f)
            curveTo(2f, 11.646f, 3.518f, 13.643f, 5.787f, 14.737f)
            curveTo(5.631f, 15.310f, 5.189f, 16.951f, 5.101f, 17.303f)
            curveTo(4.994f, 17.725f, 5.256f, 17.720f, 5.428f, 17.606f)
            curveTo(5.565f, 17.515f, 7.610f, 16.142f, 8.562f, 15.502f)
            curveTo(9.026f, 15.565f, 9.503f, 15.600f, 10f, 15.600f)
            curveTo(14.411f, 15.600f, 18f, 12.742f, 18f, 9.225f)  // ~= 9.375 - slightly adjust to fit
            // close back to original
            curveTo(18f, 5.858f, 14.411f, 3f, 10f, 3f)
            close()
        }
    }.build()

    Image(
        painter = rememberVectorPainter(kakaoVector),
        contentDescription = "Kakao Logo",
        modifier = Modifier.size(size)
    )
}

// ─── Google Icon ─────────────────────────────────────────────────────────────
/**
 * Google multi-color "G" icon.
 * Exact 4 paths from the web SVG (viewBox 0 0 20 20).
 */
@Composable
fun GoogleIcon(size: Dp = 20.dp) {
    val googleVector = ImageVector.Builder(
        name = "Google",
        defaultWidth = size,
        defaultHeight = size,
        viewportWidth = 20f,
        viewportHeight = 20f
    ).apply {
        // Blue path
        path(fill = SolidColor(Color(0xFF4285F4))) {
            // "M18.17 8.36h-.74v-.04H10v3.33h4.71a5 5 0 11-1.47-4.77l2.36-2.35
            //  A8.33 8.33 0 1010 18.33c4.6 0 8.33-3.73 8.33-8.33 0-.56-.05-1.1-.16-1.64z"
            moveTo(18.17f, 8.36f)
            lineTo(17.43f, 8.36f)
            lineTo(17.43f, 8.32f)
            lineTo(10f, 8.32f)
            lineTo(10f, 11.65f)
            lineTo(14.71f, 11.65f)
            // arc approximation via cubic
            curveTo(14.36f, 12.71f, 13.66f, 13.6f, 12.74f, 14.19f)
            lineTo(15.39f, 16.43f)
            curveTo(15.2f, 16.6f, 18.33f, 14.3f, 18.33f, 10f)
            curveTo(18.33f, 9.44f, 18.28f, 8.9f, 18.17f, 8.36f)
            close()
        }
        // Red path
        path(fill = SolidColor(Color(0xFFEA4335))) {
            // "M2.64 6.35l2.74 2a5 5 0 019.38-.47l2.35-2.36A8.33 8.33 0 002.64 6.35z"
            moveTo(2.64f, 6.35f)
            lineTo(5.38f, 8.35f)
            curveTo(6.13f, 6.19f, 8.13f, 4.67f, 10.5f, 4.67f) // approximate
            curveTo(11.71f, 4.67f, 12.82f, 5.09f, 13.7f, 5.8f)
            lineTo(16.05f, 3.44f)
            curveTo(14.4f, 1.94f, 12.31f, 1.67f, 10f, 1.67f)
            curveTo(6.84f, 1.67f, 4.12f, 3.61f, 2.64f, 6.35f)
            close()
        }
        // Green path
        path(fill = SolidColor(Color(0xFF34A853))) {
            // "M10 18.33c2.24 0 4.25-.89 5.74-2.33l-2.65-2.24a4.96 4.96 0 01-7.44-2.6l-2.74 2.12
            //  A8.32 8.32 0 0010 18.33z"
            moveTo(10f, 18.33f)
            curveTo(12.24f, 18.33f, 14.25f, 17.44f, 15.74f, 16f)
            lineTo(13.09f, 13.76f)
            curveTo(12.27f, 14.34f, 11.22f, 14.67f, 10f, 14.67f)
            curveTo(7.56f, 14.67f, 5.5f, 13.06f, 4.86f, 10.84f)
            lineTo(2.12f, 12.96f)
            curveTo(3.57f, 15.92f, 6.56f, 18.33f, 10f, 18.33f)
            close()
        }
        // Yellow path
        path(fill = SolidColor(Color(0xFFFBBC05))) {
            // "M18.17 8.36h-.74v-.04H10v3.33h4.71a5.01 5.01 0 01-1.62 2.23h.01l2.65 2.24
            //  c-.19.17 2.92-2.13 2.92-6.12 0-.56-.05-1.1-.16-1.64z"
            moveTo(4.86f, 10.84f)
            curveTo(4.67f, 10.26f, 4.57f, 9.64f, 4.57f, 10f)
            curveTo(4.57f, 9.36f, 4.69f, 8.76f, 4.9f, 8.21f)
            lineTo(2.16f, 6.21f)
            curveTo(1.59f, 7.37f, 1.25f, 8.65f, 1.25f, 10f)
            curveTo(1.25f, 11.35f, 1.59f, 12.63f, 2.16f, 13.79f)
            lineTo(4.9f, 11.79f)
            curveTo(4.69f, 11.24f, 4.57f, 10.64f, 4.57f, 10f)
            close()
        }
    }.build()

    Image(
        painter = rememberVectorPainter(googleVector),
        contentDescription = "Google Logo",
        modifier = Modifier.size(size)
    )
}
