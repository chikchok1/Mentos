package com.example.personalfinance.ui.main

import android.view.WindowManager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.personalfinance.data.UserStatsFeedback
import com.example.personalfinance.data.UserStatsStore
import com.example.personalfinance.data.UserStatsCalculator
import com.example.personalfinance.ui.theme.Blue50
import com.example.personalfinance.ui.theme.Blue500
import com.example.personalfinance.ui.theme.Gray200
import com.example.personalfinance.ui.theme.Gray400
import com.example.personalfinance.ui.theme.Gray500
import com.example.personalfinance.ui.theme.Gray600
import com.example.personalfinance.ui.theme.Gray700
import com.example.personalfinance.ui.theme.Gray900
import kotlinx.coroutines.delay

@Composable
fun UserStatsFeedbackHost(
    store: UserStatsStore,
    modifier: Modifier = Modifier
) {
    val feedbackQueue by store.feedbackQueue.collectAsState()
    val currentFeedback = feedbackQueue.firstOrNull()

    Box(
        modifier          = modifier.fillMaxWidth(),
        contentAlignment  = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = currentFeedback is UserStatsFeedback.XpGained,
            enter   = slideInVertically(animationSpec = tween(220)) { it / 2 } + fadeIn(tween(180)),
            exit    = slideOutVertically(animationSpec = tween(180)) { it / 2 } + fadeOut(tween(160))
        ) {
            val event = currentFeedback as? UserStatsFeedback.XpGained
            if (event != null) {
                XpFeedbackBubble(event)
            }
        }
    }

    LaunchedEffect(currentFeedback?.id) {
        val event = currentFeedback
        if (event is UserStatsFeedback.XpGained) {
            delay(2_500L)
            store.consumeFeedback(event.id)
        }
    }

    when (val event = currentFeedback) {
        is UserStatsFeedback.LevelUp -> {
            LevelUpDialog(
                event     = event,
                onConfirm = { store.consumeFeedback(event.id) }
            )
        }
        is UserStatsFeedback.JobChanged -> {
            JobChangedDialog(
                event     = event,
                onConfirm = { store.consumeFeedback(event.id) }
            )
        }
        else -> Unit
    }
}

