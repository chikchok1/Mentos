package com.example.personalfinance.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.personalfinance.ui.components.CharacterLayerPreview

// ── 얼굴 레이어 목록 (assets/character_layers/faces/) ────────────────────────

private val allFaces = listOf(
    "f_closed_smile.png",
    "f_smile.png",
    "f_wink.png",
    "f_smirk.png",
    "f_angry.png",
    "f_cry.png",
    "f_dead.png",
    "f_slacker.png",
    "f_sparkle.png",
    "f_surprised.png",
)

// ── CharacterLayerTestScreen ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterLayerTestScreen(navController: NavController) {
    var selectedFace by remember { mutableStateOf("f_closed_smile.png") }

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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── 합성 결과 미리보기 ────────────────────────────────────────────
            Text(
                text = "합성 결과 미리보기",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(260.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                CharacterLayerPreview(
                    facePath = "faces/$selectedFace",
                    size = 220.dp,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "base_body.png + $selectedFace",
                fontSize = 11.sp,
                color = Color.Gray,
            )

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // ── 얼굴 레이어 선택 ─────────────────────────────────────────────
            Text(
                text = "얼굴 레이어 선택",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 3열 그리드로 얼굴 선택
            val chunkSize = 3
            allFaces.chunked(chunkSize).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { face ->
                        val isSelected = face == selectedFace
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    if (isSelected) Color(0xFFFFE0F0) else Color(0xFFF5F5F5),
                                    RoundedCornerShape(12.dp),
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFFF69B4) else Color(0xFFDDDDDD),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable { selectedFace = face },
                            contentAlignment = Alignment.Center,
                        ) {
                            // 미니 미리보기
                            CharacterLayerPreview(
                                facePath = "faces/$face",
                                size = 72.dp,
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }
                    // 남은 칸 채우기
                    repeat(chunkSize - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}


