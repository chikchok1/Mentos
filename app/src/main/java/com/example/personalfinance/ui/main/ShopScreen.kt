package com.example.personalfinance.ui.main

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.personalfinance.data.CharacterAppearanceStore
import com.example.personalfinance.data.PurchaseResult
import com.example.personalfinance.data.ShopStore
import com.example.personalfinance.ui.components.CharacterLayerPreview
import com.example.personalfinance.ui.components.CharacterLayerState
import kotlinx.coroutines.launch

// ── 탭 정의 ──────────────────────────────────────────────────────────────────

private enum class ShopTab(val label: String) {
    NEW("✦ 신상"),
    FACE("얼굴"),
    HAIR("헤어"),
    HAT("모자"),
    TOP("상의"),
    BOT("하의"),
    ACC("악세서리"),
}

// ── 아이템 모델 ───────────────────────────────────────────────────────────────

private data class ShopItem(
    val grade: String,
    val categoryFolder: String,
    val filename: String,
    val displayName: String,
    val price: Int,
    val category: ShopTab,
) {
    val folder: String get() = "$grade/$categoryFolder"
}

// ── 유틸 ─────────────────────────────────────────────────────────────────────

private fun listAssets(
    context: android.content.Context,
    categoryFolder: String
): List<Pair<String, String>> {
    val grades = listOf("common", "rare", "unique", "legendary")
    val result = mutableListOf<Pair<String, String>>()

    for (grade in grades) {
        val items = runCatching {
            context.assets.list("character_layers/$grade/$categoryFolder")
                ?.filter { it.endsWith(".png") }
                ?.sorted()
                ?: emptyList()
        }.getOrDefault(emptyList())

        for (item in items) {
            result.add(grade to item)
        }
    }

    return result
}

/**
 * previewAppearance에서 해당 카테고리 레이어만 교체한 새 상태를 반환.
 * ShopTab.NEW 는 카테고리 정보가 없으므로 변경 없이 반환.
 */
private fun CharacterLayerState.withItem(item: ShopItem): CharacterLayerState = when (item.category) {
    ShopTab.FACE -> copy(face       = item.filename)
    ShopTab.HAIR -> copy(hair       = item.filename)
    ShopTab.HAT  -> copy(hat        = item.filename)
    ShopTab.TOP  -> copy(topClothes = item.filename)
    ShopTab.BOT  -> copy(botClothes = item.filename)
    ShopTab.ACC  -> copy(accessory  = item.filename)
    ShopTab.NEW  -> this
}

/**
 * previewAppearance에서 해당 카테고리 레이어를 equipped의 동일 부위로 되돌린다.
 * "현재 장착 중인 아이템을 다시 클릭하면 원래대로 돌아감" 동작에 사용.
 */
private fun CharacterLayerState.resetSlot(
    item: ShopItem,
    equipped: CharacterLayerState
): CharacterLayerState = when (item.category) {
    ShopTab.FACE -> copy(face       = equipped.face)
    ShopTab.HAIR -> copy(hair       = equipped.hair)
    ShopTab.HAT  -> copy(hat        = equipped.hat)
    ShopTab.TOP  -> copy(topClothes = equipped.topClothes)
    ShopTab.BOT  -> copy(botClothes = equipped.botClothes)
    ShopTab.ACC  -> copy(accessory  = equipped.accessory)
    ShopTab.NEW  -> this
}

/** 해당 아이템이 현재 preview 상태에서 해당 슬롯에 적용 중인지 확인 */
private fun CharacterLayerState.isPreviewedSlot(item: ShopItem): Boolean = when (item.category) {
    ShopTab.FACE -> face       == item.filename
    ShopTab.HAIR -> hair       == item.filename
    ShopTab.HAT  -> hat        == item.filename
    ShopTab.TOP  -> topClothes == item.filename
    ShopTab.BOT  -> botClothes == item.filename
    ShopTab.ACC  -> accessory  == item.filename
    ShopTab.NEW  -> false
}

/** 기본 캐릭터(빈 상태) + 해당 아이템만 → 썸네일 카드용 */
private fun ShopItem.toThumbState(): CharacterLayerState = when (category) {
    ShopTab.FACE -> CharacterLayerState(face       = filename)
    ShopTab.HAIR -> CharacterLayerState(hair       = filename)
    ShopTab.HAT  -> CharacterLayerState(hat        = filename)
    ShopTab.TOP  -> CharacterLayerState(topClothes = filename)
    ShopTab.BOT  -> CharacterLayerState(botClothes = filename)
    ShopTab.ACC  -> CharacterLayerState(accessory  = filename)
    ShopTab.NEW  -> CharacterLayerState()
}

