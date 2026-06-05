package com.example.personalfinance.ui.main.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.personalfinance.data.GachaGrade
import com.example.personalfinance.data.GachaItem
import com.example.personalfinance.data.GachaResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── 희귀도 테마 ──────────────────────────────────────────────────────────────

private data class GradeTheme(
    val lidLight: Color, val lidDark: Color,
    val bodyLight: Color, val bodyDark: Color,
    val glowColor: Color, val bgColor: Color,
    val particleColors: List<Color>, val particleCount: Int,
    val displayName: String, val labelColor: Color,
)

private fun gradeTheme(grade: GachaGrade): GradeTheme = when (grade) {
    GachaGrade.COMMON -> GradeTheme(
        lidLight = Color(0xFFECEFF1), lidDark = Color(0xFF90A4AE),
        bodyLight = Color(0xFFB0BEC5), bodyDark = Color(0xFF455A64),
        glowColor = Color(0xFFCFD8DC), bgColor = Color(0xFF090C0F),
        particleColors = listOf(Color(0xFFB0BEC5), Color(0xFFECEFF1), Color(0xFF78909C)),
        particleCount = 14, displayName = "Common", labelColor = Color(0xFFB0BEC5),
    )
    GachaGrade.RARE -> GradeTheme(
        lidLight = Color(0xFFBBDEFB), lidDark = Color(0xFF1565C0),
        bodyLight = Color(0xFF64B5F6), bodyDark = Color(0xFF0D47A1),
        glowColor = Color(0xFF82B1FF), bgColor = Color(0xFF020810),
        particleColors = listOf(Color(0xFF42A5F5), Color(0xFF90CAF9), Color(0xFF1E88E5), Color(0xFFBBDEFB)),
        particleCount = 26, displayName = "Rare", labelColor = Color(0xFF64B5F6),
    )
    GachaGrade.UNIQUE -> GradeTheme(
        lidLight = Color(0xFFF3E5F5), lidDark = Color(0xFF7B1FA2),
        bodyLight = Color(0xFFCE93D8), bodyDark = Color(0xFF4A148C),
        glowColor = Color(0xFFE040FB), bgColor = Color(0xFF05020C),
        particleColors = listOf(Color(0xFFCE93D8), Color(0xFFF48FB1), Color(0xFFAB47BC), Color(0xFFEA80FC)),
        particleCount = 36, displayName = "Unique", labelColor = Color(0xFFCE93D8),
    )
    GachaGrade.LEGENDARY -> GradeTheme(
        lidLight = Color(0xFFFFF9C4), lidDark = Color(0xFFFF8F00),
        bodyLight = Color(0xFFFFEE58), bodyDark = Color(0xFFBF360C),
        glowColor = Color(0xFFFFAB40), bgColor = Color(0xFF0C0400),
        particleColors = listOf(Color(0xFFFFD700), Color(0xFFFFA726), Color(0xFFFF7043), Color(0xFFFFF176), Color(0xFFFFAB40)),
        particleCount = 60, displayName = "Legendary", labelColor = Color(0xFFFFD700),
    )
}

// ── 파티클 ────────────────────────────────────────────────────────────────────

private data class Particle(
    val angle: Float, val speed: Float, val size: Float,
    val color: Color, val type: Int,
)

private fun buildParticles(t: GradeTheme) = List(t.particleCount) { i ->
    val a = (i.toFloat() / t.particleCount) * 2f * PI.toFloat() + (Math.random() * 0.5f).toFloat()
    Particle(a, 0.35f + (Math.random() * 0.65f).toFloat(), 5f + (Math.random() * 10f).toFloat(),
        t.particleColors[i % t.particleColors.size], i % 3)
}

private fun DrawScope.drawStar(cx: Float, cy: Float, r: Float, color: Color) {
    val path = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) r else r * 0.42f
        val a = i * PI.toFloat() / 5f - PI.toFloat() / 2f
        if (i == 0) path.moveTo(cx + rad * cos(a), cy + rad * sin(a))
        else path.lineTo(cx + rad * cos(a), cy + rad * sin(a))
    }
    path.close(); drawPath(path, color)
}

// ── 페이즈 ────────────────────────────────────────────────────────────────────

private enum class Phase { APPEAR, SHAKE, GLOW, OPEN, REVEAL, DONE }

// ── 메인 컴포저블 ─────────────────────────────────────────────────────────────

