package com.example.personalfinance.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.personalfinance.data.CharacterAppearanceStore
import com.example.personalfinance.ui.components.CharacterLayerPreview
import com.example.personalfinance.ui.components.CharacterLayerState

private fun listAssetFiles(context: android.content.Context, folder: String): List<String> =
    runCatching { context.assets.list(folder)?.filter { it.endsWith(".png") }?.sorted() ?: emptyList() }
        .getOrDefault(emptyList())

private fun displayName(filename: String?): String {
    if (filename == null) return "없음"
    return filename
        .removeSuffix(".png")
        .replace(Regex("^(f|top|bot|h|hat|acc)\\d+_?"), "")
        .replace("_", " ")
        .replaceFirstChar { it.uppercase() }
        .ifBlank { filename.removeSuffix(".png") }
}

private enum class LayerCategory(
    val label: String,
    val folder: String,
    val toState: (String?) -> CharacterLayerState,
) {
    FACE      ("얼굴",    "faces",       { CharacterLayerState(face       = it) }),
    HAIR      ("헤어",    "hairs",       { CharacterLayerState(hair       = it) }),
    HAT       ("모자",    "hats",        { CharacterLayerState(hat        = it) }),
    ACCESSORY ("악세서리", "accessories", { CharacterLayerState(accessory  = it) }),
    TOP       ("상의",    "clothes",     { CharacterLayerState(topClothes = it) }),
    BOT       ("하의",    "clothes",     { CharacterLayerState(botClothes = it) }),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterLayerTestScreen(navController: NavController) {
    val context = LocalContext.current
    val appearanceStore = remember { CharacterAppearanceStore.getInstance(context) }

    val fileLists = remember {
        mapOf(
            LayerCategory.FACE      to listAssetFiles(context, "character_layers/faces"),
            LayerCategory.HAIR      to listAssetFiles(context, "character_layers/hairs"),
            LayerCategory.HAT       to listAssetFiles(context, "character_layers/hats"),
            LayerCategory.ACCESSORY to listAssetFiles(context, "character_layers/accessories"),
            LayerCategory.TOP       to listAssetFiles(context, "character_layers/clothes").filter { it.startsWith("top") },
            LayerCategory.BOT       to listAssetFiles(context, "character_layers/clothes").filter { it.startsWith("bot") },
        )
    }

    val saved by appearanceStore.appearanceFlow.collectAsState()
    val selections = remember {
        mutableStateMapOf<LayerCategory, String?>().apply {
            put(LayerCategory.FACE,      saved.face)
            put(LayerCategory.HAIR,      saved.hair)
            put(LayerCategory.HAT,       saved.hat)
            put(LayerCategory.ACCESSORY, saved.accessory)
            put(LayerCategory.TOP,       saved.topClothes)
            put(LayerCategory.BOT,       saved.botClothes)
        }
    }

    val expanded = remember {
        mutableStateMapOf<LayerCategory, Boolean>().apply {
            LayerCategory.entries.forEach { put(it, true) }
        }
    }

    val previewState = CharacterLayerState(
        face       = selections[LayerCategory.FACE],
        hair       = selections[LayerCategory.HAIR],
        hat        = selections[LayerCategory.HAT],
        accessory  = selections[LayerCategory.ACCESSORY],
        topClothes = selections[LayerCategory.TOP],
        botClothes = selections[LayerCategory.BOT],
    )

    val hasChanges = previewState != saved
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("캐릭터 레이어 테스트") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()          // ← 시스템 네비게이션 바 높이만큼 자동 패딩
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        appearanceStore.save(previewState)
                        navController.popBackStack()
                    },
                    enabled  = hasChanges,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = Color(0xFF7B61FF),
                        disabledContainerColor = Color(0xFFDDDDDD),
                    ),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = if (hasChanges) "메인 화면에 적용" else "이미 적용된 조합",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                CharacterLayerPreview(layerState = previewState, size = 160.dp)
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(Modifier.height(4.dp))

            LayerCategory.entries.forEach { category ->
                LayerCategoryPanel(
                    category   = category,
                    files      = fileLists[category] ?: emptyList(),
                    selected   = selections[category],
                    isExpanded = expanded[category] ?: true,
                    onToggle   = { expanded[category] = !(expanded[category] ?: true) },
                    onSelect   = { selections[category] = it },
                )
                HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LayerCategoryPanel(
    category: LayerCategory,
    files: List<String>,
    selected: String?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(category.label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                text       = displayName(selected),
                fontSize   = 12.sp,
                color      = if (selected == null) Color(0xFFBBBBBB) else Color(0xFF7B61FF),
                fontWeight = if (selected == null) FontWeight.Normal else FontWeight.SemiBold,
                modifier   = Modifier.weight(1f),
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector        = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (isExpanded) "접기" else "펼치기",
                tint               = Color(0xFFAAAAAA),
                modifier           = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter   = expandVertically(tween(200)),
            exit    = shrinkVertically(tween(200)),
        ) {
            Row(
                modifier              = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThumbnailCell(
                    label      = "없음",
                    isSelected = selected == null,
                    onClick    = { onSelect(null) },
                    thumbState = CharacterLayerState(),
                )
                files.forEach { file ->
                    ThumbnailCell(
                        label      = displayName(file),
                        isSelected = selected == file,
                        onClick    = { onSelect(file) },
                        thumbState = category.toState(file),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThumbnailCell(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    thumbState: CharacterLayerState,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) Color(0xFFEDE9FF) else Color(0xFFF5F5F5))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFF7B61FF) else Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            CharacterLayerPreview(layerState = thumbState, size = 56.dp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text       = label,
            fontSize   = 10.sp,
            color      = if (isSelected) Color(0xFF7B61FF) else Color(0xFF888888),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth(),
        )
    }
}
