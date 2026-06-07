package com.example.personalfinance.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.personalfinance.data.FriendsStore
import com.example.personalfinance.data.UserStatsCalculator
import com.example.personalfinance.navigation.Screen
import com.example.personalfinance.network.ComparisonUserResponse
import com.example.personalfinance.network.FriendRequestResponse
import com.example.personalfinance.network.FriendResponse
import com.example.personalfinance.network.FriendSearchResponse
import com.example.personalfinance.ui.theme.Blue50
import com.example.personalfinance.ui.theme.Blue500
import com.example.personalfinance.ui.theme.Gray100
import com.example.personalfinance.ui.theme.Gray200
import com.example.personalfinance.ui.theme.Gray400
import com.example.personalfinance.ui.theme.Gray500
import com.example.personalfinance.ui.theme.Gray600
import com.example.personalfinance.ui.theme.Gray700
import com.example.personalfinance.ui.theme.Gray900
import com.example.personalfinance.ui.theme.GreenSuccess
import com.example.personalfinance.ui.theme.Purple50
import com.example.personalfinance.ui.theme.Purple500
import com.example.personalfinance.ui.theme.RedDanger
import kotlinx.coroutines.launch

@Composable
fun FriendsScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { FriendsStore.getInstance(context) }
    val scope = rememberCoroutineScope()

    val searchResults by store.searchResults.collectAsState()
    val receivedRequests by store.receivedRequests.collectAsState()
    val sentRequests by store.sentRequests.collectAsState()
    val friends by store.friends.collectAsState()
    val isLoading by store.isLoading.collectAsState()
    val message by store.message.collectAsState()

    var keyword by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        store.refreshAll()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        Header(
            title = "친구",
            onBack = { navController.popBackStack() }
        )

        Column(modifier = Modifier.padding(24.dp)) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Gray500) },
                placeholder = { Text("이메일 또는 사용자 ID 검색") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Blue500)
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { scope.launch { store.search(keyword) } },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = keyword.isNotBlank()
            ) {
                Icon(Icons.Rounded.Search, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("검색")
            }

            message?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Gray600, style = MaterialTheme.typography.bodySmall)
                LaunchedEffect(it) { store.clearMessage() }
            }

            if (isLoading) {
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Blue500)
                }
            }

            SectionTitle("검색 결과")
            if (searchResults.isEmpty()) {
                EmptyText("검색 결과가 없습니다.")
            } else {
                searchResults.forEach { result ->
                    SearchResultCard(
                        result = result,
                        onSend = { scope.launch { store.sendRequest(result.id) } },
                        onAccept = {
                            result.pendingRequestId?.let { requestId ->
                                scope.launch { store.acceptRequest(requestId) }
                            }
                        }
                    )
                }
            }

            SectionTitle("받은 요청")
            if (receivedRequests.isEmpty()) {
                EmptyText("받은 친구 요청이 없습니다.")
            } else {
                receivedRequests.forEach { request ->
                    RequestCard(
                        request = request,
                        received = true,
                        onAccept = { scope.launch { store.acceptRequest(request.id) } },
                        onReject = { scope.launch { store.rejectRequest(request.id) } }
                    )
                }
            }

            SectionTitle("보낸 요청")
            if (sentRequests.isEmpty()) {
                EmptyText("보낸 친구 요청이 없습니다.")
            } else {
                sentRequests.take(5).forEach { request ->
                    RequestCard(
                        request = request,
                        received = false,
                        onAccept = {},
                        onReject = {}
                    )
                }
            }

            SectionTitle("친구 목록")
            if (friends.isEmpty()) {
                EmptyText("등록된 친구가 없습니다.")
            } else {
                friends.forEach { friend ->
                    FriendCard(
                        friend = friend,
                        onCompare = { navController.navigate(Screen.FriendComparison.route(friend.friendId)) },
                        onDelete = { scope.launch { store.deleteFriend(friend.friendId) } }
                    )
                }
            }
        }
    }
}

@Composable
fun FriendComparisonScreen(navController: NavController, friendId: Long) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { FriendsStore.getInstance(context) }
    val comparison by store.comparison.collectAsState()

    LaunchedEffect(friendId) {
        store.loadComparison(friendId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        Header(
            title = "친구 비교",
            onBack = { navController.popBackStack() }
        )

        val data = comparison
        if (data == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Blue500)
            }
        } else {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "${data.month} 기준",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ComparisonUserCard("나", data.me, Modifier.weight(1f))
                    ComparisonUserCard("친구", data.friend, Modifier.weight(1f))
                }
                SectionTitle("월별 소비 비교")
                SpendingComparison(data.me, data.friend)
                SectionTitle("카테고리별 소비")
                CategoryComparison(data.me, data.friend)
            }
        }
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, null, tint = Gray600)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(48.dp))
    }
    HorizontalDivider(color = Gray100)
}

@Composable
private fun SectionTitle(title: String) {
    Spacer(Modifier.height(24.dp))
    Text(
        title,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = Gray500,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun EmptyText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Gray100.copy(alpha = 0.6f))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Gray400, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SearchResultCard(
    result: FriendSearchResponse,
    onSend: () -> Unit,
    onAccept: () -> Unit
) {
    SimpleCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar()
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName(result.nickname, result.email, result.id), fontWeight = FontWeight.SemiBold)
                Text("Lv.${result.level} · ${UserStatsCalculator.jobTitle(result.job)}", color = Gray500, fontSize = 13.sp)
            }
            when (result.requestStatus) {
                "NONE" -> SmallActionButton("요청", Icons.Rounded.PersonAdd, onSend)
                "PENDING_RECEIVED" -> SmallActionButton("수락", Icons.Rounded.Check, onAccept)
                "PENDING_SENT" -> StatusChip("요청됨")
                "FRIEND" -> StatusChip("친구")
                else -> StatusChip(result.requestStatus)
            }
        }
    }
}

