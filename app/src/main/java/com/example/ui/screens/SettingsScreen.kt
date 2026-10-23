package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val pinLockEnabled by viewModel.pinLockEnabled.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()

    var showPinDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // SAF Launchers for Backup Export & Import
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackup(uri, includeSensitive = saveSensitiveHistory)
        }
    }

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportChoiceDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
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
                // PIN Lock Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App PIN Lock", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(
                            "Require salted PIN to unlock app on start",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = pinLockEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showPinDialog = true
                            } else {
                                showDisablePinDialog = true
                            }
                        },
                        modifier = Modifier.testTag("pin_lock_switch")
                    )
                }

                if (pinLockEnabled) {
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
                            }
                        )
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

    // Set PIN Dialog
    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set 4-Digit Security PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Your PIN is hashed locally using SHA-256 with a unique cryptographic salt. It is never transmitted anywhere.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        label = { Text("Enter PIN (4-6 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                    )

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 6) confirmPinInput = it },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input")
                    )

                    if (pinError != null) {
                        Text(text = pinError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length < 4) {
                            pinError = "PIN must be at least 4 digits"
                        } else if (pinInput != confirmPinInput) {
                            pinError = "PINs do not match"
                        } else {
                            viewModel.setPin(pinInput, enableBiometrics = true)
                            showPinDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_pin_button")
                ) {
                    Text("Enable PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Disable PIN Dialog
    if (showDisablePinDialog) {
        var disablePinInput by remember { mutableStateOf("") }
        var disableError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showDisablePinDialog = false },
            title = { Text("Disable PIN Lock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your current PIN to turn off the security lock:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = disablePinInput,
                        onValueChange = { disablePinInput = it },
                        label = { Text("Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    disableError?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ok = viewModel.disablePin(disablePinInput)
                        if (ok) {
                            showDisablePinDialog = false
                        } else {
                            disableError = "Incorrect PIN"
                        }
                    }
                ) {
                    Text("Disable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisablePinDialog = false }) {
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
                    Text("• Cryptographically salted SHA-256 PIN & Biometric lock.")
                    Text("• Safe intent handling without automatic link opening.")
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
