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
import com.example.personalfinance.data.ShopStore
import com.example.personalfinance.ui.components.CharacterLayerPreview
import com.example.personalfinance.ui.components.CharacterLayerState
import kotlinx.coroutines.launch

// ── 탭 정의 ──────────────────────────────────────────────────────────────────

private enum class ShopTab(val label: String) {
    NEW("✦ 신상"),
    HAIR("헤어"),
    HAT("모자"),
    TOP("상의"),
    BOT("하의"),
    ACC("악세서리"),
}

// ── 아이템 모델 ───────────────────────────────────────────────────────────────

private data class ShopItem(
    val folder: String,
    val filename: String,
    val displayName: String,
    val price: Int,
    val category: ShopTab,
)

// ── 유틸 ─────────────────────────────────────────────────────────────────────

private fun assetDisplayName(filename: String): String {
    val noExt = filename.removeSuffix(".png")
    val stripped = noExt
        .replace(Regex("^top\\d+_"), "")
        .replace(Regex("^bot\\d+_"), "")
        .replace(Regex("^[hta]_"), "")
    return stripped.replace("_", " ").replaceFirstChar { it.uppercase() }.ifBlank { noExt }
}

private fun listAssets(context: android.content.Context, folder: String): List<String> =
    runCatching {
        context.assets.list("character_layers/$folder")
            ?.filter { it.endsWith(".png") }
            ?.sorted()
            ?: emptyList()
    }.getOrDefault(emptyList())

/** 장착 상태에서 해당 카테고리 레이어만 교체 → 미리보기 패널용 */
private fun CharacterLayerState.withItem(item: ShopItem): CharacterLayerState = when (item.category) {
    ShopTab.HAIR -> copy(hair       = item.filename)
    ShopTab.HAT  -> copy(hat        = item.filename)
    ShopTab.TOP  -> copy(topClothes = item.filename)
    ShopTab.BOT  -> copy(botClothes = item.filename)
    ShopTab.ACC  -> copy(accessory  = item.filename)
    ShopTab.NEW  -> this
}

