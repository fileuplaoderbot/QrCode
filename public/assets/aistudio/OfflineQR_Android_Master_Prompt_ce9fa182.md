# MASTER PROMPT — OfflineQR Native Android App

You are an expert Android application developer specializing in Kotlin, native Android SDK development, offline-first architecture, camera scanning, QR code generation, privacy, and modern Android UI design.

Build a complete, production-quality native Android application named **OfflineQR**.

The app must be developed using **Kotlin and the native Android SDK**. Do not create a website, web app, HTML app, or hybrid application.

## 1. Project Identity

- **App name:** OfflineQR
- **Package name / Application ID:** `com.offlineqr.scanner`
- **Platform:** Android
- **Programming language:** Kotlin
- **UI:** Native Android UI using XML layouts and Kotlin, or fully native Jetpack Compose if supported consistently.
- **Build system:** Gradle with Kotlin DSL where supported.
- **Minimum SDK:** Android 7.0 (API 24), unless a required feature makes this impractical.
- **Target SDK:** A recent stable Android SDK compatible with the build environment.
- **Architecture:** Simple, maintainable, modular, and beginner-friendly.
- **Primary requirement:** Core app features must work completely offline after installation.
- **No WebView-based UI.**
- **No HTML, CSS, or JavaScript implementation.**

Use stable dependency versions compatible with the chosen Android SDK and Gradle setup. Do not invent dependency versions or use experimental APIs unnecessarily.

## 2. Mandatory Google AI Studio Instructions

1. Inspect the available project files and determine the actual Android project structure and supported build environment.
2. Create or update real project files, not merely example snippets or pseudocode.
3. If you can edit files in the workspace, implement the application directly in those files.
4. If the environment cannot build or run a native Android project, state that limitation honestly and provide all required project files and exact paths.
5. Never claim the app builds, runs, or has been tested unless you verified it.
6. Do not leave TODOs, placeholder screens, fake buttons, empty functions, or unimplemented core features.
7. Keep Kotlin files, XML resources, manifest, Gradle configuration, database models, and resource references consistent.
8. Ensure imports, class names, resource references, permissions, and package declarations match.
9. Prefer native Android APIs and stable libraries over unnecessary dependencies.
10. If an API is unavailable on the minimum Android version, implement a safe version-aware alternative.
11. Do not silently remove difficult features. Explain genuine platform limitations and implement the best supported alternative.
12. Make reasonable engineering decisions without repeatedly asking me to choose minor details.

## 3. Offline-First Privacy Requirements

The app must not require an internet connection for its core functionality.

- Do not request the `INTERNET` permission.
- Do not add network requests, analytics, advertisements, telemetry, remote APIs, cloud accounts, or remote configuration.
- Do not load scripts, stylesheets, libraries, images, fonts, or assets from the internet at runtime.
- Do not upload scans, generated QR codes, passwords, history, favorites, or backups.
- Store application data locally on the device.
- Use local Android storage, SQLite/Room if compatible, and DataStore or SharedPreferences for appropriate settings.
- Process imported files locally.
- Keep backups under the user's control.
- Never log QR contents, Wi-Fi passwords, PINs, or sensitive scan results.
- Request only permissions needed for features the user activates.
- Avoid broad storage access when Android's Storage Access Framework can be used.
- Do not request location permission merely to scan or generate QR codes.
- Clearly explain any Android system requirement that cannot be bypassed.

Never falsely claim a feature is fully offline if it depends on a network connection or external service.

## 4. Required Features

### Feature 1: Instant QR and Barcode Scanner

Implement a functional camera-based scanner using a maintained, compatible scanning library or suitable native Android APIs.

- Open the rear camera by default.
- Display a clear scanning frame and subtle animated scan line.
- Support QR codes and common barcode formats, including EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39, Code 93, Codabar, ITF, and Data Matrix where supported by the scanning engine.
- Automatically detect and process visible codes.
- Prevent repeated callbacks from the same code during a single scan session.
- Provide torch control when supported.
- Allow switching cameras if a suitable front camera exists.
- Support autofocus and suitable camera resolution.
- Handle permission denial, camera initialization failures, and unavailable cameras gracefully.
- Stop camera processing when the scanner screen is paused or closed.
- Release camera resources correctly.
- Avoid blocking the UI thread.
- Display scan results immediately.
- Provide a manual retry option.
- Vibrate briefly after a successful scan if enabled and supported.

Prefer an established offline scanning library such as ZXing if it offers reliable support for the chosen setup. Pin compatible dependency versions in Gradle. Do not claim a scanning library is built into Android unless it actually is.

