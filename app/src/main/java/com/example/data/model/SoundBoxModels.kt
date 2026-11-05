package com.example.data.model

enum class UpiProvider(val displayName: String) {
    GOOGLE_PAY("Google Pay"),
    PHONEPE("PhonePe"),
    PAYTM("Paytm"),
    GENERIC_BANK("Bank UPI")
}

enum class LanguageMode(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिन्दी"),
    GUJARATI("gu", "Gujarati", "ગુજરાતી")
}

data class ParsedPayment(
    val amount: Double,
    val formattedAmount: String,
    val senderName: String,
    val provider: UpiProvider,
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis()
)
