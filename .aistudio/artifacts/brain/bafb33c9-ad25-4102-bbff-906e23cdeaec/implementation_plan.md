# SmartSoundBox

A production-ready Android enterprise application that intercepts UPI notifications (Google Pay, PhonePe, Paytm, and generic banking) and announces payments aloud using a high-fidelity hybrid audio engine with zero robotic native TTS.

## User Review & Critical Decisions

> [!IMPORTANT]
> All core architectural parameters and user selections have been confirmed:
> - **Dynamic Sender Name TTS**: Free Microsoft Edge TTS neural endpoint with persistent disk caching (`cacheDir/names/`), ensuring zero API keys required and instant replay.
> - **Audio Profile & Alert Chime**: Modern resonant two-tone payment alert chime preceding spoken announcements.
> - **Data Persistence**: Local Room database (`AppDatabase`) with reactive `Flow` updates, storing full transaction logs (amount, sender, provider, timestamp, status).
> - **Languages**: Triple language support (English, Hindi, Gujarati) with pre-recorded/synthesized crisp audio segment stitching for currency and numbers.

---

## 1. Overview & Core Concept

**SmartSoundBox** replicates and elevates physical merchant soundboxes (e.g., Paytm Soundbox, PhonePe SmartSpeaker) into a robust smartphone software solution.
- **What It Does**: Runs a background `NotificationListenerService` that intercepts real-time incoming status-bar notifications from major Indian UPI apps (Google Pay, PhonePe, Paytm, and banking apps), parses transaction amounts and sender identities, logs them to a persistent SQLite Room database, and invokes a synchronized audio engine to announce payments in the merchant's chosen language.
- **Target Audience**: Indian retailers, merchants, shop owners, and kiosk operators who need hands-free, crystal-clear payment verification during busy hours without staring at phone screens.
- **Zero Native TTS Policy**: Bypasses Android's robotic, flat `TextToSpeech` engine entirely. Employs crisp studio-quality segment stitching for numbers and currency combined with neural voice synthesis for dynamic sender names.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Initial Setup & Permissions**:
   - Status cards indicate **Notification Access** and **Battery Optimization Exemption**.
   - One-tap intent triggers redirect users directly to `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` and `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
2. **Soundbox Control Panel**:
   - Visual toggle and radio chips to switch announcement language: **English** (`[Name] sent [Amount] Rupees`), **Hindi** (`[Name] se [Amount] Rupaye prapt hue`), or **Gujarati** (`[Name] tarafthi [Amount] Rupiya malya`).
   - Soundbox power toggle and chime volume status.
3. **Interactive Payment Simulator**:
   - Quick testing drawer to simulate Google Pay, PhonePe, and Paytm payments with customizable sender name, amount, and provider.
   - Triggers the complete pipeline end-to-end: parser -> database -> audio stitching -> chime & speech playback.
4. **Transaction Ledger & Replay**:
   - Real-time reactive list of intercepted transactions with provider badges (GPay, PhonePe, Paytm, Banking).
   - "Re-announce" audio button on each ledger entry for noisy shop environments.

### Visual Identity & Theme
- **Aesthetic Direction**: Sleek, industrial merchant hardware aesthetic with modern fintech polish.
- **Color Palette**:
  - Dominant Background: Deep Slate `#0B132B` / `#1C2541`
  - Accent / Primary: Vibrant Soundbox Emerald `#10B981` & Electric Teal `#06B6D4`
  - Surface Card: Elevated Dark Obsidian `#1E293B` with subtle `#334155` borders
  - Highlights: Crisp White `#F8FAFC` and Gold Amber `#F59E0B` for currency amounts
- **Typography**: Clean, bold numerical displays for instant readability from a distance.

---

## 3. Key Product Decisions & Trade-Offs

