package com.example.personalfinance.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.example.personalfinance.data.*
import com.example.personalfinance.network.ApiClient
import com.example.personalfinance.ui.theme.*
import kotlinx.coroutines.launch

// ── 필터 탭 정의 ──────────────────────────────────────────────────────────────

private sealed class InventoryFilter(val label: String) {
    object All       : InventoryFilter("전체")
    object Common    : InventoryFilter("Common")
    object Rare      : InventoryFilter("Rare")
    object Unique    : InventoryFilter("Unique")
    object Legendary : InventoryFilter("Legendary")
}

private val inventoryFilters = listOf(
    InventoryFilter.All,
    InventoryFilter.Common,
    InventoryFilter.Rare,
    InventoryFilter.Unique,
    InventoryFilter.Legendary,
)

// ── InventoryScreen ───────────────────────────────────────────────────────────

@Composable
fun InventoryScreen(navController: NavController) {
    val context      = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val gachaApi     = remember { ApiClient.getGachaApi(context, tokenManager) }
    val scope        = rememberCoroutineScope()

    var isLoading        by remember { mutableStateOf(true) }
    var ownedItemIds     by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedFilter   by remember { mutableStateOf<InventoryFilter>(InventoryFilter.All) }
    var errorMsg         by remember { mutableStateOf<String?>(null) }

    // 서버에서 보유 아이템 목록 조회
    LaunchedEffect(Unit) {
        try {
            val res = gachaApi.getUserGachaState()
            if (res.isSuccessful) {
                val raw = res.body()?.get("ownedItems")
                ownedItemIds = (raw as? List<*>)
                    ?.mapNotNull { it?.toString() }
                    ?.toSet() ?: emptySet()
            } else {
                errorMsg = "아이템 목록을 불러오지 못했습니다. (${res.code()})"
            }
        } catch (e: Exception) {
            errorMsg = "네트워크 오류: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // 필터 적용
    val filteredAll = remember(selectedFilter) {
        GachaItemPool.all.filter { item ->
            when (selectedFilter) {
                InventoryFilter.All       -> true
                InventoryFilter.Common    -> item.grade == GachaGrade.COMMON
                InventoryFilter.Rare      -> item.grade == GachaGrade.RARE
                InventoryFilter.Unique    -> item.grade == GachaGrade.UNIQUE
                InventoryFilter.Legendary -> item.grade == GachaGrade.LEGENDARY
            }
        }
    }

    val ownedCount = ownedItemIds.size
    val totalCount = GachaItemPool.all.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, Gray50)))
    ) {
        // ── 상단 헤더 ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "뒤로", tint = Gray600)
            }
            Text(
                text       = "인벤토리",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            // 보유 개수 뱃지
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(Blue50, Purple50)))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text       = "$ownedCount / $totalCount",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = Blue500,
                )
            }
        }
        HorizontalDivider(color = Gray100)

        if (isLoading) {
            // ── 로딩 ──────────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color      = Blue500,
                    modifier   = Modifier.size(36.dp),
                    strokeWidth = 3.dp,
                )
            }
        } else if (errorMsg != null) {
            // ── 에러 ──────────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFE4E6)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = Icons.Rounded.WifiOff,
                            contentDescription = null,
                            tint               = Color(0xFFDC2626),
                            modifier           = Modifier.size(32.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text       = "불러오기 실패",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = Gray700,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text      = errorMsg!!,
                        color     = Gray400,
                        style     = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            errorMsg = null
                            isLoading = true
                            scope.launch {
                                try {
                                    val res = gachaApi.getUserGachaState()
                                    if (res.isSuccessful) {
                                        val raw = res.body()?.get("ownedItems")
                                        ownedItemIds = (raw as? List<*>)
                                            ?.mapNotNull { it?.toString() }
                                            ?.toSet() ?: emptySet()
                                    } else {
                                        errorMsg = "아이템 목록을 불러오지 못했습니다. (${res.code()})"
                                    }
                                } catch (e: Exception) {
                                    errorMsg = "네트워크 오류: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        shape  = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue500,
                            contentColor   = Color.White,
                        ),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("다시 시도", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // ── 등급 필터 탭 ──────────────────────────────────────────────
                GradeFilterTabs(
                    filters        = inventoryFilters,
                    selectedFilter = selectedFilter,
                    onSelect       = { selectedFilter = it },
                )

                Spacer(Modifier.height(16.dp))

                // ── 아이템 그리드 ─────────────────────────────────────────────
                if (ownedItemIds.isEmpty() && selectedFilter == InventoryFilter.All) {
                    EmptyInventory()
                } else {
                    LazyVerticalGrid(
                        columns               = GridCells.Fixed(3),
                        verticalArrangement   = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding        = PaddingValues(bottom = 32.dp),
                        modifier              = Modifier.weight(1f),
                    ) {
                        items(filteredAll) { item ->
                            val owned = item.id in ownedItemIds
                            InventoryItemCard(item = item, owned = owned)
                        }
                    }
                }
            }
        }
    }
}

