package com.example.personalfinance.ui.main

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.personalfinance.data.ShopStore
import com.example.personalfinance.ui.components.CharacterLayerPreview
import com.example.personalfinance.ui.components.CharacterLayerState
import kotlinx.coroutines.launch

private enum class ShopTab(val label: String) {
    NEW("✦ 신상"),
    HAIR("헤어"),
    HAT("모자"),
    TOP("상의"),
    BOT("하의"),
    ACC("악세서리"),
}

private data class ShopItem(
    val folder: String,
    val filename: String,
    val displayName: String,
    val price: Int,
    val thumbState: CharacterLayerState,
)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(navController: NavController) {
    val context    = LocalContext.current
    val shopStore  = remember { ShopStore.getInstance(context) }
    val coins      by shopStore.coins.collectAsState()
    val ownedItems by shopStore.ownedItems.collectAsState()
    val coroutine  = rememberCoroutineScope()
    val snackbar   = remember { SnackbarHostState() }

    var selectedTab     by remember { mutableStateOf(ShopTab.NEW) }
    var bottomSheetItem by remember { mutableStateOf<ShopItem?>(null) }

    val allItems: Map<ShopTab, List<ShopItem>> = remember {
        fun build(folder: String, toState: (String) -> CharacterLayerState) =
            listAssets(context, folder).map { file ->
                ShopItem(folder, file, assetDisplayName(file), shopStore.priceOf(file), toState(file))
            }

        val hairItems = build("hairs") { CharacterLayerState(hair = it) }
        val hatItems  = build("hats")  { CharacterLayerState(hat = it) }
        val topItems  = build("clothes") { CharacterLayerState(topClothes = it) }.filter { it.filename.startsWith("top") }
        val botItems  = build("clothes") { CharacterLayerState(botClothes = it) }.filter { it.filename.startsWith("bot") }
        val accItems  = build("accessories") { CharacterLayerState(accessory = it) }
        val newItems  = (hairItems + hatItems + topItems + botItems + accItems).filter { shopStore.isNew(it.filename) }

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

    if (bottomSheetItem != null) {
        ModalBottomSheet(
            onDismissRequest = { bottomSheetItem = null },
            containerColor   = Color.White,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            val item      = bottomSheetItem!!
            val owned     = ownedItems.contains("${item.folder}/${item.filename}")
            val canAfford = coins >= item.price

            Column(
                modifier            = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF4F3FE)),
                    contentAlignment = Alignment.Center,
                ) {
                    CharacterLayerPreview(layerState = item.thumbState, size = 160.dp)
                }

                Spacer(Modifier.height(16.dp))

                if (shopStore.isNew(item.filename)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFAECE7), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text("NEW", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF993C1D))
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Text(item.displayName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint               = Color(0xFF534AB7),
                        modifier           = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${item.price} 코인", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF534AB7))
                }

                Spacer(Modifier.height(24.dp))

                if (owned) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF4F3FE))
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("이미 보유 중인 아이템이에요", fontSize = 14.sp, color = Color(0xFF534AB7))
                    }
                } else {
                    Button(
                        onClick = {
                            val success = shopStore.spendCoins(item.price)
                            if (success) {
                                shopStore.addOwned(item.folder, item.filename)
                                bottomSheetItem = null
                                coroutine.launch { snackbar.showSnackbar("${item.displayName} 구매 완료!") }
                            } else {
                                coroutine.launch { snackbar.showSnackbar("코인이 부족해요") }
                            }
                        },
                        enabled  = canAfford,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = Color(0xFF534AB7),
                            disabledContainerColor = Color(0xFFDDDDDD),
                        ),
                    ) {
                        Text(
                            text       = if (canAfford) "구매하기" else "코인 부족",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                TextButton(onClick = { bottomSheetItem = null }) {
                    Text("취소", color = Color(0xFF888888))
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .statusBarsPadding()   // ← 상태바 높이만큼 패딩
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
                    divider = { HorizontalDivider(color = Color(0xFFEEEEEE)) }
                ) {
                    ShopTab.entries.forEach { tab ->
                        Tab(
                            selected               = selectedTab == tab,
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
        LazyVerticalGrid(
            columns               = GridCells.Fixed(3),
            modifier              = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(10.dp),
            verticalArrangement   = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(currentItems, key = { it.filename }) { item ->
                val owned = ownedItems.contains("${item.folder}/${item.filename}")
                ShopItemCard(
                    item    = item,
                    owned   = owned,
                    isNew   = shopStore.isNew(item.filename),
                    onClick = { bottomSheetItem = item },
                )
            }
        }
    }
}

@Composable
private fun ShopItemCard(
    item: ShopItem,
    owned: Boolean,
    isNew: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .then(
                if (owned) Modifier.border(1.5.dp, Color(0xFFAFA9EC), RoundedCornerShape(14.dp))
                else Modifier.border(0.5.dp, Color(0xFFE8E8E8), RoundedCornerShape(14.dp))
            )
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
                val charSize = maxWidth * 1.3f
                CharacterLayerPreview(layerState = item.thumbState, size = charSize)
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

            if (owned) {
                Text(
                    "보유",
                    fontSize = 9.sp,
                    color    = Color(0xFF534AB7),
                    modifier = Modifier
                        .background(Color(0xFFF4F3FE), RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            } else {
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