- **Hybrid Audio Engine (Edge TTS + Audio Stitcher)**:
  - *Chosen Approach*: Dynamic sender names are generated via high-quality neural voice REST synthesis and saved to disk cache (`cacheDir/names/<hash>.mp3`). Numbers, currency ("Rupees", "Rupaye", "Rupiya"), and sentence conjunctions are stitched from local high-fidelity audio assets.
  - *Why*: Sender names cannot be pre-recorded, but numbers and currency must play with zero latency even when internet connectivity fluctuates.
  - *Alternatives Considered*: Native Android TTS was explicitly banned due to robotic voice quality. Full online audio streaming fails during offline/poor network merchant scenarios.
- **Media3 / ExoPlayer Sequential Playback Queue**:
  - *Chosen Approach*: Android Media3 `ExoPlayer` manages a synchronized playlist queue to prevent overlapping audio when rapid successive transactions arrive.
  - *Why*: Merchant stores frequently receive simultaneous payments during rush hours. A thread-safe FIFO queue ensures every payment is announced distinctly.
- **Foreground NotificationListenerService**:
  - *Chosen Approach*: Service runs with `START_STICKY` and displays a persistent ongoing foreground notification with current Soundbox status.
  - *Why*: Android OS aggressively kills background services; a foreground notification coupled with battery optimization exemption ensures 24/7 uptime.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Android OS Notifications                        │
│      Google Pay          │       PhonePe        │        Paytm         │
└────────────┬─────────────┴──────────┬───────────┴──────────┬───────────┘
             │                        │                      │
             ▼                        ▼                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   SmartNotificationListenerService                     │
│  - Extends NotificationListenerService                                 │
│  - WAKELOCK acquisition & Foreground Service notification              │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ onNotificationPosted
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        UpiNotificationParser                           │
│  - Strict regex matching for GPay, PhonePe, Paytm, & Generic Banks     │
│  - Extracts: Amount (Double), Sender Name (String), Provider (Enum)    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Parsed Payment
                   ┌────────────────┴────────────────┐
                   ▼                                 ▼
┌─────────────────────────────────────┐  ┌───────────────────────────────┐
│       Room Local Persistence        │  │    AudioAnnouncementEngine    │
│  - AppDatabase / TransactionDao     │  │  - FIFO Thread-Safe Queue     │
│  - TransactionEntity table          │  │  - Modern Alert Chime         │
│  - Real-time Flow<List<Transaction>>│  │  - Edge TTS for Sender Name   │
└──────────────────┬──────────────────┘  │  - Number/Currency Stitcher   │
                   │                     │  - Media3 ExoPlayer Engine    │
                   │                     └───────────────────────────────┘
                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      Jetpack Compose UI & ViewModel                    │
│  - SoundboxStatusHeader (Notification & Battery permission checks)     │
│  - LanguageSelector (English, Hindi, Gujarati)                         │
│  - PaymentSimulatorCard (Custom amount & sender testing)               │
│  - TransactionHistoryList (Live ledger with Replay & filter)           │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Model & Entities

```kotlin
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderName: String,
    val amount: Double,
    val formattedAmount: String,
    val provider: UpiProvider, // GOOGLE_PAY, PHONEPE, PAYTM, GENERIC_BANK
    val rawText: String,
    val language: LanguageMode,
    val timestamp: Long = System.currentTimeMillis()
)
```

### Audio Segment Mapping & Number Decomposer
- Numbers decomposed into Indian numbering units:
  - Units (1-99), Hundreds (100, 200, ... 900), Thousands (1,000 ... 99,000), Lakhs.
- High-fidelity pre-synthesized audio segments and modern payment chimes bundled in `app/src/main/assets/audio/{en,hi,gu}/` and `app/src/main/res/raw/`.
- Edge TTS client handles dynamic names with fallback phonetic audio generation.

### CI/CD Workflow (`.github/workflows/build_apk.yml`)
- Automates checkout, JDK 17 setup, `./gradlew testDebugUnitTest`, `./gradlew assembleDebug`, and outputs `app-debug.apk` as a downloadable artifact.

### Unit Test Suite (`UpiNotificationParserTest.kt`)
- Full Robolectric/JUnit test suite testing GPay, PhonePe, Paytm, comma-separated amounts, decimals, and corrupted/non-payment notification resilience.
