package com.example.audio

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Natural human speech audio client for dynamic sender names and natural speech phrases.
 * Bypasses robotic native Android TTS and caches all audio files to disk.
 */
class EdgeTtsClient(private val context: Context) {

    private val cacheDir = File(context.cacheDir, "names").apply { mkdirs() }
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun getAudioFileForName(senderName: String): File = withContext(Dispatchers.IO) {
        val cleanName = senderName.trim().ifEmpty { "Customer" }
        val hash = md5(cleanName.lowercase())
        val cachedFile = File(cacheDir, "$hash.mp3")

        if (cachedFile.exists() && cachedFile.length() > 500) {
            return@withContext cachedFile
        }

        try {
            val success = fetchSpeechAudio(cleanName, "en", cachedFile)
            if (success && cachedFile.exists() && cachedFile.length() > 500) {
                return@withContext cachedFile
            }
        } catch (e: Exception) {
            Log.w("EdgeTtsClient", "Online name audio fetch failed: ${e.message}")
        }

        return@withContext cachedFile
    }

    /**
     * Synthesizes a complete natural human voice announcement sentence when internet is available.
     * Caches to disk. Returns null if offline/network unavailable.
     */
    suspend fun getAudioForFullSentence(sentence: String, langCode: String): File? = withContext(Dispatchers.IO) {
        val hash = md5("${langCode}_$sentence")
        val cachedFile = File(cacheDir, "$hash.mp3")

        if (cachedFile.exists() && cachedFile.length() > 1000) {
            return@withContext cachedFile
        }

        try {
            val success = fetchSpeechAudio(sentence, langCode, cachedFile)
            if (success && cachedFile.exists() && cachedFile.length() > 1000) {
                return@withContext cachedFile
            }
        } catch (e: Exception) {
            Log.w("EdgeTtsClient", "Online full sentence audio fetch failed: ${e.message}")
        }

        return@withContext null
    }

    private fun fetchSpeechAudio(text: String, langCode: String, targetFile: File): Boolean {
        val encodedText = URLEncoder.encode(text, "UTF-8")
        val requestUrl = "https://translate.google.com/translate_tts?ie=UTF-8&q=$encodedText&tl=$langCode&client=tw-ob"

        val request = Request.Builder()
            .url(requestUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w("EdgeTtsClient", "TTS request HTTP error: ${response.code}")
                return false
            }
            val body = response.body ?: return false
            targetFile.outputStream().use { out ->
                body.byteStream().copyTo(out)
            }
            return targetFile.length() > 400
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
