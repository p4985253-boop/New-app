package com.example.parser

import com.example.data.model.ParsedPayment
import com.example.data.model.UpiProvider
import java.text.DecimalFormat
import java.util.Locale
import java.util.regex.Pattern

object UpiNotificationParser {

    const val PACKAGE_GPAY = "com.google.android.apps.nbu.paisa.user"
    const val PACKAGE_PHONEPE = "com.phonepe.app"
    const val PACKAGE_PAYTM = "net.one97.paytm"

    // Currency regex that captures amounts with or without commas and decimals
    // Matches ₹1,500, ₹1500.50, Rs. 250, Rs 500, INR 1000.00
    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:[₹]|Rs\.?|INR)\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    // Regex for outgoing/debit notifications (e.g. "Paid ₹250 to ...", "Sent ₹500 to ...", "Debited ...")
    private val DEBIT_PATTERN = Pattern.compile(
        """\b(?:paid|sent|transferred)\b.*?\bto\b|\b(?:debited|debit|withdrawn|spent|payment to|declined|failed|otp|recharge successful|bill payment|auto-debit)\b""",
        Pattern.CASE_INSENSITIVE
    )

    private val decimalFormat = DecimalFormat("#,##,##0.##")

    /**
     * Parses notification incoming data. Returns null if notification is not an incoming UPI credit.
     */
    fun parse(
        packageName: String?,
        title: String?,
        text: String?,
        timestamp: Long = System.currentTimeMillis()
    ): ParsedPayment? {
        val safeTitle = title?.trim() ?: ""
        val safeText = text?.trim() ?: ""
        val combined = "$safeTitle $safeText".trim()

        if (combined.isEmpty()) return null

        val lowerCombined = combined.lowercase(Locale.ROOT)

        // 1. Check for incoming credit overrides ("paid you", "sent you")
        val isExplicitCredit = lowerCombined.contains("paid you") || lowerCombined.contains("sent you")

        // 2. Filter out debit / outgoing / non-credit events
        if (!isExplicitCredit && DEBIT_PATTERN.matcher(lowerCombined).find()) {
            return null
        }

        // 3. Determine UPI provider
        val provider = when (packageName) {
            PACKAGE_GPAY -> UpiProvider.GOOGLE_PAY
            PACKAGE_PHONEPE -> UpiProvider.PHONEPE
            PACKAGE_PAYTM -> UpiProvider.PAYTM
            else -> {
                when {
                    lowerCombined.contains("google pay") || lowerCombined.contains("gpay") -> UpiProvider.GOOGLE_PAY
                    lowerCombined.contains("phonepe") -> UpiProvider.PHONEPE
                    lowerCombined.contains("paytm") -> UpiProvider.PAYTM
                    else -> UpiProvider.GENERIC_BANK
                }
            }
        }

        // 4. Provider-specific parsers
        return when (provider) {
            UpiProvider.GOOGLE_PAY -> parseGooglePay(safeTitle, safeText, combined, timestamp)
            UpiProvider.PHONEPE -> parsePhonePe(safeTitle, safeText, combined, timestamp)
            UpiProvider.PAYTM -> parsePaytm(safeTitle, safeText, combined, timestamp)
            UpiProvider.GENERIC_BANK -> parseGenericBank(safeTitle, safeText, combined, timestamp)
        }
    }

    private fun parseGooglePay(
        title: String,
        text: String,
        combined: String,
        timestamp: Long
    ): ParsedPayment? {
        val amount = extractAmount(combined) ?: return null
        val targetText = if (isAppName(title)) text else combined
        var sender = ""

        val paidYouPattern = Pattern.compile(
            """(.+?)\s+paid you\s+(?:[₹]|Rs\.?|INR)""",
            Pattern.CASE_INSENSITIVE
        )
        val paidYouMatcher = paidYouPattern.matcher(targetText)

        if (paidYouMatcher.find()) {
            sender = paidYouMatcher.group(1)?.trim() ?: ""
        } else if (text.contains("paid you", ignoreCase = true) && title.isNotEmpty() && !isAppName(title)) {
            sender = title
        } else {
            sender = extractSenderAfterFrom(targetText)
        }

        sender = cleanSenderName(sender)
        if (sender.isEmpty()) {
            sender = if (title.isNotEmpty() && !isAppName(title)) title else "Customer"
        }

        return ParsedPayment(
            amount = amount,
            formattedAmount = formatAmount(amount),
            senderName = sender,
            provider = UpiProvider.GOOGLE_PAY,
            rawText = combined,
            timestamp = timestamp
        )
    }

    private fun parsePhonePe(
        title: String,
        text: String,
        combined: String,
        timestamp: Long
    ): ParsedPayment? {
        val amount = extractAmount(combined) ?: return null
        val targetText = if (isAppName(title)) text else combined
        var sender = extractSenderAfterFrom(targetText)

        if (sender.isEmpty()) {
            val sentYouPattern = Pattern.compile(
                """(.+?)\s+sent you\s+(?:[₹]|Rs\.?|INR)""",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = sentYouPattern.matcher(targetText)
            if (matcher.find()) {
                sender = matcher.group(1)?.trim() ?: ""
            }
        }

        if (sender.isEmpty() && title.isNotEmpty() && !isAppName(title)) {
            sender = title
        }

        sender = cleanSenderName(sender)
        if (sender.isEmpty()) sender = "Customer"

        return ParsedPayment(
            amount = amount,
            formattedAmount = formatAmount(amount),
            senderName = sender,
            provider = UpiProvider.PHONEPE,
            rawText = combined,
            timestamp = timestamp
        )
    }

    private fun parsePaytm(
        title: String,
        text: String,
        combined: String,
        timestamp: Long
    ): ParsedPayment? {
        val amount = extractAmount(combined) ?: return null
        val targetText = if (isAppName(title)) text else combined
        var sender = extractSenderAfterFrom(targetText)

        if (sender.isEmpty() && title.isNotEmpty() && !isAppName(title)) {
            sender = title
        }

        sender = cleanSenderName(sender)
        if (sender.isEmpty()) sender = "Customer"

        return ParsedPayment(
            amount = amount,
            formattedAmount = formatAmount(amount),
            senderName = sender,
            provider = UpiProvider.PAYTM,
            rawText = combined,
            timestamp = timestamp
        )
    }

    private fun parseGenericBank(
        title: String,
        text: String,
        combined: String,
        timestamp: Long
    ): ParsedPayment? {
        val lower = combined.lowercase(Locale.ROOT)
        val isCredit = lower.contains("credited") || lower.contains("credit") ||
                lower.contains("received") || lower.contains("deposit")

        if (!isCredit) return null

        val amount = extractAmount(combined) ?: return null
        var sender = extractSenderAfterFrom(combined)

        if (sender.isEmpty()) {
            val byPattern = Pattern.compile(
                """(?:by|from)\s+([A-Za-z\s]{3,30})(?:\s+on|\.|\z)""",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = byPattern.matcher(combined)
            if (matcher.find()) {
                sender = matcher.group(1)?.trim() ?: ""
            }
        }

        sender = cleanSenderName(sender)
        if (sender.isEmpty()) sender = "Customer"

        return ParsedPayment(
            amount = amount,
            formattedAmount = formatAmount(amount),
            senderName = sender,
            provider = UpiProvider.GENERIC_BANK,
            rawText = combined,
            timestamp = timestamp
        )
    }

    private fun extractAmount(text: String): Double? {
        val matcher = AMOUNT_PATTERN.matcher(text)
        if (matcher.find()) {
            val cleanStr = matcher.group(1)?.replace(",", "")?.trim() ?: return null
            val value = cleanStr.toDoubleOrNull()
            if (value != null && value > 0.0) {
                return value
            }
        }
        return null
    }

    private fun extractSenderAfterFrom(text: String): String {
        // Matches "... from Amit Patel via Paytm UPI", "... from Suresh Kumar received.", "... from Rajesh Kumar."
        val fromPattern = Pattern.compile(
            """from\s+([A-Za-z0-9\s]{2,40}?)(?:\s+via|\s+using|\s+on|\s+through|\s+received|\.|\z)""",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = fromPattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim() ?: ""
        }
        return ""
    }

    private fun cleanSenderName(name: String): String {
        var cleaned = name
            // Remove common trailing UPI/provider artifacts
            .replace(Regex("""(?i)\bvia\s+(?:paytm|phonepe|google pay|gpay|upi).*"""), "")
            .replace(Regex("""(?i)\busing\s+.*"""), "")
            .replace(Regex("""(?i)\bthrough\s+.*"""), "")
            .replace(Regex("""(?i)\bon\s+.*"""), "")
            .replace(Regex("""(?i)\bref\s*(?:no|id)?.*"""), "")
            .replace(Regex("""(?i)\breceived\b.*"""), "")
            .replace(Regex("""[^\w\s]"""), " ") // Strip punctuation like asterisks, dashes
            .replace(Regex("""\s+"""), " ")
            .trim()

        // Filter out if name accidentally captured app names or account numbers
        if (isAppName(cleaned) || cleaned.matches(Regex("""[0-9*X]+"""))) {
            return ""
        }

        return cleaned
    }

    private fun isAppName(name: String): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        return lower in listOf("google pay", "gpay", "phonepe", "paytm", "bhim", "upi", "bank", "received money", "payment received")
    }

    fun formatAmount(amount: Double): String {
        return decimalFormat.format(amount)
    }
}
