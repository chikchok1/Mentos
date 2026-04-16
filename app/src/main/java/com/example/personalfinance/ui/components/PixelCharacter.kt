package com.example.personalfinance.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Pixel art character rendered with Compose Canvas.
 * Mirrors the SVG rect-based character from PixelCharacter.tsx.
 * Animates with a gentle floating motion (replaces framer-motion).
 */
@Composable
fun PixelCharacter(
    level: Int,
    job: String = "beginner",
    modifier: Modifier = Modifier
) {
    val characterColor = when {
        level >= 10 -> Color(0xFF8B7FFF)  // Purple – high level
        level >= 5  -> Color(0xFF7FA4FF)  // Blue   – mid level
        else        -> Color(0xFFA8C5FF)  // Light  – beginner
    }
    val accessoryColor = when (job) {
        "warrior"  -> Color(0xFFFF6B6B)
        "mage"     -> Color(0xFFA78BFA)
        "merchant" -> Color(0xFFFCD34D)
        else       -> Color(0xFF94A3B8)
    }

    // Floating animation (replaces framer-motion)
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatGU by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = -0.35f,         // ≈ 1.75 dp float at 120 dp canvas
        animationSpec = infiniteRepeatable(
            animation  = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    Canvas(modifier = modifier.size(120.dp)) {
        // 1 grid unit = size.width / 24
        val s = size.width / 24f

        fun rect(x: Float, y: Float, w: Float, h: Float, color: Color, alpha: Float = 1f) {
            drawRect(
                color    = color.copy(alpha = alpha),
                topLeft  = Offset(x * s, (y + floatGU) * s),
                size     = Size(w * s, h * s)
            )
        }

        // Shadow ellipse (stays grounded while body floats)
        drawOval(
            color   = Color(0xFFE2E8F0).copy(alpha = 0.4f),
            topLeft = Offset(6f * s, 21f * s),
            size    = Size(12f * s, 2.5f * s)
        )

        // Body
        rect(9f, 14f, 6f, 6f, characterColor)
        rect(8f, 15f, 1f, 4f, characterColor, 0.7f)
        rect(15f, 15f, 1f, 4f, characterColor, 0.7f)

        // Head
        rect(9f, 8f, 6f, 6f, Color(0xFFFFE4C4))

        // Hair
        rect(9f, 7f, 6f, 2f, Color(0xFF8B7355))
        rect(8f, 8f, 1f, 2f, Color(0xFF8B7355))
        rect(15f, 8f, 1f, 2f, Color(0xFF8B7355))

        // Eyes
        rect(10f, 10f, 1f, 1f, Color(0xFF333333))
        rect(13f, 10f, 1f, 1f, Color(0xFF333333))

        // Smile cheeks (level ≥ 5)
        if (level >= 5) {
            rect(10f, 12f, 1f, 1f, Color(0xFFFF9E9E), 0.6f)
            rect(13f, 12f, 1f, 1f, Color(0xFFFF9E9E), 0.6f)
        }

        // Job accessories
        when (job) {
            "warrior" -> {
                rect(8f,  9f, 2f, 1f, accessoryColor)
                rect(14f, 9f, 2f, 1f, accessoryColor)
            }
            "mage" -> {
                rect(11f, 6f, 2f, 1f, accessoryColor)
                rect(12f, 5f, 1f, 1f, accessoryColor)
            }
            "merchant" -> {
                rect(14f, 16f, 2f, 2f, accessoryColor)
            }
        }

        // Arms
        rect(7f,  15f, 2f, 1f, characterColor, 0.8f)
        rect(15f, 15f, 2f, 1f, characterColor, 0.8f)

        // Legs
        rect(9f,  20f, 2f, 2f, Color(0xFF64748B))
        rect(13f, 20f, 2f, 2f, Color(0xFF64748B))
    }
}
