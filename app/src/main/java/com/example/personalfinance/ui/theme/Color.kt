package com.example.personalfinance.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.personalfinance.data.GachaGrade

// ── Brand Colors ──────────────────────────────────────────────────────────────
val Blue300  = Color(0xFFAEC6FF)
val Blue400  = Color(0xFF7FA4FF)
val Blue500  = Color(0xFF6B94FF)
val Purple400 = Color(0xFFB17CFF)
val Purple500 = Color(0xFF9B63FF)
val Blue50   = Color(0xFFEEF4FF)
val Purple50 = Color(0xFFF5EEFF)

// ── Category Colors ───────────────────────────────────────────────────────────
val CategoryFood     = Color(0xFFFF9E7C)
val CategoryShopping = Color(0xFFFFD07C)
val CategoryGame     = Color(0xFF7CD4FF)
val CategoryCulture  = Color(0xFFB17CFF)
val CategoryBeauty   = Color(0xFFFF7CB8)
val CategoryOther    = Color(0xFF94A3B8)

// ── Neutral Colors ────────────────────────────────────────────────────────────
val Gray50  = Color(0xFFF9FAFB)
val Gray100 = Color(0xFFF3F4F6)
val Gray200 = Color(0xFFE5E7EB)
val Gray400 = Color(0xFF9CA3AF)
val Gray500 = Color(0xFF6B7280)
val Gray600 = Color(0xFF4B5563)
val Gray700 = Color(0xFF374151)
val Gray900 = Color(0xFF111827)

// ── Semantic Colors ───────────────────────────────────────────────────────────
val GreenSuccess  = Color(0xFF16A34A)
val OrangeWarning = Color(0xFFEA580C)
val RedDanger     = Color(0xFFDC2626)

// ── Login UI Colors ───────────────────────────────────────────────────────────
val TextPrimary = Color(0xFF030213)
val TextSecondary = Color(0xFF717182)
val TextDark = Color(0xFF1A1A1A)
val KakaoYellow = Color(0xFFFEE500)
val GoogleGray = Color(0xFFF1F1F1)
val GoogleBlue = Color(0xFF4285F4)
val GoogleRed = Color(0xFFEA4335)
val GoogleGreen = Color(0xFF34A853)
val GoogleYellow = Color(0xFFFBBC05)

// ── Capsule Animation Tokens ────────────────────────────────────────────────

// Base colors for capsule per rarity
val capsuleBaseColors = mapOf(
    GachaGrade.COMMON to Color(0xFFE0E0E0),
    GachaGrade.RARE to Color(0xFF64B5F6),
    GachaGrade.UNIQUE to Color(0xFFCE93D8),
    GachaGrade.LEGENDARY to Color(0xFFFFD54F)
)

// Particle tint colors per rarity
val particleColors = mapOf(
    GachaGrade.COMMON to listOf(Color(0xFF9E9E9E), Color(0xFFB0BEC5)),
    GachaGrade.RARE to listOf(Color(0xFF1565C0), Color(0xFF42A5F5)),
    GachaGrade.UNIQUE to listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC)),
    GachaGrade.LEGENDARY to listOf(Color(0xFFE65100), Color(0xFFFFA726))
)
