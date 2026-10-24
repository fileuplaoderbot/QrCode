package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.LockType
import com.example.security.SecurityManager
import com.example.viewmodel.MainViewModel

@Composable
fun LockScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lockState by viewModel.lockState.collectAsStateWithLifecycle()

    val triggerBiometrics: () -> Unit = {
        if (context is FragmentActivity && SecurityManager.canAuthenticateBiometrics(context)) {
            SecurityManager.showBiometricPrompt(
                activity = context,
                title = "Unlock OfflineQR",
                subtitle = "Authenticate using fingerprint or face",
                onSuccess = { viewModel.unlockWithBiometrics() },
                onError = { /* Let user use manual unlock */ }
            )
        }
    }

    // Auto-prompt biometrics once on start if allowed and available
    LaunchedEffect(lockState.biometricsAllowed) {
        if (lockState.biometricsAllowed) {
            triggerBiometrics()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp)
            .testTag("app_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Shield Icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when (lockState.lockType) {
                        LockType.PASSWORD -> Icons.Default.Key
                        LockType.PATTERN -> Icons.Default.GridOn
                        else -> Icons.Default.Lock
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "OfflineQR Vault Locked",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            val subtitle = when (lockState.lockType) {
                LockType.PIN -> "Enter your ${lockState.pinLength}-digit security PIN"
                LockType.PASSWORD -> "Enter your password to unlock"
                LockType.PATTERN -> "Draw your unlock pattern"
                LockType.NONE -> "Unlock OfflineQR"
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Render specific UI for the active LockType
            when (lockState.lockType) {
                LockType.PIN -> {
                    PinLockLayout(
                        targetLength = lockState.pinLength,
                        biometricsAllowed = lockState.biometricsAllowed,
                        onPinEntered = { pin ->
                            viewModel.unlockWithSecret(pin)
                        },
                        onTriggerBiometric = triggerBiometrics
                    )
                }
                LockType.PASSWORD -> {
                    PasswordLockLayout(
                        biometricsAllowed = lockState.biometricsAllowed,
                        onPasswordSubmit = { pass ->
                            viewModel.unlockWithSecret(pass)
                        },
                        onTriggerBiometric = triggerBiometrics
                    )
                }
                LockType.PATTERN -> {
                    PatternLockLayout(
                        biometricsAllowed = lockState.biometricsAllowed,
                        onPatternComplete = { pattern ->
                            viewModel.unlockWithSecret(pattern)
                        },
                        onTriggerBiometric = triggerBiometrics
                    )
                }
                LockType.NONE -> {
                    // Fallback unlock button
                    Button(onClick = { viewModel.unlockWithBiometrics() }) {
                        Text("Unlock")
                    }
                }
            }

            // Error message display
            if (lockState.errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = lockState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "100% On-Device Encryption • No Cloud Recovery",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 1. PIN Lock Layout: Dynamically adapts dots indicator to targetLength (4..8)
 */
@Composable
fun PinLockLayout(
    targetLength: Int,
    biometricsAllowed: Boolean,
    onPinEntered: (String) -> Unit,
    onTriggerBiometric: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Dynamic Dots according to set PIN length (adapts for 4..8 digits)
        val dotSize = if (targetLength > 6) 13.dp else 16.dp
        val dotSpacing = if (targetLength > 6) 10.dp else 14.dp

        Row(
            horizontalArrangement = Arrangement.spacedBy(dotSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until targetLength) {
                val isFilled = i < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .border(
                            width = 2.dp,
                            color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Keypad (1..9, 0, backspace, and biometric)
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("BIO", "0", "DEL")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (row in keys) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (key in row) {
                        when (key) {
                            "BIO" -> {
                                if (biometricsAllowed) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(CircleShape)
                                            .clickable(onClick = onTriggerBiometric)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Biometric Unlock",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(62.dp))
                                }
                            }
                            "DEL" -> {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            else -> {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            if (enteredPin.length < targetLength) {
                                                val next = enteredPin + key
                                                enteredPin = next
                                                if (next.length == targetLength) {
                                                    onPinEntered(next)
                                                    enteredPin = ""
                                                }
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. Alphabet / Alphanumeric Password Lock Layout
 */
@Composable
fun PasswordLockLayout(
    biometricsAllowed: Boolean,
    onPasswordSubmit: (String) -> Unit,
    onTriggerBiometric: () -> Unit
) {
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(0.92f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (passwordInput.isNotBlank()) {
                        onPasswordSubmit(passwordInput)
                    }
                }),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("password_unlock_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (passwordInput.isNotBlank()) {
                            onPasswordSubmit(passwordInput)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("password_unlock_button")
                ) {
                    Text("Unlock Vault")
                }

                if (biometricsAllowed) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onTriggerBiometric)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Pattern Lock Layout: Interactive 3x3 touch grid
 */
@Composable
fun PatternLockLayout(
    biometricsAllowed: Boolean,
    onPatternComplete: (String) -> Unit,
    onTriggerBiometric: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        PatternLockView(
            onPatternComplete = onPatternComplete,
            lineColor = MaterialTheme.colorScheme.primary,
            dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.testTag("pattern_lock_view")
        )

        if (biometricsAllowed) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onTriggerBiometric,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Biometrics")
            }
        }
    }
}