/** 기본 캐릭터(빈 상태) + 해당 아이템만 → 썸네일 카드용 */
private fun ShopItem.toThumbState(): CharacterLayerState = when (category) {
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

    var selectedTab  by remember { mutableStateOf(ShopTab.NEW) }
    var selectedItem by remember { mutableStateOf<ShopItem?>(null) }

    // 미리보기: 현재 장착 상태 + 선택 아이템 레이어만 오버레이
    val previewState: CharacterLayerState = remember(equipped, selectedItem) {
        val item = selectedItem ?: return@remember equipped
        equipped.withItem(item)
    }

    // ── 아이템 목록 빌드 ──────────────────────────────────────────────────────
    val allItems: Map<ShopTab, List<ShopItem>> = remember {
        fun build(folder: String, tab: ShopTab) =
            listAssets(context, folder).map { file ->
                ShopItem(folder, file, assetDisplayName(file), shopStore.priceOf(file), tab)
            }

        val hairItems = build("hairs",       ShopTab.HAIR)
        val hatItems  = build("hats",        ShopTab.HAT)
        val topItems  = build("clothes",     ShopTab.TOP).filter { it.filename.startsWith("top") }
        val botItems  = build("clothes",     ShopTab.BOT).filter { it.filename.startsWith("bot") }
        val accItems  = build("accessories", ShopTab.ACC)
        val newItems  = (hairItems + hatItems + topItems + botItems + accItems)
            .filter { shopStore.isNew(it.filename) }

        mapOf(
            ShopTab.NEW  to newItems,
            ShopTab.HAIR to hairItems,
            ShopTab.HAT  to hatItems,
            ShopTab.TOP  to topItems,
            ShopTab.BOT  to botItems,
            ShopTab.ACC  to accItems,
        )
    }

    val currentItems = allItems[selectedTab] ?: emptyList()

    // ── 구매 핸들러 ───────────────────────────────────────────────────────────
    fun handleBuy(item: ShopItem) {
        if (ownedItems.contains("${item.folder}/${item.filename}")) return
        val success = shopStore.spendCoins(item.price)
        if (success) {
            shopStore.addOwned(item.folder, item.filename)
            appearanceStore.save(equipped.withItem(item))
            coroutine.launch { snackbar.showSnackbar("${item.displayName} 구매 & 장착 완료!") }
        } else {
            coroutine.launch { snackbar.showSnackbar("코인이 부족해요") }
        }
    }

    // ── 장착 핸들러 ───────────────────────────────────────────────────────────
    fun handleEquip(item: ShopItem) {
        appearanceStore.save(equipped.withItem(item))
        coroutine.launch { snackbar.showSnackbar("${item.displayName} 장착 완료!") }
    }

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
                            onClick                = {
                                selectedTab  = tab
                                selectedItem = null
                            },
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
            // ── 미리보기 패널 (장착 상태 누적) ───────────────────────────────
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
                    .padding(vertical = 32.dp),  // ← 20→32 (패널 세로 여유 확보)
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CharacterLayerPreview(
                        layerState = previewState,
                        size       = 200.dp,  // ← 120→200 (캐릭터 크기 확대)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text          = if (selectedItem != null) "미리보기" else "착용 중",
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.Medium,
                        color         = Color(0xFF7F77DD),
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            // ── 아이템 그리드 (기본 캐릭터 + 해당 아이템만) ──────────────────
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
                    val isOwned    = ownedItems.contains("${item.folder}/${item.filename}")
                    val isNew      = shopStore.isNew(item.filename)
                    val isSelected = selectedItem?.filename == item.filename
                    val isEquipped = when (item.category) {
                        ShopTab.HAIR -> equipped.hair       == item.filename
                        ShopTab.HAT  -> equipped.hat        == item.filename
                        ShopTab.TOP  -> equipped.topClothes == item.filename
                        ShopTab.BOT  -> equipped.botClothes == item.filename
                        ShopTab.ACC  -> equipped.accessory  == item.filename
                        ShopTab.NEW  -> false
                    }

                    // ★ 썸네일: 기본 캐릭터 + 이 아이템만 (equipped 무관)
                    val thumbState = remember(item) { item.toThumbState() }

                    ShopItemCard(
                        item       = item,
                        thumbState = thumbState,
                        isOwned    = isOwned,
                        isNew      = isNew,
                        isSelected = isSelected,
                        isEquipped = isEquipped,
                        onClick    = { selectedItem = item },
                        onBuy      = { handleBuy(item) },
                        onEquip    = { handleEquip(item) },
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
    isSelected: Boolean,
    isEquipped: Boolean,
    onClick: () -> Unit,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
) {
    val borderColor = when {
        isSelected -> Color(0xFF534AB7)
        isEquipped -> Color(0xFF5DCAA5)
        isOwned    -> Color(0xFFAFA9EC)
        else       -> Color(0xFFE8E8E8)
    }
    val borderWidth = if (isSelected || isEquipped) 1.5.dp else 0.5.dp
    val bgColor = when {
        isSelected -> Color(0xFFEEEDFE)
        isEquipped -> Color(0xFFE1F5EE)
        else       -> Color.White
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
                    .background(Color(0xFFF4F3FE)),
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
                isOwned && isSelected -> {
                    Button(
                        onClick        = onEquip,
                        modifier       = Modifier.fillMaxWidth().height(26.dp),
                        shape          = RoundedCornerShape(8.dp),
                        colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D9E75)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) {
                        Text("장착", fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                isOwned -> {
                    Text(
                        "보유",
                        fontSize = 9.sp,
                        color    = Color(0xFF534AB7),
                        modifier = Modifier
                            .background(Color(0xFFF4F3FE), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                isSelected -> {
                    Button(
                        onClick        = onBuy,
                        modifier       = Modifier.fillMaxWidth().height(26.dp),
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
                        Text("🪙 ${item.price}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF534AB7))
                    }
                }
            }
        }
    }
}
