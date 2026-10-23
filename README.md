# OfflineQR — Privacy-First Native Android QR & Barcode Suite

**OfflineQR** is a 100% native Kotlin Android application built using Jetpack Compose, CameraX, ZXing, and Room. It is engineered with a strict **air-gapped, zero-network privacy policy**: no internet permission is requested, no external calls are made, and all operations occur entirely on your local device.

---

## Key Features

1. **Instant Camera Scanner**
   - High-performance, offline QR & multi-format barcode recognition powered by CameraX and ZXing.
   - Supports QR Code, Data Matrix, Aztec, PDF417, EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39, Code 93, Codabar, and ITF.
   - Built-in torch toggle, front/rear camera switching, and animated neon laser viewfinder.
   - Duplicate scan throttle to avoid repeated captures.

2. **Smart Scan Result Preview**
   - Intelligently recognizes Website URLs, Wi-Fi configurations, Phone numbers, Email addresses, SMS messages, Geo coordinates, and vCards.
   - **Zero Automatic Navigation**: URLs are validated strictly against HTTP/HTTPS schemes (rejecting `javascript:`, `file:`, etc.) and require explicit user action.
   - One-tap actions for Copy, Share, Dial, Compose Email, SMS, and Maps.

3. **Wi-Fi QR Reader & Connector**
   - Parses standard Wi-Fi strings (`WIFI:T:WPA;S:...;P:...;;`).
   - Supports Show/Hide password toggle, Copy SSID, Copy Password, and secure sharing with credential warning.
   - Seamless integration with Android's system Wi-Fi panel and `WifiNetworkSpecifier` (API 29+).

4. **Multi-Type QR Generator**
   - Create offline QR codes for Plain Text, URLs, Wi-Fi Networks, Phone Numbers, Emails, SMS, and Contacts (vCard).
   - High-resolution bitmap rendering with configurable Error Correction (L, M, Q, H).
   - Save directly to device storage (`Pictures/OfflineQR`) or share via Android Sharesheet.

5. **Gallery Image Scanning**
   - Pick images via the zero-permission Android Photo Picker.
   - Memory-safe decoding with automatic downsampling for high-resolution images.

6. **Local History & Favorites**
   - Persisted on-device via Room SQLite database.
   - Real-time search query filtering and category chips (All, URL, Wi-Fi, Phone, Email, Text).
   - Star/bookmark favorites and manage records with single-item deletion or clear all history (with confirmation).

7. **Cryptographic Privacy Vault Lock**
   - Protect your scan history with a 4-to-6 digit PIN.
   - PINs are cryptographically hashed using **SHA-256 with a unique random 16-byte salt** (never stored in plaintext).
   - Biometric authentication (`BiometricPrompt`) option with PIN fallback.

8. **Offline JSON Backup & Restore**
   - Export your entire scan history to a clean JSON file using the Android Storage Access Framework (SAF).
   - Import existing backups with a choice to **Merge** or **Replace** existing items.

9. **Material 3 Design & Themes**
   - Full edge-to-edge layout with safe drawing and navigation bar insets.
   - Light Mode, Dark Mode, and Follow System Default options.
   - Subtle tactile vibration haptics on scan.

---

## Permissions & Security

| Permission | Reason |
|---|---|
| `android.permission.CAMERA` | Real-time optical QR and barcode capture |
| `android.permission.VIBRATE` | Tactile haptic feedback on successful scan |
| `android.permission.ACCESS_WIFI_STATE` | Wi-Fi helper queries |
| `android.permission.CHANGE_WIFI_STATE` | Initiating Wi-Fi network requests |

**No `android.permission.INTERNET`**: The application cannot access the internet, track telemetry, or send device data.

---

## How to Open and Build in Android Studio

1. Open **Android Studio** (Ladybug / Iguana or later recommended).
2. Select **Open** and choose the project root folder.
3. Allow Gradle to sync dependencies (`com.google.zxing:core`, `androidx.camera`, `androidx.room`, `androidx.biometric`).
4. Select your target device or emulator (API 24+) and click **Run (Shift + F10)**.

To run unit tests via Gradle:
```bash
gradle :app:testDebugUnitTest
```