### Feature 2: Smart Scan Result Preview

Recognize at least:

- Website URLs
- Plain text
- Phone numbers
- Email addresses
- SMS links
- Wi-Fi QR codes
- Geographic coordinates and map links
- Contact/vCard data
- Calendar event data when recognizable
- Common application links

Show content type, scanned content, timestamp, Copy, Share, Open Link (for valid URLs), Save to Favorites, and Scan Again actions.

Security requirements:

- Never automatically open a URL after scanning.
- Require the user to tap Open Link.
- Validate URL schemes and reject dangerous schemes such as `javascript:`, `file:`, and unsupported intent URLs.
- Open only suitable schemes through safe Android intent handling.
- Handle cases where no compatible app exists.
- Avoid exposing sensitive information unnecessarily.
- Never execute scanned content as code.

Offer phone, email, and other supported Android intents only after explicit user action.

### Feature 3: Scan History and Favorites

- Store scan content, type, timestamp, and favorite status locally.
- Display newest scans first.
- Add search and content-type filters.
- Allow marking/unmarking favorites.
- Allow copying and sharing saved results.
- Allow deleting individual items.
- Provide a clear-history option with confirmation.
- Provide a Favorites screen.
- Persist records across app restarts.
- Avoid duplicate history entries when duplicate detection is enabled.
- Let users choose whether sensitive results such as Wi-Fi credentials are saved.
- Keep database operations off the main thread.

Use a reliable local persistence solution. If Room is selected, configure all required plugins, dependencies, entities, DAOs, and database classes correctly.

### Feature 4: QR Code Generator

Support QR generation for plain text, URLs, Wi-Fi networks, phone numbers, email addresses, SMS messages, contact information, and other suitable text-based data.

- Provide clear forms for each supported type.
- Validate input.
- Allow choosing QR size where practical.
- Provide error-correction options if supported.
- Display generated QR codes clearly.
- Allow saving the QR image to a user-selected location.
- Allow sharing the image through Android's Sharesheet.
- Allow copying the underlying text and generating another QR.
- Use local QR encoding only; never upload generated content.
- Correctly escape special characters in Wi-Fi QR payloads.
- Explain limitations for unsupported security types instead of producing invalid data silently.

### Feature 5: Scan QR Codes from Gallery

Use Android's Photo Picker or Storage Access Framework.

- Decode QR codes from selected images.
- Support supported image formats and orientations.
- Handle large images without excessive memory usage.
- Resize/process images efficiently when needed.
- Display results through the same Smart Result Preview.
- Handle cancellation and invalid-image errors.
- Avoid unnecessary broad photo/storage permissions.
- Process images locally without uploading them.

If the selected library does not decode image files directly, implement a local bitmap decoding pipeline.

### Feature 6: Wi-Fi QR Reader

Recognize standard Wi-Fi payloads, such as:

`WIFI:T:WPA;S:NetworkName;P:NetworkPassword;;`

Display available SSID, security type, password, and hidden-network status.

- Add Show/Hide Password.
- Add Copy SSID and Copy Password.
- Add Share with a warning before sharing credentials.
- Handle open networks, missing passwords, escaped characters, and malformed payloads.
- Never log Wi-Fi passwords.
- Never automatically join a network simply because it was scanned.

Provide a Connect option where Android permits it. Wi-Fi connection APIs vary by OS version and device; use supported network request/specifier or suggestion mechanisms as appropriate.

- Do not promise silent connection on modern Android.
- Explain when Android requires system approval or user interaction.
- Handle permission requirements and API restrictions accurately.
- Do not request location permission unless genuinely required by the chosen API and OS version.
- Do not rely exclusively on deprecated APIs.
- If direct connection is unavailable, show a clear alternative and preserve decoded network details.

### Feature 7: Optional Privacy Lock

- Allow enabling/disabling the lock in Settings.
- Support a locally configured PIN with confirmation when creating/changing it.
- Store a salted, securely derived PIN verifier, not a plaintext PIN.
- Show a lock screen on resume according to the selected policy.
- Never log PINs or store them as plaintext in ordinary preferences.
- Use Android-supported biometric APIs where available.
- Allow biometric unlocking when supported, with PIN fallback.
- Do not claim hardware-backed security unless configured and verified.
- Explain that a forgotten PIN may require clearing app data if no recovery mechanism exists.

Use a compatible AndroidX biometric dependency if needed. Do not invent biometric APIs or bypass Android security.

