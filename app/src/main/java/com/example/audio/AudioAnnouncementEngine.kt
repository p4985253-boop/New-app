package com.example.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.LanguageMode
import com.example.data.model.ParsedPayment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Enterprise Audio Queue Manager and Hybrid Audio Stitcher.
 * Uses Media3 ExoPlayer for gapless audio concatenation.
 * Strictly avoids Android native TextToSpeech.
 */
class AudioAnnouncementEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val queueMutex = Mutex()
    private val ttsClient = EdgeTtsClient(context)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentAnnouncement = MutableStateFlow<String?>(null)
    val currentAnnouncement: StateFlow<String?> = _currentAnnouncement.asStateFlow()

    private var exoPlayer: ExoPlayer? = null

    init {
        scope.launch {
            initPlayerOnMainThread()
        }
    }

    private suspend fun initPlayerOnMainThread() = withContext(Dispatchers.Main) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                playWhenReady = true
            }
        }
    }

    /**
     * Enqueues a parsed payment for natural voice announcement.
     * Concurrent calls are safely serialized via queueMutex.
     */
    fun enqueueAnnouncement(
        payment: ParsedPayment,
        language: LanguageMode,
        chimeEnabled: Boolean = true,
        onComplete: (() -> Unit)? = null
    ) {
        scope.launch {
            queueMutex.withLock {
                try {
                    _isSpeaking.value = true
                    _currentAnnouncement.value = "${payment.senderName} - ₹${payment.formattedAmount}"
                    playPaymentAnnouncement(payment, language, chimeEnabled)
                } catch (e: Exception) {
                    Log.e("AudioEngine", "Announcement failed: ${e.message}", e)
                } finally {
                    _isSpeaking.value = false
                    _currentAnnouncement.value = null
                    onComplete?.invoke()
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun playPaymentAnnouncement(
        payment: ParsedPayment,
        language: LanguageMode,
        chimeEnabled: Boolean
    ) {
        val mediaItems = mutableListOf<MediaItem>()

        // 1. Alert Chime (Tun-tun sound)
        if (chimeEnabled) {
            mediaItems.add(MediaItem.fromUri(Uri.parse("asset:///audio/chime.wav")))
        }

        // 2. Build full natural voice sentence if online
        val intAmount = payment.amount.toLong()
        val naturalSentence = when (language) {
            LanguageMode.ENGLISH -> "${payment.senderName} sent $intAmount Rupees"
            LanguageMode.HINDI -> "${payment.senderName} से $intAmount रुपये प्राप्त हुए"
            LanguageMode.GUJARATI -> "${payment.senderName} તરફથી $intAmount રૂપિયા મળ્યા"
        }

        val fullSentenceFile = ttsClient.getAudioForFullSentence(naturalSentence, language.code)

        if (fullSentenceFile != null && fullSentenceFile.exists() && fullSentenceFile.length() > 1000) {
            // High-fidelity natural spoken sentence
            mediaItems.add(MediaItem.fromUri(Uri.fromFile(fullSentenceFile)))
        } else {
            // Hybrid Stitched Audio Fallback using local human voice clips
            val nameAudioFile = ttsClient.getAudioFileForName(payment.senderName)
            if (nameAudioFile.exists() && nameAudioFile.length() > 400) {
                mediaItems.add(MediaItem.fromUri(Uri.fromFile(nameAudioFile)))
            }

            // Connector: "sent" / "se" / "tarafthi"
            val connectorAsset = AudioSegmentResolver.getConnectorSegment(language)
            mediaItems.add(MediaItem.fromUri(Uri.parse("asset:///$connectorAsset")))

            // Decomposed amount numbers
            val amountSegments = AudioSegmentResolver.resolveAmountSegments(payment.amount, language)
            for (segment in amountSegments) {
                mediaItems.add(MediaItem.fromUri(Uri.parse("asset:///$segment")))
            }

            // Closing currency: "Rupees" / "Rupaye prapt hue" / "Rupiya malya"
            val closingAsset = AudioSegmentResolver.getClosingSegment(language)
            mediaItems.add(MediaItem.fromUri(Uri.parse("asset:///$closingAsset")))
        }

        // 3. Play sequence gaplessly and await completion
        playSequenceAwaitingCompletion(mediaItems)
    }

    private suspend fun playSequenceAwaitingCompletion(mediaItems: List<MediaItem>) = withContext(Dispatchers.Main) {
        initPlayerOnMainThread()
        val player = exoPlayer ?: return@withContext

        val completionDeferred = CompletableDeferred<Unit>()

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    player.removeListener(this)
                    if (!completionDeferred.isCompleted) {
                        completionDeferred.complete(Unit)
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.w("AudioEngine", "ExoPlayer playback warning: ${error.message}")
                player.removeListener(this)
                if (!completionDeferred.isCompleted) {
                    completionDeferred.complete(Unit)
                }
            }
        }

        player.addListener(listener)
        player.stop()
        player.clearMediaItems()
        player.setMediaItems(mediaItems)
        player.prepare()
        player.play()

        try {
            completionDeferred.await()
        } finally {
            player.removeListener(listener)
        }
    }

    fun release() {
        scope.launch(Dispatchers.Main) {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioAnnouncementEngine? = null

        fun getInstance(context: Context): AudioAnnouncementEngine {
            return INSTANCE ?: synchronized(this) {
                val instance = AudioAnnouncementEngine(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
