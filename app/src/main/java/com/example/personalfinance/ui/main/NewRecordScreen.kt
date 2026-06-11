package com.example.personalfinance.ui.main

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.example.personalfinance.data.ExpenseCategoryClassifier
import com.example.personalfinance.data.TransactionSource
import com.example.personalfinance.data.TransactionStatus
import com.example.personalfinance.data.UserStatsStore
import com.example.personalfinance.data.categoryEmoji
import com.example.personalfinance.ui.theme.*

private data class ExpenseCategory(
    val id: String,
    val name: String,
    val color: Color
)

@Composable
fun NewRecordScreen(navController: NavController) {
    val categories = listOf(
        ExpenseCategory(ExpenseCategoryClassifier.CATEGORY_FOOD_CAFE,         "식비/카페",   CategoryFood),
        ExpenseCategory(ExpenseCategoryClassifier.CATEGORY_LIVING_MART,       "생활/마트",   CategoryShopping),
        ExpenseCategory(ExpenseCategoryClassifier.CATEGORY_SHOPPING_ONLINE,   "쇼핑/온라인", CategoryGame),
        ExpenseCategory(ExpenseCategoryClassifier.CATEGORY_CULTURE_LEISURE,   "문화/여가",   CategoryCulture),
        ExpenseCategory(ExpenseCategoryClassifier.CATEGORY_FIXED_SUBSCRIPTION,"고정비/구독", CategoryBeauty),
        ExpenseCategory(ExpenseCategoryClassifier.CATEGORY_HEALTH_MEDICAL,    "건강/의료",   CategoryOther),
    )

    val context = LocalContext.current
    val store   = remember { UserStatsStore.getInstance(context) }

    var amount           by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var note             by remember { mutableStateOf("") }
    var isSaving         by remember { mutableStateOf(false) }

    val displayAmount = if (amount.isNotEmpty())
        "₩${String.format("%,d", amount.toLongOrNull() ?: 0)}"
    else "₩0"

    val canSave = amount.isNotEmpty() && selectedCategory != null

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Rounded.Close, null, tint = Gray600)
            }
            Text("새로운 지출", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Amount Display ────────────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.White, Gray50)))
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState  = displayAmount,
                    transitionSpec = {
                        scaleIn(initialScale = 1.08f) + fadeIn() togetherWith
                        scaleOut(targetScale = 0.95f) + fadeOut()
                    },
                    label = "amount"
                ) { target ->
                    Text(
                        text       = target,
                        style      = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 42.sp
                    )
                }
            }

            // ── Category Grid ─────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    "카테고리",
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = Gray600,
                    modifier   = Modifier.padding(bottom = 14.dp)
                )
                categories.chunked(3).forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { cat ->
                            val isSelected = selectedCategory == cat.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .then(
                                        if (isSelected) Modifier
                                            .shadow(4.dp, RoundedCornerShape(16.dp))
                                            .background(
                                                Brush.linearGradient(listOf(Blue50, Purple50)),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .border(1.5.dp, Blue400.copy(0.4f), RoundedCornerShape(16.dp))
                                        else Modifier.background(Gray50)
                                    )
                                    .clickable { selectedCategory = cat.id }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier         = Modifier.size(54.dp).background(cat.color.copy(0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) { Text(categoryEmoji(cat.name), fontSize = 22.sp) }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        cat.name,
                                        style      = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color      = if (isSelected) Blue500 else Gray700
                                    )
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── Note Input ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = note,
                onValueChange = { note = it },
                placeholder   = { Text("어디서 사용했나요?", color = Gray400) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                shape         = RoundedCornerShape(16.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Blue400,
                    unfocusedBorderColor    = Gray200,
                    unfocusedContainerColor = Gray50,
                    focusedContainerColor   = Color.White
                ),
                singleLine    = true
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Number Pad ────────────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("1","2","3","4","5","6","7","8","9","C","0","⌫").chunked(3).forEach { row ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { key ->
                        val isSpecial = key == "C" || key == "⌫"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSpecial) Gray100 else Gray50)
                                .clickable {
                                    when (key) {
                                        "C"  -> amount = ""
                                        "⌫"  -> if (amount.isNotEmpty()) amount = amount.dropLast(1)
                                        else -> if (amount.length < 10) amount += key
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = key,
                                style      = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color      = if (isSpecial) Gray600 else Gray700
                            )
                        }
                    }
                }
            }

            // ── Save Button ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (canSave) Brush.horizontalGradient(listOf(Blue500, Purple500))
                        else         Brush.horizontalGradient(listOf(Gray100, Gray100))
                    )
                    .clickable(enabled = canSave && !isSaving) {
                        val parsedAmount = amount.replace(",", "").toLongOrNull() ?: return@clickable
                        isSaving = true
                        store.addExpense(
                            amount       = parsedAmount,
                            category     = selectedCategory!!,
                            merchantName = note.ifBlank { selectedCategory!! },
                            status       = TransactionStatus.APPROVED_RECORDED,
                            source       = TransactionSource.MANUAL   // ← 수동 입력 명시
                        )
                        navController.popBackStack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = if (isSaving) "저장 중..." else "저장하기",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (canSave) Color.White else Gray400
                )
            }
        }
    }
}
