package com.example.personalfinance.notification

import com.example.personalfinance.data.ExpenseCategoryClassifier
import com.example.personalfinance.data.UserStatsCalculator

object PaymentNotificationAnalyzer {
    fun analyze(
        sourcePackage: String,
        title: String,
        text: String,
        treatSuccessfulOwnAppNotificationAsApproved: Boolean = false
    ): PaymentNotificationAnalysis {
        val parseResult = runCatching {
            CardNotificationParser.parse(title = title, text = text)
        }.getOrElse {
            CardNotificationParseResult(
                amount = null,
                merchantName = "",
                transactionDateTime = null,
                rawTitle = title,
                rawText = text,
                parseStatus = CardNotificationParseStatus.FAILED
            )
        }

        val notificationType = if (
            treatSuccessfulOwnAppNotificationAsApproved &&
            parseResult.parseStatus == CardNotificationParseStatus.SUCCESS
        ) {
            PaymentNotificationType.APPROVED
        } else {
            PaymentNotificationClassifier.classify(title = title, text = text)
        }

        val category = ExpenseCategoryClassifier.classify(
            merchantName = parseResult.merchantName,
            rawText = text
        )

        val status = when {
            notificationType == PaymentNotificationType.CANCELED -> PaymentNotificationAnalysisStatus.CANCELED
            parseResult.parseStatus == CardNotificationParseStatus.FAILED || parseResult.amount == null ->
                PaymentNotificationAnalysisStatus.PARSE_FAILED
            notificationType == PaymentNotificationType.APPROVED -> PaymentNotificationAnalysisStatus.APPROVED
            else -> PaymentNotificationAnalysisStatus.NEEDS_REVIEW
        }

        return PaymentNotificationAnalysis(
            sourcePackage = sourcePackage,
            title = title,
            text = text,
            parseResult = parseResult,
            notificationType = notificationType,
            status = status,
            category = category,
            earnedXP = parseResult.amount?.let { UserStatsCalculator.calculateEarnedXP(it) },
            isRecordable = status == PaymentNotificationAnalysisStatus.APPROVED
        )
    }
}

data class PaymentNotificationAnalysis(
    val sourcePackage: String,
    val title: String,
    val text: String,
    val parseResult: CardNotificationParseResult,
    val notificationType: PaymentNotificationType,
    val status: PaymentNotificationAnalysisStatus,
    val category: String,
    val earnedXP: Int?,
    val isRecordable: Boolean
)

enum class PaymentNotificationAnalysisStatus {
    APPROVED,
    CANCELED,
    NEEDS_REVIEW,
    PARSE_FAILED
}
