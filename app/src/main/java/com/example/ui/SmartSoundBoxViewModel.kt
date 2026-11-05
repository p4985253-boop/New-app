package com.example.ui

import android.app.Application
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioAnnouncementEngine
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionEntity
import com.example.data.model.LanguageMode
import com.example.data.model.ParsedPayment
import com.example.data.model.UpiProvider
import com.example.data.repository.TransactionRepository
import com.example.parser.UpiNotificationParser
import com.example.service.SmartNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmartSoundBoxViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    private val audioEngine: AudioAnnouncementEngine

    val transactions: StateFlow<List<TransactionEntity>>

    private val _selectedLanguage = MutableStateFlow(SmartNotificationListenerService.selectedLanguage)
    val selectedLanguage: StateFlow<LanguageMode> = _selectedLanguage.asStateFlow()

    private val _isChimeEnabled = MutableStateFlow(SmartNotificationListenerService.isChimeEnabled)
    val isChimeEnabled: StateFlow<Boolean> = _isChimeEnabled.asStateFlow()

    private val _hasNotificationAccess = MutableStateFlow(false)
    val hasNotificationAccess: StateFlow<Boolean> = _hasNotificationAccess.asStateFlow()

    private val _isBatteryOptimizationIgnored = MutableStateFlow(false)
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

    val isSpeaking: StateFlow<Boolean>
    val currentAnnouncement: StateFlow<String?>

    init {
        val database = AppDatabase.getInstance(application)
        repository = TransactionRepository(database.transactionDao())
        audioEngine = AudioAnnouncementEngine.getInstance(application)

        transactions = repository.recentTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        isSpeaking = audioEngine.isSpeaking
        currentAnnouncement = audioEngine.currentAnnouncement

        checkPermissions()
    }

    fun checkPermissions() {
        val context = getApplication<Application>()
        _hasNotificationAccess.value = checkNotificationListenerPermission(context)
        _isBatteryOptimizationIgnored.value = checkBatteryOptimizationIgnored(context)
    }

    fun setLanguage(language: LanguageMode) {
        _selectedLanguage.value = language
        SmartNotificationListenerService.selectedLanguage = language
    }

    fun toggleChime(enabled: Boolean) {
        _isChimeEnabled.value = enabled
        SmartNotificationListenerService.isChimeEnabled = enabled
    }

    fun simulatePayment(amount: Double, senderName: String, provider: UpiProvider) {
        viewModelScope.launch {
            val payment = ParsedPayment(
                amount = amount,
                formattedAmount = UpiNotificationParser.formatAmount(amount),
                senderName = senderName.trim().ifEmpty { "Customer" },
                provider = provider,
                rawText = "Simulated $provider: Received ₹${UpiNotificationParser.formatAmount(amount)} from $senderName",
                timestamp = System.currentTimeMillis()
            )

            // 1. Insert into Room database
            val entity = TransactionEntity(
                senderName = payment.senderName,
                amount = payment.amount,
                formattedAmount = payment.formattedAmount,
                provider = payment.provider.name,
                rawText = payment.rawText,
                language = _selectedLanguage.value.code,
                timestamp = payment.timestamp
            )
            repository.insertTransaction(entity)

            // 2. Play audio announcement
            audioEngine.enqueueAnnouncement(
                payment = payment,
                language = _selectedLanguage.value,
                chimeEnabled = _isChimeEnabled.value
            )
        }
    }

    fun replayTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val providerEnum = try {
                UpiProvider.valueOf(transaction.provider)
            } catch (_: Exception) {
                UpiProvider.GENERIC_BANK
            }

            val payment = ParsedPayment(
                amount = transaction.amount,
                formattedAmount = transaction.formattedAmount,
                senderName = transaction.senderName,
                provider = providerEnum,
                rawText = transaction.rawText,
                timestamp = transaction.timestamp
            )

            val lang = when (transaction.language) {
                "hi" -> LanguageMode.HINDI
                "gu" -> LanguageMode.GUJARATI
                else -> _selectedLanguage.value
            }

            audioEngine.enqueueAnnouncement(
                payment = payment,
                language = lang,
                chimeEnabled = _isChimeEnabled.value
            )
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    private fun checkNotificationListenerPermission(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    private fun checkBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }
}
