package com.example.personalfinance.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.personalfinance.data.ShopStore
import com.example.personalfinance.data.TokenManager
import com.example.personalfinance.network.ApiClient
import kotlinx.coroutines.launch

/**
 * 출석 체크 화면.
 *
 * - 오늘 출석 가능한 경우: "오늘 출석하고 코인 받기" 버튼 표시
 * - 이미 출석한 경우: "오늘은 이미 출석했어요" 상태 표시
 * - 출석 성공 시: 지급 코인 + 보유 코인 표시
 * - 월 예산 성공 보상이 함께 지급된 경우 별도 메시지 표시
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(navController: NavController) {
    val context       = LocalContext.current
    val shopStore     = remember { ShopStore.getInstance(context) }
    val tokenManager  = remember { TokenManager(context) }
    val coinRewardApi = remember { ApiClient.getCoinRewardApi(context, tokenManager) }
    val coroutine     = rememberCoroutineScope()
    val snackbar      = remember { SnackbarHostState() }

    // ── 화면 상태 ─────────────────────────────────────────────────────────────
    var isLoading       by remember { mutableStateOf(true) }
    var checkedToday    by remember { mutableStateOf(false) }
    var totalCoins      by remember { mutableStateOf(0) }

    // 출석 성공 직후 결과 표시용
    var justChecked        by remember { mutableStateOf(false) }
    var attendanceCoins    by remember { mutableStateOf(0) }
    var budgetRewarded     by remember { mutableStateOf(false) }
    var budgetRewardCoins  by remember { mutableStateOf(0) }

    // 화면 진입 시 오늘 출석 여부 조회
    LaunchedEffect(Unit) {
        try {
            val resp = coinRewardApi.getAttendanceStatus()
            if (resp.isSuccessful) {
                val body    = resp.body()
                checkedToday = body?.get("checkedToday") as? Boolean ?: false
                totalCoins   = (body?.get("totalCoins") as? Number)?.toInt() ?: 0
            }
        } catch (e: Exception) {
            // 네트워크 오류 시 조용히 처리
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "뒤로가기")
                    }
                    Text("출석 체크", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    // 코인 표시
                    Row(
                        modifier          = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFFF8E1))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector        = Icons.Rounded.MonetizationOn,
                            contentDescription = null,
                            tint               = Color(0xFFFFB800),
                            modifier           = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text       = "%,d".format(totalCoins),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFFE65100),
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFEEEEEE))
            }
        },
        snackbarHost   = { SnackbarHost(snackbar) },
        containerColor = Color(0xFFF7F7F9),
    ) { innerPadding ->

        if (isLoading) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color(0xFF534AB7))
            }
            return@Scaffold
        }

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))

            // ── 상태 아이콘 ───────────────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        if (checkedToday) Color(0xFFE8F5E9) else Color(0xFFEEEDFE)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (checkedToday) {
                    Icon(
                        imageVector        = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint               = Color(0xFF43A047),
                        modifier           = Modifier.size(54.dp),
                    )
                } else {
                    Text("🎁", fontSize = 44.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── 메인 메시지 ───────────────────────────────────────────────────
            if (checkedToday && !justChecked) {
                // 이미 출석한 상태
                Text(
                    text       = "오늘은 이미 출석했어요",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF222222),
                    textAlign  = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text      = "내일 다시 출석하면 코인을 받을 수 있어요",
                    fontSize  = 14.sp,
                    color     = Color(0xFF888888),
                    textAlign = TextAlign.Center,
                )

            } else if (justChecked) {
                // 방금 출석 성공
                Text(
                    text       = "출석 완료! 🎉",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF222222),
                    textAlign  = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))

                // 출석 보상 카드
                RewardCard(
                    label  = "출석 보상",
                    coins  = attendanceCoins,
                    color  = Color(0xFF534AB7),
                )

                // 예산 성공 보상 카드 (지급된 경우에만)
                if (budgetRewarded) {
                    Spacer(Modifier.height(10.dp))
                    RewardCard(
                        label  = "지난달 예산 성공 보너스 🏆",
                        coins  = budgetRewardCoins,
                        color  = Color(0xFF2E7D32),
                    )
                }

                Spacer(Modifier.height(20.dp))

                // 현재 보유 코인
                Row(
                    modifier          = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFF8E1))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.MonetizationOn,
                        contentDescription = null,
                        tint               = Color(0xFFFFB800),
                        modifier           = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = "현재 보유: %,d 코인".format(totalCoins),
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFFE65100),
                    )
                }

            } else {
                // 출석 가능 상태
                Text(
                    text       = "오늘의 출석 보상",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF222222),
                    textAlign  = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text      = "매일 출석하면 코인을 받을 수 있어요",
                    fontSize  = 14.sp,
                    color     = Color(0xFF888888),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))

                // 보상 안내 카드
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFEEEDFE))
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.MonetizationOn,
                        contentDescription = null,
                        tint               = Color(0xFF534AB7),
                        modifier           = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text       = "+20 코인",
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF534AB7),
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── 출석 버튼 ─────────────────────────────────────────────────────
            if (!checkedToday) {
                Button(
                    onClick = {
                        coroutine.launch {
                            try {
                                val resp = coinRewardApi.checkAttendance()
                                if (resp.isSuccessful) {
                                    val body = resp.body()
                                    val alreadyChecked = body?.get("alreadyChecked") as? Boolean ?: false
                                    if (alreadyChecked) {
                                        checkedToday = true
                                    } else {
                                        attendanceCoins   = (body?.get("attendanceCoins") as? Number)?.toInt() ?: 0
                                        budgetRewarded    = body?.get("budgetRewarded") as? Boolean ?: false
                                        budgetRewardCoins = (body?.get("budgetRewardCoins") as? Number)?.toInt() ?: 0
                                        totalCoins        = (body?.get("totalCoins") as? Number)?.toInt() ?: 0

                                        // ShopStore 코인 서버와 동기화
                                        shopStore.restoreFromServer()

                                        checkedToday = true
                                        justChecked  = true
                                    }
                                } else {
                                    snackbar.showSnackbar("출석 처리에 실패했어요")
                                }
                            } catch (e: Exception) {
                                snackbar.showSnackbar("네트워크 오류가 발생했어요")
                            }
                        }
                    },
                    modifier       = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape          = RoundedCornerShape(16.dp),
                    colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFF534AB7)),
                ) {
                    Text(
                        text       = "오늘 출석하고 코인 받기  🎁",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // 보상 안내 텍스트
            Spacer(Modifier.height(24.dp))
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                Text(
                    text       = "코인 획득 방법",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF444444),
                )
                Spacer(Modifier.height(8.dp))
                RewardInfoRow("🗓️ 매일 출석 체크",              "+20 코인")
                RewardInfoRow("⬆️ 레벨업 (레벨당)",             "+30 코인")
                RewardInfoRow("🏆 지난달 예산 성공 (월 1회)",   "+200 코인")
            }
        }
    }
}

// ── 보상 카드 컴포넌트 ─────────────────────────────────────────────────────────

@Composable
private fun RewardCard(label: String, coins: Int, color: Color) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text      = label,
            fontSize  = 14.sp,
            color     = color,
            fontWeight = FontWeight.Medium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector        = Icons.Rounded.MonetizationOn,
                contentDescription = null,
                tint               = color,
                modifier           = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text       = "+$coins",
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = color,
            )
        }
    }
}

// ── 보상 안내 행 컴포넌트 ─────────────────────────────────────────────────────

@Composable
private fun RewardInfoRow(label: String, value: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label,  fontSize = 12.sp, color = Color(0xFF666666))
        Text(text = value,  fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF534AB7))
    }
}
