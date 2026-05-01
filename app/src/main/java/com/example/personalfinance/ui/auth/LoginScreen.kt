package com.example.personalfinance.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfinance.ui.components.KakaoIcon
import com.example.personalfinance.ui.components.GoogleIcon
import com.example.personalfinance.ui.theme.*

@Composable
fun LoginScreen(
    onKakaoLogin: () -> Unit = {},
    onGoogleLogin: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 375.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ─── Header Section ───────────────────────────────────────
                Spacer(modifier = Modifier.height(0.dp))

                Text(
                    text = "간편하게 로그인",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 26.sp,
                        color = TextPrimary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "아이디/비밀번호 입력이 필요 없어요!\nSNS 아이디로 빠르게 로그인/회원가입 하세요!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        lineHeight = 24.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // ─── Button Section ───────────────────────────────────────

                // Kakao Button
                SnsLoginButton(
                    text = "카카오로 시작하기",
                    backgroundColor = KakaoYellow,
                    textColor = TextPrimary,
                    icon = { KakaoIcon() },
                    onClick = onKakaoLogin
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Google Button
                SnsLoginButton(
                    text = "Google로 시작하기",
                    backgroundColor = Color(0xFFF1F1F1),
                    textColor = Color(0xFF1F1F1F),
                    icon = { GoogleIcon() },
                    onClick = onGoogleLogin
                )
            }
        }
    }
}

@Composable
fun SnsLoginButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 1f else 4f,
        animationSpec = tween(durationMillis = 150),
        label = "button_elevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "button_scale"
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                    color = textColor
                )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    PersonalFinanceTheme {
        LoginScreen()
    }
}
