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
 * 픽셀 아트 캐릭터 — 레벨 + 직업(job)에 따라 외형 변화
 *
 * job 값:
 *   "beginner" — 초보 모험가 (기본)
 *   "cook"     — 요리사      (식비/카페)
 *   "manager"  — 생활관리사  (생활/마트)
 *   "merchant" — 상인        (쇼핑/온라인)
 *   "artist"   — 예술가      (문화/여가)
 *   "planner"  — 관리자      (고정비/구독)
 *   "healer"   — 힐러        (건강/의료)
 */
@Composable
fun PixelCharacter(
    level: Int,
    job: String = "beginner",
    modifier: Modifier = Modifier
) {
    // ── 레벨 구간별 몸체 색상 (5단계) ────────────────────────────────────────
    val bodyColor = when {
        level >= 50 -> Color(0xFFFFD700)  // 골드  — 마스터
        level >= 30 -> Color(0xFFE040FB)  // 보라  — 명인
        level >= 20 -> Color(0xFFFF6B6B)  // 빨강  — 전문가
        level >= 10 -> Color(0xFF7FA4FF)  // 파랑  — 숙련
        level >= 5  -> Color(0xFF6ECC8E)  // 초록  — 견습
        else        -> Color(0xFFA8C5FF)  // 연청  — 초보
    }

    // ── 직업별 의상/악세서리 색상 ─────────────────────────────────────────────
    val jobColor = when (job) {
        "cook"     -> Color(0xFFFFFFFF)  // 흰 앞치마
        "manager"  -> Color(0xFF4CAF50)  // 초록 앞치마
        "merchant" -> Color(0xFF8D6E63)  // 갈색 망토
        "artist"   -> Color(0xFF9C27B0)  // 보라 포인트
        "planner"  -> Color(0xFF1565C0)  // 남색 정장
        "healer"   -> Color(0xFF43A047)  // 초록 망토
        else       -> Color(0xFF94A3B8)  // 기본 회색
    }
    val accentColor = when (job) {
        "cook"     -> Color(0xFFFF7043)  // 프라이팬 (주황)
        "manager"  -> Color(0xFFFFB300)  // 장바구니 (노랑)
        "merchant" -> Color(0xFFFDD835)  // 금화 (노랑)
        "artist"   -> Color(0xFFE91E63)  // 악기 포인트 (핑크)
        "planner"  -> Color(0xFF90CAF9)  // 계산기 (하늘)
        "healer"   -> Color(0xFF80DEEA)  // 지팡이 (청록)
        else       -> Color(0xFFCBD5E1)
    }

    // ── 플로팅 애니메이션 ─────────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = -0.35f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    Canvas(modifier = modifier.size(120.dp)) {
        val s = size.width / 24f

        fun rect(x: Float, y: Float, w: Float, h: Float, color: Color, alpha: Float = 1f) {
            drawRect(
                color    = color.copy(alpha = alpha),
                topLeft  = Offset(x * s, (y + floatY) * s),
                size     = Size(w * s, h * s)
            )
        }

        // 그림자 (바닥에 고정)
        drawOval(
            color   = Color(0xFFE2E8F0).copy(alpha = 0.4f),
            topLeft = Offset(6f * s, 21f * s),
            size    = Size(12f * s, 2.5f * s)
        )

        // ── 직업별 하체/의상 레이어 ───────────────────────────────────────────
        when (job) {
            "cook" -> {
                // 흰 앞치마
                rect(10f, 15f, 4f, 5f, jobColor, 0.9f)
            }
            "manager" -> {
                // 초록 앞치마
                rect(10f, 15f, 4f, 5f, jobColor, 0.85f)
            }
            "merchant" -> {
                // 갈색 망토 (양쪽)
                rect(7f, 14f, 2f, 6f, jobColor, 0.8f)
                rect(15f, 14f, 2f, 6f, jobColor, 0.8f)
            }
            "healer" -> {
                // 초록 망토 (양쪽 + 뒤)
                rect(7f, 13f, 2f, 7f, jobColor, 0.75f)
                rect(15f, 13f, 2f, 7f, jobColor, 0.75f)
                rect(9f, 19f, 6f, 2f, jobColor, 0.6f)
            }
            "planner" -> {
                // 남색 정장 하의
                rect(9f, 14f, 6f, 6f, jobColor, 0.7f)
            }
        }

        // ── 몸체 ─────────────────────────────────────────────────────────────
        rect(9f, 14f, 6f, 6f, bodyColor)
        rect(8f, 15f, 1f, 4f, bodyColor, 0.7f)
        rect(15f, 15f, 1f, 4f, bodyColor, 0.7f)

        // ── 다리 ─────────────────────────────────────────────────────────────
        val legColor = when (job) {
            "artist"  -> Color(0xFF4A148C)  // 진보라 바지
            "planner" -> Color(0xFF0D47A1)  // 진남 바지
            else      -> Color(0xFF64748B)
        }
        rect(9f,  20f, 2f, 2f, legColor)
        rect(13f, 20f, 2f, 2f, legColor)

        // ── 머리 ─────────────────────────────────────────────────────────────
        rect(9f, 8f, 6f, 6f, Color(0xFFFFE4C4))

        // ── 헤어 (직업별 변화) ────────────────────────────────────────────────
        val hairColor = when (job) {
            "artist"  -> Color(0xFF7B1FA2)  // 보라 머리
            "healer"  -> Color(0xFF1B5E20)  // 진초록 머리
            "cook"    -> Color(0xFFFF6F00)  // 주황 머리
            else      -> Color(0xFF8B7355)  // 기본 갈색
        }
        rect(9f, 7f, 6f, 2f, hairColor)
        rect(8f, 8f, 1f, 2f, hairColor)
        rect(15f, 8f, 1f, 2f, hairColor)

        // ── 눈 ───────────────────────────────────────────────────────────────
        rect(10f, 10f, 1f, 1f, Color(0xFF333333))
        rect(13f, 10f, 1f, 1f, Color(0xFF333333))

        // 관리자 — 안경
        if (job == "planner") {
            rect(9.5f, 9.5f, 2f, 1.5f, accentColor, 0.5f)
            rect(12.5f, 9.5f, 2f, 1.5f, accentColor, 0.5f)
            rect(11.5f, 10f, 1f, 0.5f, accentColor, 0.7f)
        }

        // 레벨 5 이상 — 볼터치
        if (level >= 5) {
            rect(10f, 12f, 1f, 1f, Color(0xFFFF9E9E), 0.6f)
            rect(13f, 12f, 1f, 1f, Color(0xFFFF9E9E), 0.6f)
        }

        // ── 팔 ───────────────────────────────────────────────────────────────
        rect(7f,  15f, 2f, 1f, bodyColor, 0.8f)
        rect(15f, 15f, 2f, 1f, bodyColor, 0.8f)

        // ── 직업별 아이템 (오른손) ────────────────────────────────────────────
        when (job) {
            "cook" -> {
                // 프라이팬
                rect(16f, 14f, 2f, 1f, accentColor)        // 손잡이
                rect(16f, 13f, 3f, 2f, Color(0xFF616161))  // 팬 몸체
            }
            "manager" -> {
                // 장바구니
                rect(16f, 15f, 2f, 2f, accentColor)
                rect(16.5f, 14.5f, 1f, 1f, accentColor, 0.5f) // 손잡이
            }
            "merchant" -> {
                // 가방/금화
                rect(15f, 16f, 2f, 2f, accentColor, 0.9f)
                rect(15.5f, 15.5f, 1f, 1f, accentColor)
            }
            "artist" -> {
                // 악기 (기타 형태)
                rect(16f, 13f, 1f, 4f, Color(0xFF8D6E63))
                rect(15.5f, 14f, 2f, 2f, accentColor, 0.8f)
            }
            "planner" -> {
                // 계산기
                rect(16f, 14f, 2f, 3f, Color(0xFFECEFF1))
                rect(16.5f, 14.5f, 1f, 0.5f, accentColor, 0.8f)
                rect(16.5f, 15.5f, 1f, 0.5f, accentColor, 0.8f)
            }
            "healer" -> {
                // 지팡이
                rect(16.5f, 11f, 1f, 7f, Color(0xFF8D6E63))
                rect(16f, 11f, 2f, 1f, accentColor)        // 지팡이 상단 보석
            }
        }

        // ── 레벨 30+ 왕관 장식 ───────────────────────────────────────────────
        if (level >= 30) {
            rect(10f, 6f, 1f, 1f, accentColor)
            rect(12f, 5f, 2f, 2f, accentColor)
            rect(14f, 6f, 1f, 1f, accentColor)
        }

        // ── 레벨 50 마스터 — 황금 빛 후광 ────────────────────────────────────
        if (level >= 50) {
            rect(8f, 6f, 8f, 1f, Color(0xFFFFD700), 0.4f)
            rect(9f, 5f, 6f, 1f, Color(0xFFFFD700), 0.3f)
        }
    }
}
