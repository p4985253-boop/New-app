package com.example.audio

import com.example.data.model.LanguageMode

object AudioSegmentResolver {

    /**
     * Decomposes a monetary amount into a sequence of local asset paths.
     * E.g., for 250 in Hindi -> ["audio/hi/2.wav", "audio/hi/sau.wav", "audio/hi/50.wav"]
     */
    fun resolveAmountSegments(amount: Double, language: LanguageMode): List<String> {
        val longVal = amount.toLong().coerceAtLeast(1)
        val langCode = language.code
        val segments = mutableListOf<String>()

        decomposeNumber(longVal, langCode, segments)

        return segments
    }

    private fun decomposeNumber(n: Long, langCode: String, result: MutableList<String>) {
        if (n <= 0) return

        if (n >= 100000) {
            val lakhs = n / 100000
            val rem = n % 100000
            decomposeSub100(lakhs, langCode, result)
            val lakhWord = when (langCode) {
                "en" -> "lakh"
                "hi" -> "lakh"
                "gu" -> "lakh"
                else -> "lakh"
            }
            result.add("audio/$langCode/$lakhWord.mp3")
            if (rem > 0) decomposeNumber(rem, langCode, result)
            return
        }

        if (n >= 1000) {
            val thousands = n / 1000
            val rem = n % 1000
            decomposeSub100(thousands, langCode, result)
            val thousandWord = when (langCode) {
                "en" -> "thousand"
                "hi" -> "hazaar"
                "gu" -> "hajar"
                else -> "thousand"
            }
            result.add("audio/$langCode/$thousandWord.mp3")
            if (rem > 0) decomposeNumber(rem, langCode, result)
            return
        }

        if (n >= 100) {
            val hundreds = n / 100
            val rem = n % 100
            decomposeSub100(hundreds, langCode, result)
            val hundredWord = when (langCode) {
                "en" -> "hundred"
                "hi" -> "sau"
                "gu" -> "so"
                else -> "hundred"
            }
            result.add("audio/$langCode/$hundredWord.mp3")
            if (rem > 0) decomposeNumber(rem, langCode, result)
            return
        }

        decomposeSub100(n, langCode, result)
    }

    private fun decomposeSub100(n: Long, langCode: String, result: MutableList<String>) {
        if (n <= 0) return

        if (n <= 20) {
            result.add("audio/$langCode/$n.mp3")
            return
        }

        val tens = (n / 10) * 10
        val ones = n % 10

        result.add("audio/$langCode/$tens.mp3")
        if (ones > 0) {
            result.add("audio/$langCode/$ones.mp3")
        }
    }

    /**
     * Resolves the introductory conjunction connector.
     * E.g.
     * English: "[Name] sent" -> "audio/en/sent.mp3"
     * Hindi: "[Name] se" -> "audio/hi/se.mp3"
     * Gujarati: "[Name] tarafthi" -> "audio/gu/tarafthi.mp3"
     */
    fun getConnectorSegment(language: LanguageMode): String {
        return when (language) {
            LanguageMode.ENGLISH -> "audio/en/sent.mp3"
            LanguageMode.HINDI -> "audio/hi/se.mp3"
            LanguageMode.GUJARATI -> "audio/gu/tarafthi.mp3"
        }
    }

    /**
     * Resolves the closing currency & completion phrase.
     * E.g.
     * English: "Rupees" -> "audio/en/rupees.mp3"
     * Hindi: "Rupaye prapt hue" -> "audio/hi/rupaye_prapt_hue.mp3"
     * Gujarati: "Rupiya malya" -> "audio/gu/rupiya_malya.mp3"
     */
    fun getClosingSegment(language: LanguageMode): String {
        return when (language) {
            LanguageMode.ENGLISH -> "audio/en/rupees.mp3"
            LanguageMode.HINDI -> "audio/hi/rupaye_prapt_hue.mp3"
            LanguageMode.GUJARATI -> "audio/gu/rupiya_malya.mp3"
        }
    }
}