// ── ShopScreen ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(navController: NavController) {
    val context         = LocalContext.current
    val shopStore       = remember { ShopStore.getInstance(context) }
    val appearanceStore = remember { CharacterAppearanceStore.getInstance(context) }

    val coins      by shopStore.coins.collectAsState()
    val ownedItems by shopStore.ownedItems.collectAsState()
    val equipped   by appearanceStore.appearanceFlow.collectAsState()

    val coroutine = rememberCoroutineScope()
    val snackbar  = remember { SnackbarHostState() }

    // ── 미리보기 상태 (실제 장착과 분리) ──────────────────────────────────────
    //
    // previewAppearance : 상점에서만 사용하는 임시 미리보기 상태.
    //   - 화면 최초 진입 시 equipped(실제 장착)로 초기화
    //   - 아이템 클릭 시에만 변경 (탭 전환 시 초기화 안 함)
    //   - 적용 버튼 클릭 시 equipped에 저장
    //   - 뒤로가기/이탈 시 아무것도 저장되지 않음
    //
    // equippedSnapshot : 상점 진입 시점의 실제 장착 상태 고정값.
    //   "현재 장착 중인 아이템을 다시 누르면 해당 부위를 원래대로 복원" 에 사용.
    var previewAppearance  by remember { mutableStateOf(equipped) }
    val equippedSnapshot   by remember { mutableStateOf(equipped) }   // 진입 시점 스냅샷

    // equipped가 상점 밖에서 변경된 경우(예: 다른 화면에서 저장)를 대비해
    // 최초 1회만 previewAppearance를 초기화한다.
    var isPreviewInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!isPreviewInitialized) {
            previewAppearance     = equipped
            isPreviewInitialized  = true
        }
        shopStore.restoreFromServer()
    }

    var selectedTab by remember { mutableStateOf(ShopTab.NEW) }

    // ── 아이템 목록 ───────────────────────────────────────────────────────────
    val allItems: Map<ShopTab, List<ShopItem>> = remember {
        fun build(categoryFolder: String, tab: ShopTab) =
            listAssets(context, categoryFolder).map { (grade, file) ->
                ShopItem(
                    grade          = grade,
                    categoryFolder = categoryFolder,
                    filename       = file,
                    displayName    = ItemNames.display(file),
                    price          = shopStore.priceOf(grade),
                    category       = tab
                )
            }

        val faceItems = build("faces",       ShopTab.FACE)
        val hairItems = build("hairs",       ShopTab.HAIR)
        val hatItems  = build("hats",        ShopTab.HAT)
        val topItems  = build("clothes",     ShopTab.TOP).filter { it.filename.startsWith("top") }
        val botItems  = build("clothes",     ShopTab.BOT).filter { it.filename.startsWith("bot") }
        val accItems  = build("accessories", ShopTab.ACC)

        val newItems = (faceItems + hairItems + hatItems + topItems + botItems + accItems)
            .filter { shopStore.isNew(it.filename) }

        mapOf(
            ShopTab.NEW  to newItems,
            ShopTab.FACE to faceItems,
            ShopTab.HAIR to hairItems,
            ShopTab.HAT  to hatItems,
            ShopTab.TOP  to topItems,
            ShopTab.BOT  to botItems,
            ShopTab.ACC  to accItems,
        )
    }

    val currentItems = allItems[selectedTab] ?: emptyList()

    // ── 아이템 클릭 핸들러 ────────────────────────────────────────────────────
    //
    // 동작 규칙:
    //   1. 현재 preview에 이미 해당 슬롯·파일이 적용 중인 경우
    //      → 실제 장착(equippedSnapshot) 값으로 해당 슬롯을 되돌림
    //   2. 그 외
    //      → previewAppearance의 해당 슬롯만 교체 (다른 슬롯은 유지)
    fun handleItemClick(item: ShopItem) {
        previewAppearance = if (previewAppearance.isPreviewedSlot(item)) {
            // 같은 아이템을 다시 클릭 → 해당 슬롯을 장착 상태로 복원
            previewAppearance.resetSlot(item, equippedSnapshot)
        } else {
            // 다른 아이템 클릭 → 해당 슬롯만 교체, 나머지 preview 조합 유지
            previewAppearance.withItem(item)
        }
    }

    // ── 구매 핸들러 ───────────────────────────────────────────────────────────
    fun handleBuy(item: ShopItem) {
        coroutine.launch {
            val result = shopStore.purchaseWithServer(
                folder   = item.folder,
                filename = item.filename,
                price    = item.price
            )
            when (result) {
                is PurchaseResult.Success          -> snackbar.showSnackbar("${item.displayName} 구매 완료!")
                is PurchaseResult.AlreadyOwned     -> snackbar.showSnackbar("이미 보유 중인 아이템이에요")
                is PurchaseResult.InsufficientCoins -> snackbar.showSnackbar("코인이 부족해요")
                is PurchaseResult.Error            -> snackbar.showSnackbar(result.message)
            }
        }
    }

    // ── 적용 핸들러 ───────────────────────────────────────────────────────────
    // previewAppearance를 실제 장착 상태로 저장 (서버 동기화 포함)
    fun handleApply() {
        coroutine.launch {
            val success = appearanceStore.saveWithServer(previewAppearance, ownedItems)
            if (success) {
                snackbar.showSnackbar("캐릭터 장착 상태가 저장되었습니다!")
            } else {
                // 서버 저장 실패해도 로컬에는 이미 반영됨(saveWithServer 내부에서 save() 선호출)
                snackbar.showSnackbar("저장되었습니다 (서버 동기화 실패)")
            }
        }
    }

    // ── 미리보기 변경 여부 (적용 버튼 활성화 판단) ────────────────────────────
    val isPreviewChanged = previewAppearance != equipped

    // ── UI ───────────────────────────────────────────────────────────────────
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
                    Text("상점", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier          = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF4F3FE))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🪙", fontSize = 14.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text       = "%,d".format(coins),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFF534AB7),
                        )
                    }
                }

                val tabIndex = ShopTab.entries.indexOf(selectedTab)
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
                    divider = { HorizontalDivider(color = Color(0xFFEEEEEE)) },
                ) {
                    ShopTab.entries.forEach { tab ->
                        Tab(
                            selected               = selectedTab == tab,
                            // 탭 전환 시 previewAppearance를 초기화하지 않음
                            onClick                = { selectedTab = tab },
                            selectedContentColor   = Color(0xFF534AB7),
                            unselectedContentColor = Color(0xFF888888),
                            text = {
                                Text(
                                    text       = tab.label,
                                    fontSize   = 13.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
            }
        },
        snackbarHost   = { SnackbarHost(snackbar) },
        containerColor = Color(0xFFF7F7F9),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── 미리보기 패널 ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFDEDCFD), Color(0xFFEEEDFE)),
                            radius = 600f,
                        )
                    )
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // previewAppearance를 캐릭터 미리보기에 전달
                    CharacterLayerPreview(
                        layerState = previewAppearance,
                        size       = 160.dp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text          = if (isPreviewChanged) "미리보기" else "착용 중",
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.Medium,
                        color         = Color(0xFF7F77DD),
                        letterSpacing = 0.5.sp,
                    )

                    // 적용 버튼: 미리보기가 실제 장착 상태와 다를 때만 표시
                    if (isPreviewChanged) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // 되돌리기 버튼
                            OutlinedButton(
                                onClick = { previewAppearance = equipped },
                                shape   = RoundedCornerShape(12.dp),
                                colors  = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF534AB7)
                                ),
                                border  = BorderStroke(1.dp, Color(0xFF534AB7)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text("되돌리기", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            // 적용 버튼
                            Button(
                                onClick        = { handleApply() },
                                shape          = RoundedCornerShape(12.dp),
                                colors         = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF534AB7)
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text("장착 적용", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ── 아이템 그리드 ─────────────────────────────────────────────────
            LazyVerticalGrid(
                columns               = GridCells.Fixed(3),
                modifier              = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalArrangement   = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding        = PaddingValues(bottom = 16.dp),
            ) {
                items(currentItems, key = { "${it.folder}/${it.filename}" }) { item ->
                    val isOwned = ownedItems.contains("${item.folder}/${item.filename}")
                    val isNew   = shopStore.isNew(item.filename)

                    // isPreviewed: previewAppearance에서 해당 슬롯에 이 아이템이 적용 중인지
                    val isPreviewed = previewAppearance.isPreviewedSlot(item)

                    // isEquipped: 실제 장착(equipped) 상태 기준
                    val isEquipped = when (item.category) {
                        ShopTab.FACE -> equipped.face       == item.filename
                        ShopTab.HAIR -> equipped.hair       == item.filename
                        ShopTab.HAT  -> equipped.hat        == item.filename
                        ShopTab.TOP  -> equipped.topClothes == item.filename
                        ShopTab.BOT  -> equipped.botClothes == item.filename
                        ShopTab.ACC  -> equipped.accessory  == item.filename
                        ShopTab.NEW  -> false
                    }

                    val thumbState = remember(item) { item.toThumbState() }

                    ShopItemCard(
                        item        = item,
                        thumbState  = thumbState,
                        isOwned     = isOwned,
                        isNew       = isNew,
                        isPreviewed = isPreviewed,
                        isEquipped  = isEquipped,
                        onClick     = { handleItemClick(item) },
                        onBuy       = { handleBuy(item) },
                    )
                }
            }
        }
    }
}

