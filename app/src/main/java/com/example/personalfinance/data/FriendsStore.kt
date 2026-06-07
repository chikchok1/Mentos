package com.example.personalfinance.data

import android.content.Context
import android.util.Log
import com.example.personalfinance.network.ApiClient
import com.example.personalfinance.network.FriendComparisonResponse
import com.example.personalfinance.network.FriendRequestCreateRequest
import com.example.personalfinance.network.FriendRequestResponse
import com.example.personalfinance.network.FriendResponse
import com.example.personalfinance.network.FriendSearchResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FriendsStore private constructor(context: Context) {
    private val appContext = context.applicationContext

    private val _searchResults = MutableStateFlow<List<FriendSearchResponse>>(emptyList())
    val searchResults: StateFlow<List<FriendSearchResponse>> = _searchResults.asStateFlow()

    private val _receivedRequests = MutableStateFlow<List<FriendRequestResponse>>(emptyList())
    val receivedRequests: StateFlow<List<FriendRequestResponse>> = _receivedRequests.asStateFlow()

    private val _sentRequests = MutableStateFlow<List<FriendRequestResponse>>(emptyList())
    val sentRequests: StateFlow<List<FriendRequestResponse>> = _sentRequests.asStateFlow()

    private val _friends = MutableStateFlow<List<FriendResponse>>(emptyList())
    val friends: StateFlow<List<FriendResponse>> = _friends.asStateFlow()

    private val _comparison = MutableStateFlow<FriendComparisonResponse?>(null)
    val comparison: StateFlow<FriendComparisonResponse?> = _comparison.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    suspend fun refreshAll() {
        _isLoading.value = true
        try {
            safeRefresh("친구 정보를 불러오지 못했습니다.") { refreshFriends() }
            safeRefresh("받은 요청을 불러오지 못했습니다.") { refreshReceived() }
            safeRefresh("보낸 요청을 불러오지 못했습니다.") { refreshSent() }
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun search(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        runApi("친구 검색 실패") {
            val api = ApiClient.getFriendApi(appContext, TokenManager(appContext))
            val response = api.search(trimmed)
            if (response.isSuccessful) {
                _searchResults.value = response.body().orEmpty()
            } else {
                fail(response.code(), "친구 검색 실패")
            }
        }
    }

    suspend fun sendRequest(receiverId: Long) {
        runApi("친구 요청 실패") {
            val api = ApiClient.getFriendRequestApi(appContext, TokenManager(appContext))
            val response = api.create(FriendRequestCreateRequest(receiverId))
            if (response.isSuccessful) {
                _message.value = "친구 요청을 보냈습니다."
                safeRefresh("보낸 요청을 새로고침하지 못했습니다.") { refreshSent() }
            } else {
                fail(response.code(), "친구 요청 실패")
            }
        }
    }

    suspend fun acceptRequest(requestId: Long) {
        runApi("친구 요청 수락 실패") {
            val api = ApiClient.getFriendRequestApi(appContext, TokenManager(appContext))
            val response = api.accept(requestId)
            if (response.isSuccessful) {
                _message.value = "친구 요청을 수락했습니다."
                safeRefresh("받은 요청을 새로고침하지 못했습니다.") { refreshReceived() }
                safeRefresh("친구 목록을 새로고침하지 못했습니다.") { refreshFriends() }
            } else {
                fail(response.code(), "친구 요청 수락 실패")
            }
        }
    }

    suspend fun rejectRequest(requestId: Long) {
        runApi("친구 요청 거절 실패") {
            val api = ApiClient.getFriendRequestApi(appContext, TokenManager(appContext))
            val response = api.reject(requestId)
            if (response.isSuccessful) {
                _message.value = "친구 요청을 거절했습니다."
                safeRefresh("받은 요청을 새로고침하지 못했습니다.") { refreshReceived() }
            } else {
                fail(response.code(), "친구 요청 거절 실패")
            }
        }
    }

    suspend fun deleteFriend(friendId: Long) {
        runApi("친구 삭제 실패") {
            val api = ApiClient.getFriendApi(appContext, TokenManager(appContext))
            val response = api.deleteFriend(friendId)
            if (response.isSuccessful) {
                _message.value = "친구를 삭제했습니다."
                safeRefresh("친구 목록을 새로고침하지 못했습니다.") { refreshFriends() }
            } else {
                fail(response.code(), "친구 삭제 실패")
            }
        }
    }

    suspend fun loadComparison(friendId: Long) {
        runApi("친구 비교 조회 실패") {
            val api = ApiClient.getFriendApi(appContext, TokenManager(appContext))
            val response = api.getComparison(friendId)
            if (response.isSuccessful) {
                _comparison.value = response.body()
            } else {
                fail(response.code(), "친구 비교 조회 실패")
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private suspend fun refreshFriends() {
        val api = ApiClient.getFriendApi(appContext, TokenManager(appContext))
        val response = api.getFriends()
        if (response.isSuccessful) {
            _friends.value = response.body().orEmpty()
        } else {
            fail(response.code(), "친구 목록 조회 실패")
        }
    }

    private suspend fun refreshReceived() {
        val api = ApiClient.getFriendRequestApi(appContext, TokenManager(appContext))
        val response = api.received()
        if (response.isSuccessful) {
            _receivedRequests.value = response.body().orEmpty()
        } else {
            fail(response.code(), "받은 요청 조회 실패")
        }
    }

    private suspend fun refreshSent() {
        val api = ApiClient.getFriendRequestApi(appContext, TokenManager(appContext))
        val response = api.sent()
        if (response.isSuccessful) {
            _sentRequests.value = response.body().orEmpty()
        } else {
            fail(response.code(), "보낸 요청 조회 실패")
        }
    }

    private suspend fun runApi(errorMessage: String, block: suspend () -> Unit) {
        _isLoading.value = true
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "$errorMessage: ${e.message}")
            _message.value = errorMessage
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun safeRefresh(errorMessage: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "$errorMessage: ${e.message}")
            _message.value = errorMessage
        }
    }

    private fun fail(code: Int, message: String): Nothing {
        throw IllegalStateException("$message (HTTP $code)")
    }

    companion object {
        private const val TAG = "FriendsStore"

        @Volatile
        private var instance: FriendsStore? = null

        fun getInstance(context: Context): FriendsStore =
            instance ?: synchronized(this) {
                instance ?: FriendsStore(context.applicationContext).also { instance = it }
            }
    }
}