@Composable
private fun XpFeedbackBubble(event: UserStatsFeedback.XpGained) {
    // 배경을 앱 브랜드 Blue50으로, 테두리도 Blue 계열로 통일
    Row(
        modifier = Modifier
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(28.dp), spotColor = Blue500.copy(alpha = 0.10f))
            .background(color = Color(0xFFEEF4FF), shape = RoundedCornerShape(28.dp))
            .border(width = 1.dp, color = Color(0xFFC7D8FF), shape = RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 이모지 대신 파란 원형 아이콘으로 앱 아이콘 스타일에 맞춤
        Box(
            modifier         = Modifier
                .size(34.dp)
                .background(Blue500, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✦", fontSize = 14.sp, color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text       = "+${event.earnedXp} XP",
                color      = Color(0xFF1E3A8A),
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text       = xpDetailText(event.message),
                color      = Color(0xFF4B6BB5),
                fontSize   = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun LevelUpDialog(
    event: UserStatsFeedback.LevelUp,
    onConfirm: () -> Unit
) {
    GrowthEventDialog(
        icon        = "🌟",
        title       = "레벨 업!",
        centerText  = "Lv. ${event.previousLevel} → Lv. ${event.currentLevel}",
        message     = "꾸준한 기록과 예산 관리로 성장했어요.",
        onConfirm   = onConfirm
    )
}

@Composable
private fun JobChangedDialog(
    event: UserStatsFeedback.JobChanged,
    onConfirm: () -> Unit
) {
    val previousTitle = UserStatsCalculator.jobTitle(event.previousJob)
    val centerText = if (previousTitle != event.currentJobTitle) {
        "$previousTitle → ${event.currentJobTitle}"
    } else {
        event.currentJobTitle
    }
    val defaultReason = "이번 달 소비 패턴이 반영되었어요."
    val detail = if (event.reason != defaultReason) event.reason else null

    GrowthEventDialog(
        icon       = "🎒",
        title      = "새 직업 획득!",
        centerText = centerText,
        message    = defaultReason,
        detail     = detail,
        onConfirm  = onConfirm
    )
}

@Composable
private fun GrowthEventDialog(
    icon: String,
    title: String,
    centerText: String,
    message: String,
    detail: String? = null,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onConfirm,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(dialogWindow) {
            if (dialogWindow != null) {
                val previousDimAmount = dialogWindow.attributes.dimAmount
                dialogWindow.setDimAmount(0.20f)
                dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                onDispose { dialogWindow.setDimAmount(previousDimAmount) }
            } else {
                onDispose { }
            }
        }

        AnimatedVisibility(
            visible = true,
            enter   = scaleIn(initialScale = 0.94f, animationSpec = tween(180)) + fadeIn(tween(180)),
            exit    = scaleOut(targetScale = 0.96f, animationSpec = tween(140)) + fadeOut(tween(140))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier  = Modifier
                        .fillMaxWidth(0.82f)
                        .widthIn(max = 328.dp),
                    shape     = RoundedCornerShape(24.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier              = Modifier
                            .background(Color.White)
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        horizontalAlignment   = Alignment.CenterHorizontally
                    ) {
                    // ── 아이콘 원형 — Blue50 단색 배경 ──────────────────
                    Box(
                        modifier         = Modifier
                            .size(58.dp)
                            .background(Color(0xFFF1F5FF), CircleShape)
                            .border(1.dp, Color(0xFFDCE7FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, fontSize = 25.sp)
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text       = title,
                        color      = Gray900,
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    // ── 레벨 chip — 이전/이후 명확히 구분 ────────────────
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val parts = centerText.split(" → ")
                        if (parts.size == 2) {
                            // 이전 레벨: 회색 chip
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF5F6F8), RoundedCornerShape(18.dp))
                                    .padding(horizontal = 13.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text       = parts[0],
                                    color      = Gray500,
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text     = "  →  ",
                                color    = Gray400,
                                fontSize = 14.sp
                            )
                            // 이후 레벨: Blue chip
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF1F5FF), RoundedCornerShape(18.dp))
                                    .border(1.dp, Color(0xFFD4E1FF), RoundedCornerShape(18.dp))
                                    .padding(horizontal = 13.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text       = parts[1],
                                    color      = Color(0xFF1E3A8A),
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF1F5FF), RoundedCornerShape(18.dp))
                                    .border(1.dp, Color(0xFFD4E1FF), RoundedCornerShape(18.dp))
                                    .padding(horizontal = 13.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text       = centerText,
                                    color      = Color(0xFF1E3A8A),
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text      = message,
                        color     = Gray600,
                        fontSize  = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )

                    if (!detail.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text       = detail,
                            color      = Gray500,
                            fontSize   = 13.sp,
                            lineHeight = 18.sp,
                            textAlign  = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // ── 확인 버튼 — Blue500 단색 ──────────────────────────
                    Button(
                        onClick          = onConfirm,
                        modifier         = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape            = RoundedCornerShape(14.dp),
                        colors           = ButtonDefaults.buttonColors(containerColor = Blue500.copy(alpha = 0.94f)),
                        elevation        = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        contentPadding   = PaddingValues(0.dp)
                    ) {
                        Text(
                            text       = "확인",
                            color      = Color.White,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    }
                }
            }
        }
    }
}

private fun xpDetailText(message: String): String {
    return message
        .replace(Regex("""^\+\d+\s*XP\s*획득!\s*"""), "")
        .replace("예산 관리 보너스가 적용되었어요.", "예산 관리 보너스가 반영되었어요.")
        .ifBlank { "소비 기록이 반영되었어요." }
}
