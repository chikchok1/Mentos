package com.example.personalfinance.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.personalfinance.data.ExpenseCategoryClassifier
import com.example.personalfinance.data.TransactionSource
import com.example.personalfinance.data.TransactionStatus
import com.example.personalfinance.data.UserStatsStore
import com.example.personalfinance.data.TokenManager
import com.example.personalfinance.network.ApiClient
import com.example.personalfinance.network.SaveNotificationParseLogRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class CardNotificationListenerService : NotificationListenerService() {

    // [FIX #6] 서비스 생명쿈에 한 번만 생성하고 onDestroy에서 cancel
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected for test payment notifications.")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()   // [FIX #6] 서비스 종료 시 코루틴 정리
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val diagnosticId = sbn.toDiagnosticId()
        val notification = sbn.notification
        val title = notification.extractTitle()

        if (!CardNotificationSourcePolicy.isSupported(sbn.packageName, packageName)) {
            CardNotificationDebugStore.recordDiagnostic(
                diagnosticId = diagnosticId,
                packageName = sbn.packageName,
                title = title,
                status = CardNotificationDiagnosticStatus.IGNORED_PACKAGE,
                reason = "허용되지 않은 패키지",
                handled = true
            )
            enqueueNotificationParseLog(
                diagnosticId = diagnosticId,
                packageName = sbn.packageName,
                title = title,
                rawText = null,
                status = NotificationParseLogStatus.UNSUPPORTED_FORMAT,
                failureReason = "지원하지 않는 패키지"
            )
            Log.d(TAG, "Ignoring unsupported notification package=${sbn.packageName}")
            return
        }

        if (notification == null) {
            CardNotificationDebugStore.recordDiagnostic(
                diagnosticId = diagnosticId,
                packageName = sbn.packageName,
                title = title,
                status = CardNotificationDiagnosticStatus.PARSE_FAILED,
                reason = "알림 내용 없음",
                handled = true,
                allowRawTextPreview = true
            )
            enqueueNotificationParseLog(
                diagnosticId = diagnosticId,
                packageName = sbn.packageName,
                title = title,
                rawText = null,
                status = NotificationParseLogStatus.PARSE_FAILED,
                failureReason = "알림 내용 없음"
            )
            return
        }

        val content = notification.extractContent()
        CardNotificationDebugStore.recordDiagnostic(
            diagnosticId = diagnosticId,
            packageName = sbn.packageName,
            title = content.title,
            status = CardNotificationDiagnosticStatus.RECEIVED,
            reason = "알림 수신",
            handled = false,
            rawText = content.text,
            allowRawTextPreview = true
        )
        enqueueNotificationParseLog(
            diagnosticId = diagnosticId,
            packageName = sbn.packageName,
            title = content.title,
            rawText = content.text,
            status = NotificationParseLogStatus.RECEIVED,
            failureReason = "알림 수신"
        )

        if (!PaymentNotificationCandidateFilter.isCandidate(content.title, content.text)) {
            CardNotificationDebugStore.recordDiagnostic(
                diagnosticId = diagnosticId,
                packageName = sbn.packageName,
                title = content.title,
                status = CardNotificationDiagnosticStatus.IGNORED_NON_PAYMENT,
                reason = if (content.text.isBlank()) {
                    "rawText 추출 부족"
                } else {
                    "결제 알림 아님"
                },
                handled = true,
                rawText = content.text,
                allowRawTextPreview = true
            )
            enqueueNotificationParseLog(
                diagnosticId = diagnosticId,
                packageName = sbn.packageName,
                title = content.title,
                rawText = content.text,
                status = NotificationParseLogStatus.IGNORED_NOT_PAYMENT,
                failureReason = if (content.text.isBlank()) {
                    "rawText 추출 부족"
                } else {
                    "결제 알림 아님"
                }
            )
            Log.i(TAG, "Ignoring non-payment notification package=${sbn.packageName}")
            return
        }

        val result = runCatching {
            CardNotificationParser.parse(title = content.title, text = content.text)
        }.getOrElse { error ->
            Log.w(TAG, "Failed to parse supported payment notification.", error)
            CardNotificationParseResult(
                amount = null,
                merchantName = "",
                transactionDateTime = null,
                rawTitle = content.title,
                rawText = content.text,
                parseStatus = CardNotificationParseStatus.FAILED
            )
        }
        val notificationType = if (
            sbn.packageName == packageName &&
            result.parseStatus == CardNotificationParseStatus.SUCCESS
        ) {
            PaymentNotificationType.APPROVED
        } else {
            PaymentNotificationClassifier.classify(
                title = content.title,
                text = content.text
            )
        }
        val serviceScope = this.serviceScope
        serviceScope.launch {
            // 로컬 기본 분류 후 백엔드 AI 결과로 보정
            var category = ExpenseCategoryClassifier.classifyMerchant(result.merchantName)

            if (result.merchantName.isNotBlank()) {
                try {
                    val tokenManager = TokenManager(this@CardNotificationListenerService)
                    val classificationApi = ApiClient.getClassificationApi(this@CardNotificationListenerService, tokenManager)
                    val response = classificationApi.categorizeMerchant(result.merchantName)
                    
                    if (response.isSuccessful) {
                        val aiCategory = response.body()?.get("category")
                        if (aiCategory != null && aiCategory in ExpenseCategoryClassifier.categories) {
                            category = aiCategory
                            Log.i(TAG, "AI successfully classified ${result.merchantName} as $category")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "AI Classification API error", e)
                }
            }

            var diagnosticStatus = CardNotificationDiagnosticStatus.NEEDS_REVIEW
            var diagnosticReason = "승인/취소 판단 불가"

            val handlingStatus = if (PaymentNotificationRecordPolicy.shouldRecord(result, notificationType)) {
                val approvedAmount = requireNotNull(result.amount)
                val processedKey = ProcessedNotificationKey(
                    packageName = sbn.packageName,
                    postTime = sbn.postTime,
                    amount = approvedAmount,
                    merchantName = result.merchantName
                )

                if (markProcessed(processedKey)) {
                    val store = UserStatsStore.getInstance(this@CardNotificationListenerService)
                    val recorded = store.addExpense(
                        amount = approvedAmount,
                        category = category,
                        merchantName = result.merchantName,
                        transactionDateTime = result.transactionDateTime,
                        status = TransactionStatus.APPROVED_RECORDED,
                        source = if (sbn.packageName == packageName) {
                            TransactionSource.SAMPLE
                        } else {
                            TransactionSource.NOTIFICATION
                        },
                        transactionId = processedKey.toTransactionId()
                    )
                    if (recorded) {
                        Log.i(TAG, "Added approved payment notification to UserStatsStore")
                        diagnosticStatus = CardNotificationDiagnosticStatus.APPROVED_RECORDED
                        diagnosticReason = "정상 반영"
                        CardNotificationHandlingStatus.APPROVED_RECORDED
                    } else {
                        Log.i(TAG, "Ignoring duplicate stored payment transaction")
                        diagnosticStatus = CardNotificationDiagnosticStatus.DUPLICATE_IGNORED
                        diagnosticReason = "중복 알림"
                        CardNotificationHandlingStatus.DUPLICATE_IGNORED
                    }
                } else {
                    Log.i(TAG, "Ignoring duplicate parsed payment notification")
                    diagnosticStatus = CardNotificationDiagnosticStatus.DUPLICATE_IGNORED
                    diagnosticReason = "중복 알림"
                    CardNotificationHandlingStatus.DUPLICATE_IGNORED
                }
            } else {
                when {
                    notificationType == PaymentNotificationType.CANCELED -> {
                        Log.i(TAG, "Ignoring canceled payment notification package=${sbn.packageName}")
                        diagnosticStatus = CardNotificationDiagnosticStatus.CANCELED
                        diagnosticReason = "취소 알림"
                        CardNotificationHandlingStatus.CANCELED_IGNORED
                    }
                    result.parseStatus == CardNotificationParseStatus.FAILED || result.amount == null -> {
                        Log.i(TAG, "Payment notification parsing failed package=${sbn.packageName}")
                        diagnosticStatus = CardNotificationDiagnosticStatus.PARSE_FAILED
                        diagnosticReason = when {
                            content.text.isBlank() -> "rawText 추출 부족"
                            result.amount == null -> "금액 파싱 실패"
                            result.merchantName.isBlank() -> "점포명 파싱 실패"
                            else -> "파싱 실패"
                        }
                        CardNotificationHandlingStatus.PARSE_FAILED
                    }
                    else -> {
                        Log.i(TAG, "Payment notification needs review package=${sbn.packageName}")
                        diagnosticStatus = CardNotificationDiagnosticStatus.NEEDS_REVIEW
                        diagnosticReason = "승인/취소 판단 불가"
                        CardNotificationHandlingStatus.NEEDS_REVIEW
                    }
                }
            }

            CardNotificationDebugStore.recordDiagnostic(
                diagnosticId = diagnosticId,
                packageName = sbn.packageName,
                title = content.title,
                status = diagnosticStatus,
                reason = diagnosticReason,
                handled = true,
                rawText = content.text,
                allowRawTextPreview = true
            )

            enqueueNotificationParseLog(
                diagnosticId = diagnosticId,
                packageName = sbn.packageName,
                title = content.title,
                rawText = content.text,
                status = when (handlingStatus) {
                    CardNotificationHandlingStatus.APPROVED_RECORDED -> NotificationParseLogStatus.PARSE_SUCCESS
                    CardNotificationHandlingStatus.DUPLICATE_IGNORED -> NotificationParseLogStatus.DUPLICATED
                    CardNotificationHandlingStatus.PARSE_FAILED -> NotificationParseLogStatus.PARSE_FAILED
                    else -> NotificationParseLogStatus.UNSUPPORTED_FORMAT
                },
                failureReason = diagnosticReason,
                amount = result.amount,
                merchantName = result.merchantName.takeIf { it.isNotBlank() },
                occurredAt = result.transactionDateTime?.toString(),
                clientTransactionId = if (handlingStatus == CardNotificationHandlingStatus.APPROVED_RECORDED ||
                    handlingStatus == CardNotificationHandlingStatus.DUPLICATE_IGNORED
                ) {
                    ProcessedNotificationKey(
                        packageName = sbn.packageName,
                        postTime = sbn.postTime,
                        amount = result.amount ?: 0L,
                        merchantName = result.merchantName
                    ).toTransactionId()
                } else {
                    null
                }
            )

            CardNotificationDebugStore.update(
                sourcePackage = sbn.packageName,
                title = content.title,
                text = content.text,
                result = result,
                category = category,
                notificationType = notificationType,
                handlingStatus = handlingStatus
            )

            Log.i(
                TAG,
                "Handled payment notification: handlingStatus=$handlingStatus, " +
                    "notificationType=$notificationType, parseStatus=${result.parseStatus}, " +
                    "diagnosticStatus=$diagnosticStatus"
            )
        }
    }

    private fun StatusBarNotification.toDiagnosticId(): String =
        "${packageName}|$postTime|${id}|${tag.orEmpty()}"

    private fun Notification?.extractTitle(): String =
        this?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()

    private fun Notification.extractContent(): ExtractedNotificationContent {
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val parts = LinkedHashSet<String>()

        fun addPart(value: CharSequence?) {
            val text = value?.toString()?.trim().orEmpty()
            if (text.isNotBlank()) {
                parts.add(text)
            }
        }

        addPart(extras.getCharSequence(Notification.EXTRA_TITLE))
        addPart(extras.getCharSequence(Notification.EXTRA_TITLE_BIG))
        addPart(extras.getCharSequence(Notification.EXTRA_TEXT))
        addPart(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
        addPart(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
        addPart(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
        addPart(extras.getCharSequence(Notification.EXTRA_INFO_TEXT))
        addPart(tickerText)
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.forEach { line -> addPart(line) }

        return ExtractedNotificationContent(
            title = title,
            text = parts.joinToString(separator = " ")
        )
    }

    private data class ExtractedNotificationContent(
        val title: String,
        val text: String
    )

    private data class ProcessedNotificationKey(
        val packageName: String,
        val postTime: Long,
        val amount: Long,
        val merchantName: String
    ) {
        fun toTransactionId(): String = "$packageName|$postTime|$amount|$merchantName"
    }

    private fun enqueueNotificationParseLog(
        diagnosticId: String,
        packageName: String,
        title: String?,
        rawText: String?,
        status: String,
        failureReason: String?,
        amount: Long? = null,
        merchantName: String? = null,
        occurredAt: String? = null,
        clientTransactionId: String? = null
    ) {
        serviceScope.launch {
            try {
                val tokenManager = TokenManager(this@CardNotificationListenerService)
                val api = ApiClient.getNotificationParseLogApi(
                    this@CardNotificationListenerService,
                    tokenManager
                )
                val response = api.save(
                    SaveNotificationParseLogRequest(
                        diagnosticId = diagnosticId,
                        packageName = packageName,
                        title = title,
                        rawText = rawText,
                        status = status,
                        failureReason = failureReason,
                        parsedAmount = amount,
                        parsedMerchant = merchantName,
                        parsedOccurredAt = occurredAt,
                        clientTransactionId = clientTransactionId,
                        receivedAt = LocalDateTime.now().toString()
                    )
                )
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    Log.e(
                        TAG,
                        "Notification parse log sync failed HTTP ${response.code()}: " +
                            "status=$status, error=$errorBody"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Notification parse log sync failed: ${e.message}", e)
            }
        }
    }

    private object NotificationParseLogStatus {
        const val RECEIVED = "RECEIVED"
        const val PARSE_SUCCESS = "PARSE_SUCCESS"
        const val PARSE_FAILED = "PARSE_FAILED"
        const val DUPLICATED = "DUPLICATED"
        const val IGNORED_NOT_PAYMENT = "IGNORED_NOT_PAYMENT"
        const val UNSUPPORTED_FORMAT = "UNSUPPORTED_FORMAT"
    }

    private companion object {
        const val TAG = "CardNotificationListener"
        const val MAX_PROCESSED_NOTIFICATION_KEYS = 100

        val processedNotificationKeys = LinkedHashSet<ProcessedNotificationKey>()

        fun markProcessed(key: ProcessedNotificationKey): Boolean =
            synchronized(processedNotificationKeys) {
                if (!processedNotificationKeys.add(key)) {
                    return@synchronized false
                }

                if (processedNotificationKeys.size > MAX_PROCESSED_NOTIFICATION_KEYS) {
                    val iterator = processedNotificationKeys.iterator()
                    if (iterator.hasNext()) {
                        iterator.next()
                        iterator.remove()
                    }
                }

                true
            }
    }
}

internal object PaymentNotificationCandidateFilter {
    private val amountPattern = Regex("""([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\s*원""")
    private val paymentKeywords = listOf(
        "결제",
        "승인",
        "사용",
        "체크카드",
        "신용카드",
        "출금",
        "자동이체",
        "정기결제"
    )
    private val cancelKeywords = listOf(
        "취소",
        "승인취소",
        "결제취소",
        "환불",
        "매출취소"
    )

    fun isCandidate(title: String, rawText: String): Boolean {
        val combined = "$title $rawText"
        val normalized = normalize(combined)

        return amountPattern.containsMatchIn(combined) &&
            (paymentKeywords + cancelKeywords).any { keyword ->
                normalized.contains(normalize(keyword))
            }
    }

    private fun normalize(value: String): String =
        value.filterNot { it.isWhitespace() }
}

internal object PaymentNotificationRecordPolicy {
    fun shouldRecord(
        result: CardNotificationParseResult,
        notificationType: PaymentNotificationType
    ): Boolean =
        result.parseStatus == CardNotificationParseStatus.SUCCESS &&
            result.amount != null &&
            notificationType == PaymentNotificationType.APPROVED
}
