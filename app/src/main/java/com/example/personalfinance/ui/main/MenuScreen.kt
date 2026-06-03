package com.example.personalfinance.ui.main

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.example.personalfinance.data.UserStatsCalculator
import com.example.personalfinance.data.UserStatsStore
import com.example.personalfinance.ui.theme.*

private data class MenuItem(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun MenuScreen(
    navController: NavController,
    onNotificationDebugClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val menuItems = listOf(
        MenuItem("friends",   "친구",     Icons.Rounded.People,      Blue400),
        MenuItem("shop",      "상점",     Icons.Rounded.Storefront,   CategoryShopping),
        MenuItem("gacha",     "가챠",     Icons.Rounded.AutoAwesome,  CategoryCulture),
        MenuItem("inventory", "인벤토리", Icons.Rounded.Inventory2,   CategoryFood),
    )

    val context    = LocalContext.current
    val store      = remember { UserStatsStore.getInstance(context) }
    val userStats  by store.statsFlow.collectAsState()
    val level      = userStats.currentLevel
    val xp         = userStats.currentXP
    val levelTitle = UserStatsCalculator.levelTitle(level)
    val currentJob = UserStatsCalculator.determineJob(userStats.categorySpending)
    val jobTitle   = UserStatsCalculator.jobTitle(currentJob)
    val itemCount  by store.transactionsFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Rounded.Close, null, tint = Gray600)
            }
            Text("메뉴", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(48.dp))
        }
        HorizontalDivider(color = Gray100)

        // ── Profile ───────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(64.dp)
                    .background(Brush.linearGradient(listOf(Blue50, Purple50)), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("👤", fontSize = 28.sp) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("사용자님", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) // 실제 이름은 백엔드 연동 시 추가
                Text(
                    "Lv.$level $levelTitle · $jobTitle",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = Gray500,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        HorizontalDivider(color = Gray100)

        // ── Menu Items ────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            menuItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Gray50)
                        .clickable {
                            when (item.id) {
                                "gacha"     -> navController.navigate(com.example.personalfinance.navigation.Screen.Gacha.route)
                                "inventory" -> navController.navigate(com.example.personalfinance.navigation.Screen.Inventory.route)
                            }
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier         = Modifier.size(48.dp).background(item.color.copy(0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, null, tint = item.color, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = Gray400)
                }
            }
        }

        // ── Settings ──────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                "설정",
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = Gray500,
                modifier   = Modifier.padding(bottom = 10.dp)
            )
            listOf(
                "결제 알림 테스트" to onNotificationDebugClick,
                "🎨 캐릭터 레이어 테스트" to { navController.navigate(com.example.personalfinance.navigation.Screen.CharacterLayerTest.route) },
                "알림 설정" to {},
                "프로필 수정" to {},
                "앱 정보" to {}
            ).forEach { (label, onClick) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Gray200, RoundedCornerShape(16.dp))
                        .clickable { onClick() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                    Icon(Icons.Rounded.ChevronRight, null, tint = Gray400)
                }
            }
            
            // 로그아웃 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFF0F0))
                    .clickable { onLogout() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("로그아웃", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFD32F2F))
                Icon(Icons.Rounded.Logout, null, tint = Color(0xFFD32F2F))
            }
        }

        // ── My Stats ──────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "나의 통계",
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = Gray500,
                modifier   = Modifier.padding(bottom = 12.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Blue50, Purple50)))
                    .padding(24.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("$level" to "레벨", "$xp" to "XP", "${itemCount.size}" to "아이템").forEach { (value, label) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(label, style = MaterialTheme.typography.bodySmall, color = Gray600, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
