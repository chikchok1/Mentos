package com.example.personalfinance.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.personalfinance.data.TokenManager
import com.example.personalfinance.network.ApiClient
import com.example.personalfinance.ui.theme.Gray100
import com.example.personalfinance.ui.theme.Gray500
import com.example.personalfinance.ui.theme.Gray600
import com.example.personalfinance.ui.theme.Gray700
import kotlinx.coroutines.launch

@Composable
fun GachaDebugScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val gachaApi = remember { ApiClient.getGachaApi(context, tokenManager) }
    val coroutineScope = rememberCoroutineScope()

    var coinCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    // 화면 진입 시 서버에서 현재 코인 수 조회
    LaunchedEffect(Unit) {
        try {
            val res = gachaApi.getUserGachaState()
            if (res.isSuccessful) {
                coinCount = (res.body()?.get("coins") as? Number)?.toInt() ?: 0
            }
        } catch (e: Exception) {
            Toast.makeText(context, "코인 조회 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // ── 상단 헤더 ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "뒤로가기", tint = Gray600)
            }
            Text(
                "가챠 테스트",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            // 코인 새로고침 버튼
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val res = gachaApi.getUserGachaState()
                            if (res.isSuccessful) {
                                coinCount = (res.body()?.get("coins") as? Number)?.toInt() ?: 0
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "새로고침 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = "새로고침", tint = Gray600)
            }
        }

        HorizontalDivider(color = Gray100)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "이 화면은 개발·테스트 전용입니다. 코인을 서버에 직접 지급하여 가챠 기능을 검증할 수 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray700
            )

            // ── 코인 현황 패널 ─────────────────────────────────────────────
            CoinStatusPanel(coinCount = coinCount)

            // ── 코인 지급 버튼 ─────────────────────────────────────────────
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            val res = gachaApi.addCoinsForTest(100)
                            if (res.isSuccessful) {
                                val total = (res.body()?.get("totalCoins") as? Number)?.toInt() ?: coinCount
                                coinCount = total
                                Toast.makeText(context, "코인 100개 지급 완료! 현재: ${coinCount}개", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "지급 실패: ${res.code()}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE65100)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("처리 중...", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Rounded.MonetizationOn, contentDescription = null)
                    Text(
                        "코인 100개 지급받기",
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── 안내 패널 ──────────────────────────────────────────────────
            GachaDebugInfoPanel()
        }
    }
}

@Composable
private fun CoinStatusPanel(coinCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFFF8E1), Color(0xFFFFF3E0))
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFE65100),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "현재 보유 코인 (서버)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFBF360C)
            )
        }
        Text(
            text = "$coinCount",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE65100),
            fontSize = 56.sp
        )
        Text(
            "개",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFBF360C)
        )
    }
}

@Composable
private fun GachaDebugInfoPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Gray100, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "코인 사용 안내",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Gray700
        )
        Spacer(Modifier.height(4.dp))
        GachaInfoRow(label = "캡슐머신 1회 뽑기", value = "코인 10개")
        GachaInfoRow(label = "중복 아이템 (Common)", value = "코인 2개 보상")
        GachaInfoRow(label = "중복 아이템 (Rare)", value = "코인 5개 보상")
        GachaInfoRow(label = "중복 아이템 (Unique)", value = "코인 10개 보상")
        GachaInfoRow(label = "중복 아이템 (Legendary)", value = "코인 30개 보상")
    }
}

@Composable
private fun GachaInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Gray500)
        Text(value, style = MaterialTheme.typography.bodySmall, color = Gray700, fontWeight = FontWeight.Medium)
    }
}
