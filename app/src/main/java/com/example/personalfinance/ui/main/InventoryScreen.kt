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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.example.personalfinance.data.ShopStore
import com.example.personalfinance.data.CharacterAppearanceStore
import com.example.personalfinance.ui.components.CharacterLayerPreview
import com.example.personalfinance.ui.components.CharacterLayerState
import com.example.personalfinance.ui.theme.*
import kotlinx.coroutines.launch

// ── 카테고리 정의 ──────────────────────────────────────────────────────────────

private enum class InventoryCategory(
    val label: String,
    val folder: String,
) {
    FACE("얼굴",     "faces"),
    HAIR("헤어",     "hairs"),
    HAT ("모자",     "hats"),
    TOP ("상의",     "clothes"),   // filename startsWith "top"
    BOT ("하의",     "clothes"),   // filename startsWith "bot"
    ACC ("악세서리", "accessories"),
}

// ── 등급 필터 정의 ────────────────────────────────────────────────────────────

private sealed class GradeFilter(val label: String) {
    object All       : GradeFilter("전체")
    object Common    : GradeFilter("커먼")
    object Rare      : GradeFilter("레어")
    object Unique    : GradeFilter("유니크")
    object Legendary : GradeFilter("레전더리")
}

private val gradeFilters = listOf(
    GradeFilter.All,
    GradeFilter.Common,
    GradeFilter.Rare,
    GradeFilter.Unique,
    GradeFilter.Legendary,
)

// ── 등급 색상 ─────────────────────────────────────────────────────────────────

private fun gradeColor(grade: String): Color = when (grade) {
    "legendary" -> Color(0xFFE65100)
    "unique"    -> Color(0xFF7B1FA2)
    "rare"      -> Color(0xFF1976D2)
    else        -> Color(0xFF9E9E9E)
}

// ── Assets 파일 목록 조회 (ShopScreen 동일 로직) ──────────────────────────────

private fun listCategoryAssets(
    context: android.content.Context,
    category: InventoryCategory,
): List<Triple<String, String, String>> {   // (grade, categoryFolder, filename)
    val grades = listOf("common", "rare", "unique", "legendary")
    val result = mutableListOf<Triple<String, String, String>>()
    for (grade in grades) {
        val files = runCatching {
            context.assets.list("character_layers/$grade/${category.folder}")
                ?.filter { it.endsWith(".png") }
                ?.filter { file ->
                    when (category) {
                        InventoryCategory.TOP -> file.startsWith("top")
                        InventoryCategory.BOT -> file.startsWith("bot")
                        else -> true
                    }
                }
                ?.sorted()
                ?: emptyList()
        }.getOrDefault(emptyList())
        for (file in files) {
            result.add(Triple(grade, category.folder, file))
        }
    }
    return result
}

private fun CharacterLayerState.hasEquippedItem(): Boolean =
    listOf(face, hair, hat, accessory, topClothes, botClothes).any { !it.isNullOrBlank() }

// ── InventoryScreen ───────────────────────────────────────────────────────────

