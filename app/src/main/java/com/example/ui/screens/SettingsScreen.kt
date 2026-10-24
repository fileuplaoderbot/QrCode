package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.LockType
import com.example.security.SecurityManager
import com.example.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val vibrateOnScan by viewModel.vibrateOnScan.collectAsStateWithLifecycle()
    val preventDuplicates by viewModel.preventDuplicates.collectAsStateWithLifecycle()
    val saveSensitiveHistory by viewModel.saveSensitiveHistory.collectAsStateWithLifecycle()
    val lockType by viewModel.lockType.collectAsStateWithLifecycle()
    val pinLength by viewModel.pinLength.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()

    var showSecuritySetupDialog by remember { mutableStateOf(false) }
    var showDisableSecurityDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // SAF Launchers for Backup Export & Import
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        viewModel.setAwaitingActivityResult(false)
        if (uri != null) {
            viewModel.exportBackup(uri, includeSensitive = saveSensitiveHistory)
        }
    }

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportChoiceDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        viewModel.setAwaitingActivityResult(false)
        if (uri != null) {
            pendingImportUri = uri
            showImportChoiceDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Security, Offline Storage, Haptics & Appearance",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Privacy & Security Card
        Text(
            text = "PRIVACY & SECURITY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Security Lock Section
                if (lockType == LockType.NONE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vault Lock Protection", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                "Choose Number PIN (4-8 digits), Password, or Pattern lock",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showSecuritySetupDialog = true },
                            modifier = Modifier.testTag("setup_lock_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set Up Lock")
                        }
                    }
                } else {
                    // Active Lock Information
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val icon = when (lockType) {
                                        LockType.PIN -> Icons.Default.Numbers
                                        LockType.PASSWORD -> Icons.Default.Key
                                        LockType.PATTERN -> Icons.Default.GridOn
                                        LockType.NONE -> Icons.Default.LockOpen
                                    }
                                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                val title = when (lockType) {
                                    LockType.PIN -> "$pinLength-Digit Number PIN"
                                    LockType.PASSWORD -> "Alphanumeric Password"
                                    LockType.PATTERN -> "3x3 Pattern Lock"
                                    LockType.NONE -> "Unlocked"
                                }
                                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(
                                    "Salted SHA-256 local hash active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { showSecuritySetupDialog = true },
                                modifier = Modifier.testTag("change_lock_button")
                            ) {
                                Text("Change")
                            }
                            FilledTonalButton(
                                onClick = { showDisableSecurityDialog = true },
                                modifier = Modifier.testTag("disable_lock_button")
                            ) {
                                Text("Turn Off")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Biometrics toggle if supported
                    val hasBiometricHardware = remember { SecurityManager.canAuthenticateBiometrics(context) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric Unlock", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(
                                if (hasBiometricHardware) "Unlock with fingerprint or face" else "Biometrics not available on device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = biometricEnabled,
                            enabled = hasBiometricHardware,
                            onCheckedChange = { enabled ->
                                viewModel.setBiometricEnabled(enabled)
                            },
                            modifier = Modifier.testTag("biometric_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Instant Test Lock button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.lockAppNow() }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Test Lock Screen Now", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("Lock immediately to verify unlock credentials", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Scanner Behavior Card
        Text(
            text = "SCANNER & HISTORY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Vibration on scan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vibrate on Scan", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            "Subtle haptic feedback on successful decode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = vibrateOnScan,
                        onCheckedChange = { viewModel.setVibrateOnScan(it) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                // Duplicate prevention
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Prevent Duplicate Scans", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            "Avoid saving repeated codes during same session",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preventDuplicates,
                        onCheckedChange = { viewModel.setPreventDuplicates(it) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                // Save Sensitive
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Save Sensitive Scans", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            "Store Wi-Fi credentials in local history",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = saveSensitiveHistory,
                        onCheckedChange = { viewModel.setSaveSensitiveHistory(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Appearance & Theme
        Text(
            text = "APPEARANCE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showThemeDialog = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Theme Mode", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        text = when (themeMode) {
                            "light" -> "Light Theme"
                            "dark" -> "Dark Theme"
                            else -> "Follow System Default"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(imageVector = Icons.Default.Brightness4, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Offline Backup & Restore
        Text(
            text = "BACKUP & RESTORE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.setAwaitingActivityResult(true)
                            exportLauncher.launch("OfflineQR_Backup_${System.currentTimeMillis()}.json")
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Export History to JSON", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            "Save encrypted/local records to a JSON file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.setAwaitingActivityResult(true)
                            importLauncher.launch(arrayOf("application/json"))
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Import Backup from JSON", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            "Restore scans from an existing backup file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(imageVector = Icons.Default.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // About Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAboutDialog = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("About OfflineQR", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Version 1.0.0 • Strict Zero-Network Privacy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    // Comprehensive Security Setup Dialog with 3 Lock Types
    if (showSecuritySetupDialog) {
        val hasBiometrics = remember { SecurityManager.canAuthenticateBiometrics(context) }
        var selectedTab by remember { mutableIntStateOf(0) } // 0 = PIN, 1 = Password, 2 = Pattern
        var enableBio by remember { mutableStateOf(hasBiometrics) }

        // PIN state
        var pinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        // Password state
        var passwordInput by remember { mutableStateOf("") }
        var confirmPasswordInput by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var passwordError by remember { mutableStateOf<String?>(null) }

        // Pattern state
        var patternStep by remember { mutableIntStateOf(0) } // 0 = first draw, 1 = confirm draw
        var firstPattern by remember { mutableStateOf<String?>(null) }
        var secondPattern by remember { mutableStateOf<String?>(null) }
        var currentDrawnPattern by remember { mutableStateOf("") }
        var patternClearCounter by remember { mutableIntStateOf(0) }
        var patternError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSecuritySetupDialog = false },
            title = {
                Text("Set Up Security Lock", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Protect your scanned history and Wi-Fi credentials with local cryptographic hashing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 3 Types Tab Row
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("PIN", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.Numbers, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Password", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Pattern", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    when (selectedTab) {
                        // 1. NUMBER PIN TAB (4 to 8 Digits)
                        0 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Enter a numeric PIN from 4 to 8 digits. On opening the app, exactly this number of dots will appear on the unlock screen.",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = {
                                        val digitsOnly = it.filter { ch -> ch.isDigit() }
                                        if (digitsOnly.length <= 8) {
                                            pinInput = digitsOnly
                                            pinError = null
                                        }
                                    },
                                    label = { Text("New PIN (4 to 8 digits)") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("setup_pin_input")
                                )

                                // Live dynamic dots preview
                                if (pinInput.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Length preview (${pinInput.length} dots): ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            for (i in 0 until pinInput.length) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary)
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = confirmPinInput,
                                    onValueChange = {
                                        val digitsOnly = it.filter { ch -> ch.isDigit() }
                                        if (digitsOnly.length <= 8) {
                                            confirmPinInput = digitsOnly
                                            pinError = null
                                        }
                                    },
                                    label = { Text("Confirm PIN") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("setup_confirm_pin_input")
                                )

                                pinError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        // 2. ALPHANUMERIC PASSWORD TAB
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Enter an alphanumeric text password (minimum 4 characters). You can include letters, numbers, and symbols.",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = {
                                        passwordInput = it
                                        passwordError = null
                                    },
                                    label = { Text("New Password") },
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("setup_password_input")
                                )

                                OutlinedTextField(
                                    value = confirmPasswordInput,
                                    onValueChange = {
                                        confirmPasswordInput = it
                                        passwordError = null
                                    },
                                    label = { Text("Confirm Password") },
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth().testTag("setup_confirm_password_input")
                                )

                                passwordError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        // 3. PATTERN LOCK TAB
                        2 -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val stepTitle = if (patternStep == 0) "Step 1 of 2: Draw or Tap Pattern" else "Step 2 of 2: Confirm Pattern"
                                val stepSubtitle = if (patternStep == 0) {
                                    "Connect at least 4 dots by dragging or tapping dots"
                                } else {
                                    "Redraw the exact same pattern to confirm"
                                }

                                Text(stepTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                Text(stepSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                PatternLockView(
                                    onPatternComplete = { pattern ->
                                        currentDrawnPattern = pattern
                                        patternError = null
                                    },
                                    clearTrigger = patternClearCounter to patternStep,
                                    modifier = Modifier.size(230.dp)
                                )

                                val dotCount = if (currentDrawnPattern.isNotBlank()) currentDrawnPattern.split(",").size else 0
                                Text(
                                    text = if (dotCount > 0) "$dotCount dots connected" else "Touch or drag to connect dots",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (dotCount >= 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            currentDrawnPattern = ""
                                            patternClearCounter++
                                            patternError = null
                                        }
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear")
                                    }

                                    if (patternStep == 0) {
                                        Button(
                                            onClick = {
                                                if (dotCount < 4) {
                                                    patternError = "Please connect at least 4 dots"
                                                } else {
                                                    firstPattern = currentDrawnPattern
                                                    currentDrawnPattern = ""
                                                    patternStep = 1
                                                    patternClearCounter++ // Clears previous drawn pattern completely!
                                                    patternError = null
                                                }
                                            },
                                            enabled = dotCount >= 4
                                        ) {
                                            Text("Next: Confirm")
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                if (currentDrawnPattern != firstPattern) {
                                                    patternError = "Pattern does not match. Please redraw to confirm."
                                                    currentDrawnPattern = ""
                                                    patternClearCounter++
                                                } else {
                                                    secondPattern = currentDrawnPattern
                                                    patternError = null
                                                    viewModel.setLock(
                                                        type = LockType.PATTERN,
                                                        secret = firstPattern!!,
                                                        length = firstPattern!!.split(",").size,
                                                        enableBiometrics = enableBio
                                                    )
                                                    showSecuritySetupDialog = false
                                                }
                                            },
                                            enabled = dotCount >= 4
                                        ) {
                                            Text("Confirm Pattern")
                                        }
                                    }
                                }

                                if (patternStep == 1) {
                                    TextButton(
                                        onClick = {
                                            patternStep = 0
                                            firstPattern = null
                                            secondPattern = null
                                            currentDrawnPattern = ""
                                            patternClearCounter++
                                            patternError = null
                                        }
                                    ) {
                                        Text("Start Over from Step 1")
                                    }
                                }

                                patternError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }

                    // Biometrics toggle checkbox inside setup dialog
                    if (hasBiometrics) {
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { enableBio = !enableBio }
                        ) {
                            Checkbox(
                                checked = enableBio,
                                onCheckedChange = { enableBio = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Enable Biometrics", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("Fingerprint or Face unlock", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (selectedTab) {
                            0 -> {
                                if (pinInput.length < 4 || pinInput.length > 8) {
                                    pinError = "PIN must be between 4 and 8 digits"
                                } else if (pinInput != confirmPinInput) {
                                    pinError = "PINs do not match"
                                } else {
                                    viewModel.setLock(
                                        type = LockType.PIN,
                                        secret = pinInput,
                                        length = pinInput.length,
                                        enableBiometrics = enableBio
                                    )
                                    showSecuritySetupDialog = false
                                }
                            }
                            1 -> {
                                if (passwordInput.length < 4) {
                                    passwordError = "Password must be at least 4 characters"
                                } else if (passwordInput != confirmPasswordInput) {
                                    passwordError = "Passwords do not match"
                                } else {
                                    viewModel.setLock(
                                        type = LockType.PASSWORD,
                                        secret = passwordInput,
                                        length = passwordInput.length,
                                        enableBiometrics = enableBio
                                    )
                                    showSecuritySetupDialog = false
                                }
                            }
                            2 -> {
                                val matchPattern = secondPattern ?: if (patternStep == 1 && currentDrawnPattern == firstPattern) currentDrawnPattern else null
                                if (firstPattern == null || matchPattern == null) {
                                    patternError = "Please complete Step 1 and confirm your pattern in Step 2"
                                } else if (firstPattern != matchPattern) {
                                    patternError = "Patterns do not match"
                                } else {
                                    viewModel.setLock(
                                        type = LockType.PATTERN,
                                        secret = firstPattern!!,
                                        length = firstPattern!!.split(",").size,
                                        enableBiometrics = enableBio
                                    )
                                    showSecuritySetupDialog = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_security_button")
                ) {
                    Text("Save & Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecuritySetupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Disable Security Dialog (prompts for current credential)
    if (showDisableSecurityDialog) {
        var enteredSecret by remember { mutableStateOf("") }
        var disableError by remember { mutableStateOf<String?>(null) }
        var patternInput by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showDisableSecurityDialog = false },
            title = { Text("Disable Vault Security") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Please verify your current credential to disable app security:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    when (lockType) {
                        LockType.PIN -> {
                            OutlinedTextField(
                                value = enteredSecret,
                                onValueChange = { enteredSecret = it },
                                label = { Text("Current $pinLength-Digit PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("disable_pin_input")
                            )
                        }
                        LockType.PASSWORD -> {
                            OutlinedTextField(
                                value = enteredSecret,
                                onValueChange = { enteredSecret = it },
                                label = { Text("Current Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("disable_password_input")
                            )
                        }
                        LockType.PATTERN -> {
                            Text("Draw your current pattern:", style = MaterialTheme.typography.labelMedium)
                            PatternLockView(
                                onPatternComplete = { pattern ->
                                    patternInput = pattern
                                },
                                modifier = Modifier.size(200.dp)
                            )
                        }
                        LockType.NONE -> {}
                    }

                    disableError?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toCheck = if (lockType == LockType.PATTERN) (patternInput ?: "") else enteredSecret
                        viewModel.disableSecurity(toCheck) { ok ->
                            if (ok) {
                                showDisableSecurityDialog = false
                            } else {
                                disableError = "Incorrect credential. Please try again."
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_disable_security_button")
                ) {
                    Text("Disable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableSecurityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme") },
            text = {
                Column {
                    listOf("system" to "Follow System", "light" to "Light Mode", "dark" to "Dark Mode").forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(code)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontWeight = if (themeMode == code) FontWeight.Bold else FontWeight.Normal)
                            if (themeMode == code) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Option Choice Dialog (Merge or Replace)
    if (showImportChoiceDialog && pendingImportUri != null) {
        val uri = pendingImportUri!!
        AlertDialog(
            onDismissRequest = { showImportChoiceDialog = false },
            title = { Text("Import Backup") },
            text = {
                Text("Choose whether to merge imported items with your existing history, or replace your existing history entirely:")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportChoiceDialog = false
                        viewModel.importBackup(uri, replaceExisting = false)
                    }
                ) {
                    Text("Merge")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportChoiceDialog = false
                        viewModel.importBackup(uri, replaceExisting = true)
                    }
                ) {
                    Text("Replace All", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("OfflineQR v1.0") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Zero internet permissions in AndroidManifest.xml.")
                    Text("• Pure on-device QR & barcode scanning and generation.")
                    Text("• Local SQLite / Room database storage.")
                    Text("• 3 Lock Types: Number PIN (4-8 digits), Password & Pattern.")
                    Text("• Dynamic PIN dots matching exact configured length.")
                    Text("• Safe gallery photo decoding with EXIF & multi-format support.")
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}