### Feature 8: Light, Dark, and System Themes

Provide Light, Dark, and Follow System options.

- Add a theme selector in Settings.
- Persist the selection locally.
- Apply it consistently to every screen.
- Ensure readable contrast.
- Style dialogs, buttons, inputs, lists, results, scanner overlays, and navigation consistently.
- Avoid flashing the wrong theme during startup.
- Use native theme resources or a consistent Compose Material implementation.

### Feature 9: Vibration Feedback

- Add an enable/disable setting.
- Use short, subtle vibration after successful scans.
- Respect user preference.
- Check API availability and required permissions.
- Avoid repeated vibration for the same scan.
- Handle devices without vibration hardware.

### Feature 10: Duplicate Detection

- Identify repeated scan content using normalized content or a stable hash.
- Avoid unnecessary duplicate history entries during the same session.
- Let users choose whether previously scanned content should be saved again.
- Show a subtle duplicate-detected message.
- Do not merge different codes merely because their visible labels match.
- Preserve original and latest scan timestamps if useful.
- Keep detection local.

### Feature 11: Offline Backup, Export, and Import

- Export scan history, favorites, and relevant preferences to JSON.
- Use Android's Storage Access Framework to select the destination.
- Allow importing previously exported backups.
- Validate imported JSON before modifying stored data.
- Handle malformed, incomplete, oversized, and incompatible backups safely.
- Confirm before replacing or merging existing history.
- Offer merge and replace options where practical.
- Exclude PINs, authentication secrets, and raw security-sensitive credentials by default.
- Clearly tell users what will be exported.
- Prevent path traversal and unsafe file handling.
- Never execute imported content.
- Keep all backup operations local.

## 5. User Interface and Navigation

Create a polished, modern, minimal native Android interface.

The home screen should include app branding, prominent Scan QR, Generate QR, Scan from Gallery, recent scans, quick access to Favorites, History, and Settings.

Create these screens or equivalent native navigation destinations:

1. Home
2. Live Scanner
3. Scan Result
4. Scan History
5. Favorites
6. QR Generator
7. Gallery Picker / Gallery Scan Result
8. Wi-Fi QR Details
9. Settings
10. Privacy Lock
11. Backup and Restore
12. About

UI requirements:

- Responsive layouts for different Android screen sizes.
- Native touch interactions.
- Loading, empty, success, and error states.
- Clear navigation and back-button handling.
- Accessible labels and suitable touch target sizes.
- Readable text and keyboard-aware forms.
- No fake buttons or dead-end screens.
- No WebView-based interface.
- No internet-dependent visual assets.
- Use vector drawables, native icons, or bundled assets.
- Maintain a consistent color palette and typography.

Choose either Jetpack Compose or XML layouts as the primary UI approach and implement it consistently. If using Compose, implement all screens natively. If using XML, create the actual XML resources and connect them to Kotlin code. Do not mix approaches randomly.

## 6. Permissions and Android Manifest

Create a correct `AndroidManifest.xml`. Request permissions only when needed. Potential permissions may include `android.permission.CAMERA`, the appropriate vibration permission if required, and suitable Wi-Fi-related permissions for supported Android versions and the connection workflow.

Rules:

- Do not add `android.permission.INTERNET`.
- Avoid broad file-system access.
- Do not request location without a genuine API-level requirement.
- Request camera permission at runtime.
- Explain permissions when appropriate.
- Handle denial gracefully without crashing.
- Configure exported components and intent filters safely.
- Do not expose internal components unnecessarily.
- Review every manifest permission against the actual implementation.

## 7. Recommended Project Structure