// ── 등급 필터 탭 ─────────────────────────────────────────────────────────────

@Composable
private fun GradeFilterTabs(
    filters: List<InventoryFilter>,
    selectedFilter: InventoryFilter,
    onSelect: (InventoryFilter) -> Unit,
) {
    val gradeColorMap = mapOf(
        "전체"      to Blue500,
        "Common"    to Color(0xFF9E9E9E),
        "Rare"      to Color(0xFF1976D2),
        "Unique"    to Color(0xFF7B1FA2),
        "Legendary" to Color(0xFFE65100),
    )

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { filter ->
            val isSelected = filter == selectedFilter
            val color = gradeColorMap[filter.label] ?: Gray500
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) color else color.copy(alpha = 0.08f)
                    )
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = filter.label,
                    color      = if (isSelected) Color.White else color,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

// ── 아이템 카드 ───────────────────────────────────────────────────────────────

@Composable
private fun InventoryItemCard(item: GachaItem, owned: Boolean) {
    val gradeColors = item.grade.gradientColors.map { Color(it) }

    val infiniteTransition = rememberInfiniteTransition(label = "itemGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (owned) 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (owned) 3.dp else 0.dp),
        border    = if (owned)
            BorderStroke(1.5.dp, Brush.linearGradient(gradeColors))
        else
            BorderStroke(1.dp, Gray100),
        modifier  = Modifier
            .fillMaxWidth()
            .scale(if (owned) glowScale else 1f),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 이미지 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (owned)
                            Brush.radialGradient(
                                colors = listOf(
                                    gradeColors.first().copy(alpha = 0.15f),
                                    gradeColors.last().copy(alpha = 0.05f)
                                )
                            )
                        else
                            Brush.radialGradient(
                                colors = listOf(Gray50, Gray100)
                            )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (owned) {
                    Image(
                        painter            = painterResource(id = item.drawableResId),
                        contentDescription = item.name,
                        modifier           = Modifier.size(56.dp),
                    )
                } else {
                    Icon(
                        imageVector        = Icons.Rounded.Lock,
                        contentDescription = "미보유",
                        tint               = Gray200,
                        modifier           = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // 등급 뱃지
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (owned)
                            Brush.horizontalGradient(gradeColors)
                        else
                            Brush.horizontalGradient(listOf(Gray200, Gray200))
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = item.grade.displayName.take(1),
                    color = if (owned) Color.White else Gray400,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(4.dp))

            // 아이템 이름
            Text(
                text      = if (owned) item.name else "???",
                style     = MaterialTheme.typography.labelSmall,
                color     = if (owned) Gray700 else Gray400,
                textAlign = TextAlign.Center,
                maxLines  = 2,
            )
        }
    }
}

// ── 빈 인벤토리 ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyInventory() {
    val infiniteTransition = rememberInfiniteTransition(label = "emptyFloat")
    val floatY by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = -10f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatAnim",
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(bottom = 80.dp),
        ) {
            // 아이콘 일러스트
            Box(
                modifier = Modifier
                    .offset(y = floatY.dp)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Blue50, Purple50)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Blue500, Purple500)
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.Inventory2,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(32.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text       = "아직 아이템이 없어요",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color      = Gray700,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = "가챠를 돌려서\n첫 번째 아이템을 모아보세요",
                style     = MaterialTheme.typography.bodyMedium,
                color     = Gray400,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )
        }
    }
}
