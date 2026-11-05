package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TransactionEntity
import com.example.data.model.LanguageMode
import com.example.data.model.UpiProvider
import com.example.ui.SmartSoundBoxViewModel
import com.example.ui.theme.GPayBlue
import com.example.ui.theme.GenericBankGreen
import com.example.ui.theme.PaytmCyan
import com.example.ui.theme.PhonePePurple
import com.example.ui.theme.SoundboxAmber
import com.example.ui.theme.SoundboxCardBg
import com.example.ui.theme.SoundboxCardBorder
import com.example.ui.theme.SoundboxDarkBg
import com.example.ui.theme.SoundboxEmerald
import com.example.ui.theme.SoundboxSurface
import com.example.ui.theme.SoundboxTeal
import com.example.ui.theme.SoundboxTextPrimary
import com.example.ui.theme.SoundboxTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundBoxMainScreen(
    viewModel: SmartSoundBoxViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe lifecycle so returning from system settings refreshes permissions automatically
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val isChimeEnabled by viewModel.isChimeEnabled.collectAsStateWithLifecycle()
    val hasNotificationAccess by viewModel.hasNotificationAccess.collectAsStateWithLifecycle()
    val isBatteryOptimized by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val currentAnnouncement by viewModel.currentAnnouncement.collectAsStateWithLifecycle()

    var showSimulatorDrawer by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = SoundboxDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (hasNotificationAccess) SoundboxEmerald else SoundboxAmber)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SmartSoundBox",
                            fontWeight = FontWeight.Bold,
                            color = SoundboxTextPrimary,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleChime(!isChimeEnabled) },
                        modifier = Modifier.testTag("chime_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isChimeEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                            contentDescription = "Chime Toggle",
                            tint = if (isChimeEnabled) SoundboxEmerald else SoundboxTextSecondary
                        )
                    }
                    if (transactions.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearHistory() },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = SoundboxTextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SoundboxDarkBg
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Live Speaker Status & Language Bar
            item {
                SpeakerStatusCard(
                    isSpeaking = isSpeaking,
                    currentAnnouncement = currentAnnouncement,
                    selectedLanguage = selectedLanguage,
                    onSelectLanguage = { viewModel.setLanguage(it) }
                )
            }

            // 2. Permission Warnings (if not granted)
            item {
                AnimatedVisibility(visible = !hasNotificationAccess || !isBatteryOptimized) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!hasNotificationAccess) {
                            PermissionWarningCard(
                                title = "Notification Access Required",
                                description = "SmartSoundBox needs notification permission to intercept UPI credits.",
                                buttonLabel = "Grant Access",
                                icon = Icons.Default.NotificationsActive,
                                onClick = {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    context.startActivity(intent)
                                }
                            )
                        }
                        if (!isBatteryOptimized) {
                            PermissionWarningCard(
                                title = "Background Run Exemption",
                                description = "Exclude from battery saver to announce payments 24/7 without delays.",
                                buttonLabel = "Enable 24/7",
                                icon = Icons.Default.BatteryAlert,
                                onClick = {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 3. Payment Simulator Toggle & Action
            item {
                PaymentSimulatorSection(
                    onSimulatePayment = { amount, name, provider ->
                        viewModel.simulatePayment(amount, name, provider)
                    }
                )
            }

            // 4. Ledger Header
            item {
                val totalAmount = transactions.sumOf { it.amount }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Payment Ledger",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoundboxTextPrimary
                        )
                        Text(
                            text = "${transactions.size} payments intercepted",
                            fontSize = 12.sp,
                            color = SoundboxTextSecondary
                        )
                    }
                    if (transactions.isNotEmpty()) {
                        Surface(
                            color = SoundboxSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SoundboxCardBorder)
                        ) {
                            Text(
                                text = "Total: ₹${String.format(Locale.getDefault(), "%,.2f", totalAmount)}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SoundboxEmerald
                            )
                        }
                    }
                }
            }

            // 5. Transactions List
            if (transactions.isEmpty()) {
                item {
                    EmptyTransactionsPlaceholder()
                }
            } else {
                items(transactions, key = { it.id }) { transaction ->
                    TransactionItemCard(
                        transaction = transaction,
                        onReplay = { viewModel.replayTransaction(transaction) },
                        onDelete = { viewModel.deleteTransaction(transaction.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SpeakerStatusCard(
    isSpeaking: Boolean,
    currentAnnouncement: String?,
    selectedLanguage: LanguageMode,
    onSelectLanguage: (LanguageMode) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isSpeaking) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("speaker_status_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoundboxCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSpeaking) SoundboxEmerald else SoundboxCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated soundbox icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(if (isSpeaking) pulseScale else 1.0f)
                        .clip(CircleShape)
                        .background(if (isSpeaking) SoundboxEmerald else SoundboxSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speaker",
                        tint = if (isSpeaking) Color.Black else SoundboxEmerald,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isSpeaking) "ANNOUNCING PAYMENT..." else "SMARTSOUNDBOX ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSpeaking) SoundboxEmerald else SoundboxTeal,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = currentAnnouncement ?: "Listening for Google Pay, PhonePe & Paytm",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SoundboxTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Language Selector Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ANNOUNCEMENT LANGUAGE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoundboxTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LanguageMode.entries.forEach { lang ->
                        val isSelected = lang == selectedLanguage
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectLanguage(lang) },
                            label = {
                                Text(
                                    text = "${lang.displayName} (${lang.nativeName})",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoundboxEmerald,
                                selectedLabelColor = Color.Black,
                                containerColor = SoundboxSurface,
                                labelColor = SoundboxTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = SoundboxCardBorder,
                                selectedBorderColor = SoundboxEmerald
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lang_chip_${lang.code}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionWarningCard(
    title: String,
    description: String,
    buttonLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1F10)),
        border = androidx.compose.foundation.BorderStroke(1.dp, SoundboxAmber)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SoundboxAmber,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SoundboxAmber
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = SoundboxTextPrimary.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = SoundboxAmber, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("permission_button")
            ) {
                Text(text = buttonLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PaymentSimulatorSection(
    onSimulatePayment: (Double, String, UpiProvider) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var amountInput by remember { mutableStateOf("250") }
    var senderInput by remember { mutableStateOf("Amit Patel") }
    var selectedProvider by remember { mutableStateOf(UpiProvider.GOOGLE_PAY) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_simulator_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoundboxSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SoundboxCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Test Simulator",
                        tint = SoundboxTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Payment Simulator (Live Test)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = SoundboxTextPrimary
                    )
                }
                Text(
                    text = if (expanded) "Hide ▲" else "Open ▼",
                    fontSize = 12.sp,
                    color = SoundboxTeal,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick amount presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(50, 100, 250, 500, 1500).forEach { preset ->
                            OutlinedButton(
                                onClick = { amountInput = preset.toString() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (amountInput == preset.toString()) SoundboxEmerald else SoundboxCardBorder
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (amountInput == preset.toString()) SoundboxCardBg else Color.Transparent
                                )
                            ) {
                                Text(
                                    text = "₹$preset",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (amountInput == preset.toString()) SoundboxEmerald else SoundboxTextSecondary
                                )
                            }
                        }
                    }

                    // Input fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("Amount (₹)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sim_amount_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SoundboxEmerald,
                                unfocusedBorderColor = SoundboxCardBorder,
                                focusedTextColor = SoundboxTextPrimary,
                                unfocusedTextColor = SoundboxTextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = senderInput,
                            onValueChange = { senderInput = it },
                            label = { Text("Sender Name", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("sim_sender_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SoundboxEmerald,
                                unfocusedBorderColor = SoundboxCardBorder,
                                focusedTextColor = SoundboxTextPrimary,
                                unfocusedTextColor = SoundboxTextPrimary
                            )
                        )
                    }

                    // Provider selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        UpiProvider.entries.forEach { prov ->
                            val isSelected = prov == selectedProvider
                            val chipColor = when (prov) {
                                UpiProvider.GOOGLE_PAY -> GPayBlue
                                UpiProvider.PHONEPE -> PhonePePurple
                                UpiProvider.PAYTM -> PaytmCyan
                                UpiProvider.GENERIC_BANK -> GenericBankGreen
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedProvider = prov },
                                label = { Text(prov.displayName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor,
                                    selectedLabelColor = Color.White,
                                    containerColor = SoundboxCardBg,
                                    labelColor = SoundboxTextSecondary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Simulate Trigger Button
                    Button(
                        onClick = {
                            val amount = amountInput.toDoubleOrNull() ?: 250.0
                            onSimulatePayment(amount, senderInput, selectedProvider)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("simulate_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoundboxEmerald,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Test Announce Payment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    onReplay: () -> Unit,
    onDelete: () -> Unit
) {
    val providerColor = when (transaction.provider) {
        UpiProvider.GOOGLE_PAY.name -> GPayBlue
        UpiProvider.PHONEPE.name -> PhonePePurple
        UpiProvider.PAYTM.name -> PaytmCyan
        else -> GenericBankGreen
    }

    val timeFormatted = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(transaction.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SoundboxCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, SoundboxCardBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Provider Indicator Pill
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(providerColor.copy(alpha = 0.18f))
                    .border(1.dp, providerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (transaction.provider) {
                        UpiProvider.GOOGLE_PAY.name -> "G"
                        UpiProvider.PHONEPE.name -> "P"
                        UpiProvider.PAYTM.name -> "Tm"
                        else -> "₹"
                    },
                    color = providerColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.senderName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SoundboxTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.provider,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = providerColor
                    )
                    Text(
                        text = " • $timeFormatted",
                        fontSize = 11.sp,
                        color = SoundboxTextSecondary
                    )
                }
            }

            // Amount Display
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = "₹${transaction.formattedAmount}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = SoundboxEmerald
                )
                Text(
                    text = transaction.language.uppercase(),
                    fontSize = 10.sp,
                    color = SoundboxTextSecondary
                )
            }

            // Replay announcement icon button
            IconButton(
                onClick = onReplay,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("replay_button_${transaction.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Replay Announcement",
                    tint = SoundboxTeal,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete icon
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = SoundboxTextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyTransactionsPlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoundboxSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SoundboxCardBorder)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = SoundboxTextSecondary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "No Payments Intercepted Yet",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = SoundboxTextPrimary
            )
            Text(
                text = "Payments received on Google Pay, PhonePe, Paytm, or bank UPI apps will appear and announce automatically. Use the Payment Simulator above to test!",
                fontSize = 12.sp,
                color = SoundboxTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}