@Composable
fun InventoryScreen(navController: NavController) {
    val context         = LocalContext.current
    val shopStore       = remember { ShopStore.getInstance(context) }
    val appearanceStore = remember { CharacterAppearanceStore.getInstance(context) }

    val ownedItems   by shopStore.ownedItems.collectAsState()
    val equipped     by appearanceStore.appearanceFlow.collectAsState()

    val coroutine = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isLoading    by remember { mutableStateOf(true) }

    var selectedCategory by remember { mutableStateOf(InventoryCategory.FACE) }
    var selectedGrade    by remember { mutableStateOf<GradeFilter>(GradeFilter.All) }

    // 화면 진입 시 서버 동기화 (실패 시 로컬 데이터로 폴백)
    LaunchedEffect(Unit) {
        try {
            val localBeforeRestore = equipped
            shopStore.restoreFromServer()
            val restored = appearanceStore.restoreFromServer()
            val serverState = appearanceStore.appearanceFlow.value
            if (restored && localBeforeRestore.hasEquippedItem() && !serverState.hasEquippedItem()) {
                appearanceStore.saveWithServer(localBeforeRestore, shopStore.ownedItems.value)
            }
        } catch (e: Exception) {
            // 네트워크 오류 등은 무시하고 로컬 데이터로 동작
            android.util.Log.w("InventoryScreen", "서버 동기화 실패 (로컬 폴백): ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // 카테고리 내 전체 아이템 목록
    val categoryAssets = remember(selectedCategory) {
        listCategoryAssets(context, selectedCategory)
    }

    // 보유 + 등급 필터 적용
    val displayItems = remember(categoryAssets, ownedItems, selectedGrade) {
        categoryAssets.filter { (grade, folder, filename) ->
            val itemId = "$grade/$folder/$filename"
            val owned = ownedItems.contains(itemId)
            val gradeMatch = when (selectedGrade) {
                GradeFilter.All       -> true
                GradeFilter.Common    -> grade == "common"
                GradeFilter.Rare      -> grade == "rare"
                GradeFilter.Unique    -> grade == "unique"
                GradeFilter.Legendary -> grade == "legendary"
            }
            owned && gradeMatch
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F7F9))
                .padding(innerPadding)
        ) {
        // ── 상단 헤더 ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "뒤로", tint = Gray600)
                }
                Text(
                    text       = "인벤토리",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                // 보유 수량 뱃지
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF4F3FE))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text       = "보유 ${ownedItems.size}개",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFF534AB7),
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            // ── 카테고리 탭 ───────────────────────────────────────────────────
            val tabIndex = InventoryCategory.entries.indexOf(selectedCategory)
            ScrollableTabRow(
                selectedTabIndex = tabIndex,
                containerColor   = Color.White,
                contentColor     = Color(0xFF534AB7),
                edgePadding      = 12.dp,
                indicator        = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        color    = Color(0xFF534AB7),
                    )
                },
                divider = {},
            ) {
                InventoryCategory.entries.forEach { cat ->
                    Tab(
                        selected               = selectedCategory == cat,
                        onClick                = {
                            selectedCategory = cat
                            selectedGrade    = GradeFilter.All
                        },
                        selectedContentColor   = Color(0xFF534AB7),
                        unselectedContentColor = Color(0xFF888888),
                        text = {
                            Text(
                                text       = cat.label,
                                fontSize   = 13.sp,
                                fontWeight = if (selectedCategory == cat) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }
        }

        if (isLoading) {
            // ── 로딩 ──────────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color       = Color(0xFF534AB7),
                    modifier    = Modifier.size(36.dp),
                    strokeWidth = 3.dp,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
            ) {
                Spacer(Modifier.height(14.dp))

                // ── 등급 필터 칩 ──────────────────────────────────────────────
                GradeFilterChips(
                    selectedGrade = selectedGrade,
                    onSelect      = { selectedGrade = it },
                )

                Spacer(Modifier.height(14.dp))

                // ── 아이템 그리드 ─────────────────────────────────────────────
                if (displayItems.isEmpty()) {
                    EmptyCategoryView(category = selectedCategory, gradeFilter = selectedGrade)
                } else {
                    LazyVerticalGrid(
                        columns               = GridCells.Fixed(3),
                        verticalArrangement   = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding        = PaddingValues(bottom = 32.dp),
                        modifier              = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = displayItems,
                            key   = { (grade, folder, file) -> "$grade/$folder/$file" },
                        ) { (grade, folder, filename) ->
                                InventoryItemCard(
                                    grade      = grade,
                                    folder     = folder,
                                    filename   = filename,
                                    category   = selectedCategory,
                                    isEquipped = when (selectedCategory) {
                                        InventoryCategory.FACE -> equipped.face == filename
                                        InventoryCategory.HAIR -> equipped.hair == filename
                                        InventoryCategory.HAT  -> equipped.hat == filename
                                        InventoryCategory.TOP  -> equipped.topClothes == filename
                                        InventoryCategory.BOT  -> equipped.botClothes == filename
                                        InventoryCategory.ACC  -> equipped.accessory == filename
                                    },
                                    onEquip    = {
                                        val newAppearance = when (selectedCategory) {
                                            InventoryCategory.FACE -> equipped.copy(face = filename)
                                            InventoryCategory.HAIR -> equipped.copy(hair = filename)
                                            InventoryCategory.HAT  -> equipped.copy(hat = filename)
                                            InventoryCategory.TOP  -> equipped.copy(topClothes = filename)
                                            InventoryCategory.BOT  -> equipped.copy(botClothes = filename)
                                            InventoryCategory.ACC  -> equipped.copy(accessory = filename)
                                        }
                                        coroutine.launch {
                                            val saved = appearanceStore.saveWithServer(newAppearance, ownedItems)
                                            if (!saved) {
                                                snackbarHostState.showSnackbar("캐릭터 서버 저장에 실패했습니다.")
                                                return@launch
                                            }
                                            snackbarHostState.showSnackbar("장착 완료!")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 등급 필터 칩 ──────────────────────────────────────────────────────────────

private val gradeFilterColors = mapOf(
    "전체"      to Color(0xFF534AB7),
    "커먼"      to Color(0xFF9E9E9E),
    "레어"      to Color(0xFF1976D2),
    "유니크"    to Color(0xFF7B1FA2),
    "레전더리"  to Color(0xFFE65100),
)

@Composable
private fun GradeFilterChips(
    selectedGrade: GradeFilter,
    onSelect: (GradeFilter) -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        gradeFilters.forEach { filter ->
            val isSelected = filter == selectedGrade
            val color = gradeFilterColors[filter.label] ?: Color(0xFF534AB7)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) color else color.copy(alpha = 0.08f))
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
private fun InventoryItemCard(
    grade: String,
    folder: String,
    filename: String,
    category: InventoryCategory,
    isEquipped: Boolean,
    onEquip: () -> Unit,
) {
    val accent = gradeColor(grade)

    val thumbState = remember(grade, folder, filename) {
        when (category) {
            InventoryCategory.FACE -> CharacterLayerState(face       = filename)
            InventoryCategory.HAIR -> CharacterLayerState(hair       = filename)
            InventoryCategory.HAT  -> CharacterLayerState(hat        = filename)
            InventoryCategory.TOP  -> CharacterLayerState(topClothes = filename)
            InventoryCategory.BOT  -> CharacterLayerState(botClothes = filename)
            InventoryCategory.ACC  -> CharacterLayerState(accessory  = filename)
        }
    }

    val gradeBadgeLabel = when (grade) {
        "legendary" -> "L"
        "unique"    -> "U"
        "rare"      -> "R"
        else        -> "C"
    }

    Card(
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border    = BorderStroke(1.5.dp, accent.copy(alpha = 0.35f)),
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 이미지 영역
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF4F3FE))
                    .border(
                        width  = if (grade == "common") 0.dp else 1.5.dp,
                        color  = accent,
                        shape  = RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CharacterLayerPreview(
                    layerState = thumbState,
                    size       = maxWidth * 1.3f,
                )
            }

            Spacer(Modifier.height(6.dp))

            // 등급 뱃지 + 이름
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text       = gradeBadgeLabel,
                    fontSize   = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    modifier   = Modifier
                        .background(accent, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text     = ItemNames.display(filename),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color    = Color(0xFF333333),
                )
            }
            
            Spacer(Modifier.height(8.dp))
            if (isEquipped) {
                Text(
                    "장착 중",
                    fontSize = 10.sp,
                    color    = Color(0xFF0F6E56),
                    modifier = Modifier
                        .background(Color(0xFFD4F0E7), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            } else {
                Button(
                    onClick        = onEquip,
                    modifier       = Modifier.fillMaxWidth().height(28.dp),
                    shape          = RoundedCornerShape(8.dp),
                    colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D9E75)),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) {
                    Text("장착", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── 빈 카테고리 뷰 ────────────────────────────────────────────────────────────

@Composable
private fun EmptyCategoryView(category: InventoryCategory, gradeFilter: GradeFilter) {
    val infiniteTransition = rememberInfiniteTransition(label = "emptyFloat")
    val floatY by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = -10f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "float",
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(bottom = 80.dp),
        ) {
            Box(
                modifier = Modifier
                    .offset(y = floatY.dp)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFDEDCFD), Color(0xFFEEEDFE))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF534AB7), Color(0xFF9C6FE4))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.Inventory2,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(30.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            val msg = if (gradeFilter == GradeFilter.All) {
                "${category.label} 카테고리에\n보유한 아이템이 없어요"
            } else {
                "${category.label} · ${gradeFilter.label} 등급\n보유한 아이템이 없어요"
            }
            Text(
                text       = msg,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color      = Gray700,
                textAlign  = TextAlign.Center,
                lineHeight = 26.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = "상점에서 아이템을 구매해보세요",
                style     = MaterialTheme.typography.bodyMedium,
                color     = Gray400,
                textAlign = TextAlign.Center,
            )
        }
    }
}
