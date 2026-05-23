package com.example.personalfinance.notification

import java.time.LocalDate
import java.time.LocalDateTime

object CardNotificationParser {
    private const val AMOUNT_TEXT_PATTERN = """[0-9]{1,3}(?:,[0-9]{3})+|[0-9]+"""

    private val amountPattern = Regex("""($AMOUNT_TEXT_PATTERN)\s*원""")
    private val dateTimePattern = Regex("""(\d{2})/(\d{2})\s+(\d{1,2}):(\d{2})""")
    private val bracketTagPattern = Regex("""^\[[^\]]+]\s*""")
    private val balanceInfoPattern = Regex(
        """(?:(?:남은|잔여)\s*(?:지원금|금액|포인트)?|지원금\s*잔액|포인트\s*잔액|잔액|잔여금|보유금액)\s*[:：]?\s*(?:$AMOUNT_TEXT_PATTERN)\s*원?"""
    )
    private val accountCardWordPattern = Regex("""(토스뱅크|체크카드|신용카드|카드|계좌|잔액)""")
    private val paymentAmountKeywords = listOf("결제", "승인", "사용", "출금")
    private val merchantCleanupPattern = Regex(
        """^(샘플\s*결제|체크승인|일시불\s*승인|승인취소|결제취소|매출취소|체크카드|신용카드|정기결제|자동납부|승인|결제|사용|취소|환불)\s*|\s*(체크승인|일시불\s*승인|승인취소|결제취소|매출취소|체크카드|신용카드|정기결제|자동납부|승인|결제|사용|취소|환불)$"""
    )

    fun parse(
        title: String?,
        text: String?,
        referenceDate: LocalDate = LocalDate.now()
    ): CardNotificationParseResult {
        val rawTitle = title.orEmpty()
        val rawText = text.orEmpty()
        val amountMatch = selectPaymentAmountMatch(rawText)

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

        val transactionYear = if (referenceDate.monthValue == 1 && month == 12) {
            referenceDate.year - 1
        } else {
            referenceDate.year
        }

        return runCatching {
            LocalDateTime.of(transactionYear, month, day, hour, minute)
        }.getOrNull()
    }

    private fun extractMerchantName(text: String, amountRange: IntRange): String {
        val afterAmount = text.substring(amountRange.last + 1).cleanMerchantCandidate()
        if (afterAmount.isNotBlank()) return afterAmount

        val beforeAmount = text.substring(0, amountRange.first)
            .replace(dateTimePattern, "")
            .cleanMerchantCandidate()

        return beforeAmount
    }

    private fun selectPaymentAmountMatch(text: String): MatchResult? {
        val matches = amountPattern.findAll(text).toList()
        if (matches.isEmpty()) return null

        val paymentCandidates = matches.filterNot { match ->
            isBalanceAmountCandidate(text, match)
        }

        // 결제 키워드 근처 매칭 우선
        val nearKeyword = paymentCandidates.firstOrNull { match ->
            hasPaymentKeywordNear(text, match.range)
        }
        if (nearKeyword != null) return nearKeyword

        // 결제 키워드 근처가 없으면 — 잔액 키워드 없는 후보 중 마지막 금액 선택
        // ("남은 지원금 122,920원 2,800원" 구조에서 실결제는 뒤에 오는 경향)
        return paymentCandidates.lastOrNull()
    }

    private fun isBalanceAmountCandidate(text: String, match: MatchResult): Boolean {
        val lineRange = text.lineRangeContaining(match.range)
        val line = text.substring(lineRange)
        val relativeAmountRange = (match.range.first - lineRange.first)..(match.range.last - lineRange.first)

        return balanceInfoPattern.findAll(line).any { balanceMatch ->
            balanceMatch.range.overlaps(relativeAmountRange)
        }
    }

    private fun hasPaymentKeywordNear(text: String, range: IntRange): Boolean {
        val context = text.localContext(
            start = (range.first - KEYWORD_LOOK_BEHIND).coerceAtLeast(0),
            end = (range.last + 1 + KEYWORD_LOOK_AHEAD).coerceAtMost(text.length)
        )

        return paymentAmountKeywords.any { keyword ->
            normalizeKeywordContext(context).contains(normalizeKeywordContext(keyword))
        }
    }

    private fun String.localContext(start: Int, end: Int): String =
        substring(start, end.coerceAtLeast(start))

    private fun String.lineRangeContaining(range: IntRange): IntRange {
        val lineStart = lastIndexOf('\n', startIndex = range.first).let { index ->
            if (index == -1) 0 else index + 1
        }
        val lineEnd = indexOf('\n', startIndex = range.last + 1).let { index ->
            if (index == -1) length else index
        }

        return lineStart until lineEnd
    }

    private fun normalizeKeywordContext(value: String): String =
        value.filterNot { it.isWhitespace() }

    private fun IntRange.overlaps(other: IntRange): Boolean =
        first <= other.last && other.first <= last

    private fun String.cleanMerchantCandidate(): String {
        val withoutBalance = replace(balanceInfoPattern, " ")
        val pipePreferred = if ('|' in withoutBalance) {
            withoutBalance.substringAfterLast('|')
        } else {
            withoutBalance
        }

        return pipePreferred
            .lineSequence()
            .map { line -> line.trim() }
            .filterNot { line -> line.startsWith("잔액") }
            .joinToString(separator = " ")
            .replace(bracketTagPattern, "")
            .replace(accountCardWordPattern, " ")
            .trim()
            .replace(merchantCleanupPattern, "")
            .trim()
            .replace(Regex("""\s+"""), " ")
    }

    private const val KEYWORD_LOOK_BEHIND = 12
    private const val KEYWORD_LOOK_AHEAD = 12
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