@Composable
private fun RequestCard(
    request: FriendRequestResponse,
    received: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val id = if (received) request.requesterId else request.receiverId
    val email = if (received) request.requesterEmail else request.receiverEmail
    val nickname = if (received) request.requesterNickname else request.receiverNickname
    SimpleCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar()
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName(nickname, email, id), fontWeight = FontWeight.SemiBold)
                Text(request.status, color = Gray500, fontSize = 13.sp)
            }
            if (received) {
                IconButton(onClick = onAccept) {
                    Icon(Icons.Rounded.Check, null, tint = GreenSuccess)
                }
                IconButton(onClick = onReject) {
                    Icon(Icons.Rounded.Close, null, tint = RedDanger)
                }
            }
        }
    }
}

@Composable
private fun FriendCard(
    friend: FriendResponse,
    onCompare: () -> Unit,
    onDelete: () -> Unit
) {
    SimpleCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar()
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName(friend.nickname, friend.email, friend.friendId), fontWeight = FontWeight.SemiBold)
                if (friend.characterVisible) {
                    Text(
                        "Lv.${friend.level ?: 1} · ${UserStatsCalculator.jobTitle(friend.job ?: "beginner")}",
                        color = Gray500,
                        fontSize = 13.sp
                    )
                    Text(
                        "XP ${friend.totalXp ?: 0}",
                        color = Gray400,
                        fontSize = 12.sp
                    )
                } else {
                    Text("캐릭터 비공개", color = Gray500, fontSize = 13.sp)
                }
                Text(
                    if (friend.monthlySpendingVisible) "이번 달 ${formatWon(friend.monthlySpending ?: 0L)}" else "지출 비공개",
                    color = if (friend.monthlySpendingVisible) Blue500 else Gray500,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onCompare,
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(Icons.Rounded.SyncAlt, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("비교")
            }
            TextButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, null, tint = RedDanger, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("삭제", color = RedDanger)
            }
        }
    }
}

@Composable
private fun ComparisonUserCard(label: String, user: ComparisonUserResponse, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Blue50, Purple50)))
            .padding(16.dp)
    ) {
        Column {
            Text(label, color = Gray600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                displayName(user.nickname, user.email, user.id),
                color = Gray900,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            if (user.characterVisible) {
                Text("Lv.${user.level ?: 1}", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("${user.totalXp ?: 0} XP", color = Gray600, fontSize = 13.sp)
                Text(UserStatsCalculator.jobTitle(user.job ?: "beginner"), color = Blue500, fontSize = 13.sp)
            } else {
                Text("캐릭터 비공개", color = Gray500, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SpendingComparison(me: ComparisonUserResponse, friend: ComparisonUserResponse) {
    SimpleCard {
        SpendingRow("나", me.monthlySpendingVisible, me.monthlySpending)
        HorizontalDivider(color = Gray100, modifier = Modifier.padding(vertical = 10.dp))
        SpendingRow("친구", friend.monthlySpendingVisible, friend.monthlySpending)
        if (!friend.monthlySpendingVisible) {
            Spacer(Modifier.height(8.dp))
            Text("친구가 지출 정보를 비공개로 설정했습니다.", color = Gray500, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SpendingRow(label: String, visible: Boolean, amount: Long?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!visible) Icon(Icons.Rounded.Lock, null, tint = Gray400, modifier = Modifier.size(16.dp))
            if (!visible) Spacer(Modifier.width(6.dp))
            Text(label, color = Gray700)
        }
        Text(
            if (visible) formatWon(amount ?: 0L) else "비공개",
            color = if (visible) Gray900 else Gray500,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CategoryComparison(me: ComparisonUserResponse, friend: ComparisonUserResponse) {
    SimpleCard {
        Text("나", fontWeight = FontWeight.SemiBold, color = Gray700)
        CategoryList(me)
        Spacer(Modifier.height(14.dp))
        Text("친구", fontWeight = FontWeight.SemiBold, color = Gray700)
        if (!friend.monthlySpendingVisible) {
            Text("지출 비공개", color = Gray500, modifier = Modifier.padding(top = 8.dp))
        } else {
            CategoryList(friend)
        }
    }
}

@Composable
private fun CategoryList(user: ComparisonUserResponse) {
    if (user.categorySpending.isEmpty()) {
        Text("이번 달 소비 내역이 없습니다.", color = Gray400, modifier = Modifier.padding(top = 8.dp))
        return
    }
    user.categorySpending.forEach { category ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(category.category, color = Gray600, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("${formatWon(category.amount)} · ${category.ratio}%", color = Gray900, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SimpleCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Gray200, RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun Avatar() {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(Brush.linearGradient(listOf(Blue50, Purple50)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.People, null, tint = Blue500)
    }
}

@Composable
private fun SmallActionButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue500)
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 13.sp)
    }
}

@Composable
private fun StatusChip(text: String) {
    AssistChip(
        onClick = {},
        label = { Text(text, fontSize = 12.sp) }
    )
}

private fun displayName(nickname: String?, email: String?, id: Long): String =
    nickname?.takeIf { it.isNotBlank() }
        ?: email?.takeIf { it.isNotBlank() }
        ?: "사용자 #$id"

private fun formatWon(amount: Long): String = "${String.format("%,d", amount)}원"
