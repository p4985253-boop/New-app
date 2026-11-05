package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.service.SmartNotificationListenerService
import com.example.ui.SmartSoundBoxViewModel
import com.example.ui.screens.SoundBoxMainScreen
import com.example.ui.theme.SmartSoundBoxTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SmartSoundBoxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start background service to ensure it's active
        try {
            val serviceIntent = Intent(this, SmartNotificationListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (_: Exception) {
            // System will manage NotificationListenerService lifecycle when granted
        }

        setContent {
            SmartSoundBoxTheme {
                SoundBoxMainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }
}