Create a maintainable structure similar to this, adapting filenames where appropriate while ensuring all required files exist:

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `gradle/libs.versions.toml` if using a version catalog
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/offlineqr/scanner/MainActivity.kt`
- `OfflineQRApplication.kt` if needed
- `navigation/`
- `ui/`
- `ui/screens/`
- `ui/components/`
- `ui/theme/`
- `scanner/`
- `generator/`
- `decoder/`
- `wifi/`
- `history/`
- `data/`
- `data/local/`
- `data/repository/`
- `security/`
- `backup/`
- `settings/`
- `utils/`

For XML-based UI, create required resources under:

- `app/src/main/res/layout/`
- `app/src/main/res/drawable/`
- `app/src/main/res/mipmap/`
- `app/src/main/res/values/`
- `app/src/main/res/values-night/`

Create additional resources as needed. Do not create empty folders or files just to imitate the structure; implement actual functionality.

## 8. Dependency and Compatibility Rules

- Use Kotlin and Android SDK APIs.
- Use stable Gradle-compatible dependencies.
- Use a proven QR encoder and decoder.
- Use CameraX only if dependencies and SDK compatibility are correctly configured; otherwise choose a suitable supported camera implementation.
- Use Room only if its Gradle configuration and generated code are correctly set up.
- Use AndroidX when needed for compatibility, navigation, lifecycle, or biometrics.
- Do not assume AndroidX is prohibited.
- Avoid unnecessary dependencies and outdated libraries.
- Verify dependency declarations and plugin versions.
- Core features must work without internet after required libraries have been installed during the build process.

Clarify the difference between **offline runtime operation** and **offline building**: Gradle may need to download dependencies on the first build unless already cached.

## 9. Performance and Reliability

- Do not run expensive image processing or database operations on the main thread.
- Avoid memory leaks and camera resource leaks.
- Handle Activity/Fragment/Compose lifecycle correctly.
- Prevent duplicate scan callbacks and unnecessary camera initialization.
- Handle configuration changes and process recreation.
- Use stable database record IDs.
- Validate input.
- Handle exceptions without crashing.
- Do not swallow exceptions silently; use appropriate recovery and non-sensitive logging.
- Never log sensitive QR or Wi-Fi content.
- Keep the APK reasonably lightweight.
- Avoid unnecessary background services.
- Do not run continuously in the background without a user-visible reason.
- Keep navigation and back handling consistent.

## 10. Testing and Verification

Implement tests where feasible, including tests for:

- Wi-Fi QR payload parsing
- Escaped Wi-Fi credentials
- QR content type detection
- URL validation
- Duplicate detection
- History persistence
- Backup serialization and validation
- PIN verification logic
- QR generator input validation

Verify manually or through available automated tools:

1. Project sync succeeds.
2. Kotlin source files compile.
3. Resource references resolve.
4. Manifest is valid.
5. App launches.
6. Camera permission handling works.
7. QR scanning works on a compatible device or emulator with a camera.
8. Gallery decoding works.
9. QR generation works.
10. History and favorites persist after restart.
11. Theme selection persists.
12. Backup export and import work.
13. PIN lock works when enabled.
14. Core functionality works in airplane mode.
15. No internet permission or runtime network dependency is present.

If no real device or emulator is available, clearly separate completed build verification from tests requiring a device.

## 11. Required Deliverables

Deliver a complete native Android project, not just a design or demonstration.

Include:

1. All required Gradle configuration files.
2. Android manifest.
3. All Kotlin source files.
4. All required XML layouts or Compose UI implementation.
5. Theme and drawable resources.
6. Database entities, DAOs, and repositories if applicable.
7. QR scanning and generation implementation.
8. Wi-Fi QR parsing and supported connection workflow.
9. History and favorites.
10. Gallery scanning.
11. Privacy lock.
12. Local backup and restore.
13. Unit tests where practical.
14. README with build instructions, feature descriptions, permissions, and known Android limitations.
15. Exact steps to open and build in Android Studio.
16. An APK only if the environment actually supports building one.

Do not output only a high-level plan. Start implementing the real project. If a build tool is available, build the project and fix compilation errors. If unavailable, provide complete files and state clearly that the build has not been verified.

## 12. Final Quality Checklist

Before declaring the project complete, confirm:

- [ ] Native Kotlin Android app, not a website.
- [ ] Application ID is `com.offlineqr.scanner`.
- [ ] Core functionality requires no internet connection.
- [ ] No `INTERNET` permission.
- [ ] QR and barcode scanner is implemented.
- [ ] Smart result preview is implemented.
- [ ] History and favorites persist locally.
- [ ] QR generator works.
- [ ] Gallery scanning works.
- [ ] Wi-Fi QR parsing works.
- [ ] Wi-Fi connection respects Android restrictions.
- [ ] Optional PIN and biometric lock are implemented appropriately.
- [ ] Light, dark, and system themes work.
- [ ] Vibration settings work.
- [ ] Duplicate detection works.
- [ ] Backup export and import work.
- [ ] Permissions are requested only when necessary.
- [ ] No core feature is represented by a fake or placeholder button.
- [ ] Project files and dependencies are consistent.
- [ ] Build and testing claims reflect actual verification.

**Now inspect the project environment and begin building OfflineQR as a complete, native Kotlin Android application using the Android SDK. Prioritize correctness, offline privacy, working features, Android compatibility, and a polished user experience.**