@Composable
fun CapsuleOpeningAnimation(
    result: GachaResult,
    onDismiss: () -> Unit,
    durationMs: Int = 2500,
) {
    val item: GachaItem = when (result) {
        is GachaResult.NewItem        -> result.item
        is GachaResult.DuplicateCoin  -> result.item
    }
    val isDuplicate = result is GachaResult.DuplicateCoin
    val coinReward  = if (result is GachaResult.DuplicateCoin) result.coins else 0

    val theme     = remember { gradeTheme(item.grade) }
    val particles = remember { buildParticles(theme) }
    val scope     = rememberCoroutineScope()

    // ── 상태 ──────────────────────────────────────────────────────────────────
    var phase       by remember { mutableStateOf(Phase.APPEAR) }
    var shakeMag    by remember { mutableFloatStateOf(0f) }
    var showCapsule by remember { mutableStateOf(true) }
    var showItem    by remember { mutableStateOf(false) }

    // ── 무한 루프 애니메이션 (흔들기 / 글로우 / 회전) ─────────────────────────
    val inf = rememberInfiniteTransition(label = "inf")
    val glowPulse by inf.animateFloat(0.3f, 1f,
        infiniteRepeatable(tween(480, easing = EaseInOut), RepeatMode.Reverse), "glow")
    val shakeRaw by inf.animateFloat(-1f, 1f,
        infiniteRepeatable(tween(70, easing = EaseInOut), RepeatMode.Reverse), "shake")
    val rotation by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(2800, easing = LinearEasing)), "rot")

    // ── 단발 애니메이터 ───────────────────────────────────────────────────────
    val bgAlpha       = remember { Animatable(0f) }
    val capsuleScale  = remember { Animatable(0f) }
    val lidOffsetY    = remember { Animatable(0f) }
    val lidOffsetX    = remember { Animatable(0f) }   // 뚜껑이 살짝 비스듬히
    val lidRotation   = remember { Animatable(0f) }   // 뚜껑 회전
    val lidScale      = remember { Animatable(1f) }   // 뚜껑 스케일 (작아지며 날아감)
    val bodyOffsetY   = remember { Animatable(0f) }
    val burstProgress = remember { Animatable(0f) }
    val itemAlpha     = remember { Animatable(0f) }
    val itemScale     = remember { Animatable(0.3f) }

    // ── 단일 LaunchedEffect — 경쟁 없이 순서대로 실행 ─────────────────────────
    LaunchedEffect(Unit) {
        // [1] APPEAR: 배경 페이드인 + 캡슐 등장
        phase = Phase.APPEAR
        bgAlpha.animateTo(1f, tween(300))
        capsuleScale.animateTo(1f, tween(550, easing = FastOutSlowInEasing))

        // [2] SHAKE: 흔들기 700ms
        phase = Phase.SHAKE
        shakeMag = 16f
        delay(700L)

        // [3] GLOW: 솔기 빛 + 에너지빔 550ms
        phase = Phase.GLOW
        shakeMag = 6f
        delay(550L)

        // [4] OPEN: 2단계 뚜껑 오픈
        phase = Phase.OPEN
        shakeMag = 0f

        // 4-A: 내부 압력으로 뚜껑이 살짝 위로 들림 (천천히)
        launch { lidOffsetY.animateTo(-28f, tween(180, easing = EaseOut)) }
        delay(180L)

        // 4-B: 뚜껑이 회전하며 비스듬히 날아감 (빠르게)
        launch {
            lidOffsetY.animateTo(-980f, tween(480, easing = androidx.compose.animation.core.EaseIn))
        }
        launch {
            lidOffsetX.animateTo(60f, tween(480, easing = EaseOut))
        }
        launch {
            lidRotation.animateTo(-35f, tween(480, easing = EaseOut))
        }
        launch {
            lidScale.animateTo(0.7f, tween(480, easing = androidx.compose.animation.core.EaseIn))
        }

        // 몸통: 아래로 살짝 눌렸다가 spring 복귀
        launch {
            bodyOffsetY.animateTo(50f, tween(200, easing = EaseOut))
            bodyOffsetY.animateTo(20f, spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMediumLow,
            ))
        }

        // 파티클
        burstProgress.animateTo(1f, tween(500, easing = EaseOut))
        delay(120L)

        // [5] REVEAL: 아이템 등장
        showCapsule = false
        showItem    = true
        phase       = Phase.REVEAL
        launch { itemAlpha.animateTo(1f, tween(380)) }
        itemScale.animateTo(
            1f, spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMedium,
            )
        )
        delay(900L)

        // [6] DONE
        phase = Phase.DONE
    }

    val shakeX = if (phase == Phase.SHAKE || phase == Phase.GLOW) shakeRaw * shakeMag else 0f

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = bgAlpha.value }
                .background(theme.bgColor)
                .clickable(
                    enabled           = phase == Phase.DONE,
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                ) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {

            // ── 배경 글로우 / 에너지 빔 ────────────────────────────────────────
            if (phase == Phase.GLOW || phase == Phase.OPEN || phase == Phase.REVEAL || phase == Phase.DONE) {
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width / 2f; val cy = size.height / 2f
                    for (r in 1..5) {
                        drawCircle(theme.glowColor.copy(alpha = glowPulse * 0.09f * (6 - r)), 130f * r, Offset(cx, cy))
                    }
                    if (phase == Phase.GLOW) {
                        rotate(rotation, Offset(cx, cy)) {
                            for (i in 0..7) {
                                val a = i * (PI.toFloat() / 4f)
                                drawLine(
                                    color = theme.glowColor.copy(alpha = glowPulse * 0.5f),
                                    start = Offset(cx + cos(a) * 110f, cy + sin(a) * 110f),
                                    end   = Offset(cx + cos(a) * 220f, cy + sin(a) * 220f),
                                    strokeWidth = 3f, cap = StrokeCap.Round,
                                )
                            }
                        }
                    }
                }
            }

            // ── 파티클 버스트 ──────────────────────────────────────────────────
            if (phase == Phase.OPEN || phase == Phase.REVEAL || phase == Phase.DONE) {
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width / 2f; val cy = size.height / 2f
                    val maxR = size.minDimension * 0.55f * burstProgress.value
                    val fade = (1f - burstProgress.value * 0.5f).coerceIn(0f, 1f)
                    particles.forEach { p ->
                        val px = cx + cos(p.angle) * maxR * p.speed
                        val py = cy + sin(p.angle) * maxR * p.speed
                        when (p.type) {
                            0 -> drawCircle(p.color.copy(alpha = fade), p.size, Offset(px, py))
                            1 -> drawStar(px, py, p.size * 0.9f, p.color.copy(alpha = fade))
                            else -> drawLine(
                                p.color.copy(alpha = fade),
                                Offset(cx + cos(p.angle) * maxR * p.speed * 0.4f, cy + sin(p.angle) * maxR * p.speed * 0.4f),
                                Offset(px, py), p.size * 0.5f, cap = StrokeCap.Round,
                            )
                        }
                    }
                    if (phase == Phase.OPEN) {
                        val fa = (1f - burstProgress.value).coerceIn(0f, 1f)
                        drawCircle(Color.White.copy(alpha = fa * 0.85f), 90f * fa, Offset(cx, cy))
                    }
                }
            }

            // ── 캡슐 (뚜껑 + 몸통) ────────────────────────────────────────────
            if (showCapsule) {
                // 가챠폰 캡슐: 정원 형태 (지름 = 너비 = 높이)
                val diameter = 240.dp
                val halfH    = 120.dp
                val cornerR  = 120.dp  // 지름의 절반 → 완전한 반원

                Box(
                    modifier = Modifier
                        .size(diameter)
                        .graphicsLayer {
                            scaleX       = capsuleScale.value
                            scaleY       = capsuleScale.value
                            translationX = shakeX
                        },
                ) {
                    // ── 몸통 (아래 반구) ─────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .width(diameter)
                            .height(halfH)
                            .align(Alignment.BottomCenter)
                            .graphicsLayer { translationY = bodyOffsetY.value }
                            .clip(RoundedCornerShape(bottomStart = cornerR, bottomEnd = cornerR))
                            .background(Brush.verticalGradient(listOf(theme.bodyLight, theme.bodyDark)))
                    ) {
                        // 하이라이트
                        Box(
                            Modifier
                                .size(70.dp)
                                .offset(12.dp, 8.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                        )
                    }

                    // ── 뚜껑 (위 반구) ───────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .width(diameter)
                            .height(halfH)
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                translationY = lidOffsetY.value
                                translationX = lidOffsetX.value
                                rotationZ    = lidRotation.value
                                scaleX       = lidScale.value
                                scaleY       = lidScale.value
                            }
                            .clip(RoundedCornerShape(topStart = cornerR, topEnd = cornerR))
                            .background(Brush.verticalGradient(listOf(theme.lidLight, theme.lidDark)))
                    ) {
                        // 하이라이트 (왼쪽 위)
                        Box(
                            Modifier
                                .size(80.dp)
                                .offset(18.dp, 14.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.30f))
                        )
                    }

                    // ── 솔기 글로우 (GLOW 단계) ─────────────────────────────
                    if (phase == Phase.GLOW) {
                        Canvas(
                            Modifier
                                .width(diameter)
                                .height(6.dp)
                                .align(Alignment.Center)
                        ) {
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    listOf(Color.Transparent, theme.glowColor.copy(glowPulse), Color.Transparent)
                                ),
                                start = Offset(0f, size.height / 2f),
                                end   = Offset(size.width, size.height / 2f),
                                strokeWidth = size.height,
                            )
                        }
                    }
                }
            }

            // ── 아이템 공개 ──────────────────────────────────────────────────
            if (showItem) {
                Column(
                    modifier = Modifier
                        .graphicsLayer { alpha = itemAlpha.value; scaleX = itemScale.value; scaleY = itemScale.value }
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // 등급 배지
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(theme.labelColor.copy(alpha = 0.18f))
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text("✦ ${theme.displayName} ✦", color = theme.labelColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(24.dp))

                    // 아이템 이미지
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(theme.lidLight.copy(0.30f), theme.bodyDark.copy(0.65f)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter            = painterResource(id = item.drawableResId),
                            contentDescription = item.name,
                            modifier           = Modifier.size(135.dp).padding(8.dp),
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(item.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)

                    Spacer(Modifier.height(10.dp))

                    if (isDuplicate) {
                        Text("이미 보유한 아이템  →  코인 +$coinReward", color = Color(0xFFFFB74D), fontSize = 14.sp, textAlign = TextAlign.Center)
                    } else {
                        Text("새 아이템 획득!", color = theme.labelColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (phase == Phase.DONE) {
                        Spacer(Modifier.height(44.dp))
                        Text("탭하여 닫기", color = Color.White.copy(alpha = 0.38f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
