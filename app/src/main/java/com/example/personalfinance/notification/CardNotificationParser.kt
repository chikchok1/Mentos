package com.example.personalfinance.notification

import java.time.LocalDate
import java.time.LocalDateTime

object CardNotificationParser {
    private val amountPattern = Regex("""([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\s*원""")
    private val dateTimePattern = Regex("""(\d{2})/(\d{2})\s+(\d{1,2}):(\d{2})""")
    private val bracketTagPattern = Regex("""^\[[^\]]+]\s*""")
    private val merchantCleanupPattern = Regex("""^(체크승인|일시불\s*승인|승인|결제)\s*|\s*(체크승인|일시불\s*승인|승인|결제)$""")

    fun parse(
        title: String?,
        text: String?,
        referenceDate: LocalDate = LocalDate.now()
    ): CardNotificationParseResult {
        val rawTitle = title.orEmpty()
        val rawText = text.orEmpty()
        val amountMatch = amountPattern.find(rawText)

        if (amountMatch == null) {
            return CardNotificationParseResult(
                amount = null,
                merchantName = "",
                transactionDateTime = null,
                rawTitle = rawTitle,
                rawText = rawText,
                parseStatus = CardNotificationParseStatus.FAILED
            )
        }

        val amount = amountMatch.groupValues[1].replace(",", "").toLong()
        val transactionDateTime = parseTransactionDateTime(rawText, referenceDate)
        val merchantName = extractMerchantName(rawText, amountMatch.range)

        return CardNotificationParseResult(
            amount = amount,
            merchantName = merchantName,
            transactionDateTime = transactionDateTime,
            rawTitle = rawTitle,
            rawText = rawText,
            parseStatus = if (merchantName.isNotBlank()) {
                CardNotificationParseStatus.SUCCESS
            } else {
                CardNotificationParseStatus.FAILED
            }
        )
    }

    private fun parseTransactionDateTime(
        text: String,
        referenceDate: LocalDate
    ): LocalDateTime? {
        val match = dateTimePattern.find(text) ?: return null
        val month = match.groupValues[1].toInt()
        val day = match.groupValues[2].toInt()
        val hour = match.groupValues[3].toInt()
        val minute = match.groupValues[4].toInt()

        return LocalDateTime.of(referenceDate.year, month, day, hour, minute)
    }

    private fun extractMerchantName(text: String, amountRange: IntRange): String {
        val afterAmount = text.substring(amountRange.last + 1).cleanMerchantCandidate()
        if (afterAmount.isNotBlank()) return afterAmount

        val beforeAmount = text.substring(0, amountRange.first)
            .replace(dateTimePattern, "")
            .cleanMerchantCandidate()

        return beforeAmount
    }

    private fun String.cleanMerchantCandidate(): String =
        trim()
            .replace(bracketTagPattern, "")
            .trim()
            .replace(merchantCleanupPattern, "")
            .trim()
}

data class CardNotificationParseResult(
    val amount: Long?,
    val merchantName: String,
    val transactionDateTime: LocalDateTime?,
    val rawTitle: String,
    val rawText: String,
    val parseStatus: CardNotificationParseStatus
)

enum class CardNotificationParseStatus {
    SUCCESS,
    FAILED
}