// ── 아이템 카드 ───────────────────────────────────────────────────────────────

@Composable
private fun ShopItemCard(
    item: ShopItem,
    thumbState: CharacterLayerState,
    isOwned: Boolean,
    isNew: Boolean,
    isPreviewed: Boolean,   // 현재 previewAppearance에 적용 중
    isEquipped: Boolean,    // 실제 장착 중
    onClick: () -> Unit,
    onBuy: () -> Unit,
) {
    val borderColor = when {
        isPreviewed -> Color(0xFF534AB7)
        isEquipped  -> Color(0xFF5DCAA5)
        isOwned     -> Color(0xFFAFA9EC)
        else        -> Color(0xFFE8E8E8)
    }
    val borderWidth = if (isPreviewed || isEquipped) 1.5.dp else 0.5.dp
    val bgColor = when {
        isPreviewed -> Color(0xFFEEEDFE)
        isEquipped  -> Color(0xFFE1F5EE)
        else        -> Color.White
    }

    val gradeColor = when (item.grade) {
        "legendary" -> Color(0xFFE65100)
        "unique"    -> Color(0xFF7B1FA2)
        "rare"      -> Color(0xFF1976D2)
        else        -> Color(0xFFE8E8E8)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Column {
            BoxWithConstraints(
                modifier         = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF4F3FE))
                    .border(if (item.grade == "common") 0.dp else 1.5.dp, gradeColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                CharacterLayerPreview(layerState = thumbState, size = maxWidth * 1.3f)
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text     = item.displayName,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color    = Color(0xFF222222),
            )

            Spacer(Modifier.height(4.dp))

            when {
                isEquipped -> {
                    Text(
                        "장착 중",
                        fontSize = 9.sp,
                        color    = Color(0xFF0F6E56),
                        modifier = Modifier
                            .background(Color(0xFFD4F0E7), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                isOwned -> {
                    // 보유 중이지만 preview에 적용됐을 때도 구매 버튼 없이 표시
                    Text(
                        if (isPreviewed) "미리보기 중" else "보유중인 항목",
                        fontSize = 9.sp,
                        color    = Color(0xFF534AB7),
                        modifier = Modifier
                            .background(Color(0xFFF4F3FE), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                isPreviewed -> {
                    // 미소유 아이템인데 preview에 올라간 경우 → 구매 버튼 표시
                    Button(
                        onClick        = onBuy,
                        modifier       = Modifier
                            .fillMaxWidth()
                            .height(26.dp),
                        shape          = RoundedCornerShape(8.dp),
                        colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFF534AB7)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) {
                        Text("🪙 ${item.price}", fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isNew) {
                            Text(
                                "NEW",
                                fontSize = 8.sp,
                                color    = Color(0xFF993C1D),
                                modifier = Modifier
                                    .background(Color(0xFFFAECE7), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                        }
                        val gradeLabel = when (item.grade) {
                            "legendary" -> Color(0xFFE65100)
                            "unique"    -> Color(0xFF7B1FA2)
                            "rare"      -> Color(0xFF1976D2)
                            else        -> Color(0xFF9E9E9E)
                        }
                        Text(
                            item.grade.take(1).uppercase(),
                            fontSize   = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                            modifier   = Modifier
                                .background(gradeLabel, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "🪙 ${item.price}",
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFF534AB7),
                        )
                    }
                }
            }
        }
    }
}
