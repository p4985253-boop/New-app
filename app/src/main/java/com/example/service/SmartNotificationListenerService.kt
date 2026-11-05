package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.AudioAnnouncementEngine
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionEntity
import com.example.data.model.LanguageMode
import com.example.data.model.ParsedPayment
import com.example.data.repository.TransactionRepository
import com.example.parser.UpiNotificationParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Enterprise Background Notification Listener Service.
 * Intercepts incoming UPI notifications from Google Pay, PhonePe, Paytm, and Banks,
 * saves them to the Room database, and triggers voice announcements aloud.
 */
class SmartNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var audioEngine: AudioAnnouncementEngine
    private lateinit var repository: TransactionRepository
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "soundbox_foreground_channel"
        @Volatile
        var isServiceRunning = false
            private set

        @Volatile
        var selectedLanguage: LanguageMode = LanguageMode.ENGLISH

        @Volatile
        var isChimeEnabled: Boolean = true
    }

    override fun onCreate() {
        super.onCreate()
        audioEngine = AudioAnnouncementEngine.getInstance(applicationContext)
        val database = AppDatabase.getInstance(applicationContext)
        repository = TransactionRepository(database.transactionDao())

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SmartSoundBox::WakeLock")

        createNotificationChannel()
        startForegroundServiceWithNotification()
        isServiceRunning = true
        Log.i("SoundBoxService", "SmartNotificationListenerService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Return START_STICKY to guarantee auto-restart if killed by system
        return START_STICKY
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        // Combine text and bigText if available for maximum parsing accuracy
        val resolvedText = when {
            !bigText.isNullOrEmpty() && (text.isNullOrEmpty() || bigText.length > text.length) -> bigText
            else -> text ?: ""
        }

        // Parse UPI notification
        val parsedPayment = UpiNotificationParser.parse(
            packageName = packageName,
            title = title,
            text = resolvedText,
            timestamp = sbn.postTime
        )

        if (parsedPayment != null) {
            Log.i("SoundBoxService", "Intercepted UPI Payment: ₹${parsedPayment.formattedAmount} from ${parsedPayment.senderName} (${parsedPayment.provider})")
            processPayment(parsedPayment)
        }
    }

    private fun processPayment(payment: ParsedPayment) {
        serviceScope.launch {
            // 1. Acquire partial wake lock to prevent CPU sleep during speech
            try {
                wakeLock?.acquire(15000L)
            } catch (e: Exception) {
                Log.w("SoundBoxService", "Could not acquire wakelock: ${e.message}")
            }

            // 2. Persist to Room Database
            try {
                val entity = TransactionEntity(
                    senderName = payment.senderName,
                    amount = payment.amount,
                    formattedAmount = payment.formattedAmount,
                    provider = payment.provider.name,
                    rawText = payment.rawText,
                    language = selectedLanguage.code,
                    timestamp = payment.timestamp
                )
                repository.insertTransaction(entity)
            } catch (e: Exception) {
                Log.e("SoundBoxService", "Failed to save transaction: ${e.message}")
            }

            // 3. Dispatch to Audio Announcement Engine
            audioEngine.enqueueAnnouncement(
                payment = payment,
                language = selectedLanguage,
                chimeEnabled = isChimeEnabled,
                onComplete = {
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock?.release()
                        }
                    } catch (e: Exception) {
                        Log.w("SoundBoxService", "Error releasing wakelock: ${e.message}")
                    }
                }
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_soundbox_status),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.service_running_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceWithNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_running_title))
            .setContentText(getString(R.string.service_running_desc))
            .setSmallIcon(R.drawable.ic_soundbox_fg)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        serviceScope.cancel()
        Log.i("SoundBoxService", "SmartNotificationListenerService destroyed")
    }
}
