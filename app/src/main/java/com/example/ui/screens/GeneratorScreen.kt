package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.generator.QrGenerator
import com.example.viewmodel.MainViewModel
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Text", "URL", "Wi-Fi", "Phone", "Email", "SMS", "Contact")
    val tabIcons = listOf(
        Icons.Default.TextFields,
        Icons.Default.Language,
        Icons.Default.Wifi,
        Icons.Default.Call,
        Icons.Default.Email,
        Icons.Default.Sms,
        Icons.Default.Person
    )

    // Form fields
    var plainText by remember { mutableStateOf("https://github.com") }
    var urlText by remember { mutableStateOf("https://example.com") }

    // Wi-Fi fields
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPass by remember { mutableStateOf("") }
    var wifiSecurity by remember { mutableStateOf("WPA") }
    var wifiHidden by remember { mutableStateOf(false) }
    var showWifiPass by remember { mutableStateOf(false) }

    // Phone fields
    var phoneNumber by remember { mutableStateOf("") }

    // Email fields
    var emailAddress by remember { mutableStateOf("") }
    var emailSubject by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }

    // SMS fields
    var smsNumber by remember { mutableStateOf("") }
    var smsMessage by remember { mutableStateOf("") }

    // Contact fields
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactOrg by remember { mutableStateOf("") }

    // Error Correction
    var selectedEcc by remember { mutableStateOf(ErrorCorrectionLevel.M) }

    // Generated QR State
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentPayload by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    // Compute payload based on active tab
    fun computeActivePayload(): String {
        return when (selectedTabIndex) {
            0 -> plainText.trim()
            1 -> {
                val u = urlText.trim()
                if (!u.startsWith("http://") && !u.startsWith("https://") && u.isNotEmpty()) "https://$u" else u
            }
            2 -> {
                if (wifiSsid.isBlank()) ""
                else QrGenerator.buildWifiPayload(wifiSsid, wifiPass, wifiSecurity, wifiHidden)
            }
            3 -> {
                if (phoneNumber.isBlank()) "" else "tel:${phoneNumber.trim()}"
            }
            4 -> {
                if (emailAddress.isBlank()) ""
                else QrGenerator.buildEmailPayload(emailAddress.trim(), emailSubject.trim(), emailBody.trim())
            }
            5 -> {
                if (smsNumber.isBlank()) ""
                else QrGenerator.buildSmsPayload(smsNumber.trim(), smsMessage.trim())
            }
            6 -> {
                if (contactName.isBlank()) ""
                else QrGenerator.buildVCardPayload(contactName.trim(), contactPhone.trim(), contactEmail.trim(), contactOrg.trim(), "")
            }
            else -> ""
        }
    }

    // Auto-generate QR when payload or settings change
    LaunchedEffect(
        selectedTabIndex, plainText, urlText, wifiSsid, wifiPass, wifiSecurity, wifiHidden,
        phoneNumber, emailAddress, emailSubject, emailBody, smsNumber, smsMessage,
        contactName, contactPhone, contactEmail, contactOrg, selectedEcc
    ) {
        val payload = computeActivePayload()
        currentPayload = payload
        if (payload.isNotBlank()) {
            isGenerating = true
            try {
                val bmp = QrGenerator.generateQrBitmap(
                    content = payload,
                    size = 720,
                    errorCorrectionLevel = selectedEcc
                )
                generatedBitmap = bmp
            } catch (e: Exception) {
                generatedBitmap = null
            } finally {
                isGenerating = false
            }
        } else {
            generatedBitmap = null
            isGenerating = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("generator_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "QR Code Generator",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Generate high-density, offline QR codes with custom payload formats",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(imageVector = tabIcons[index], contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Form according to tab
        when (selectedTabIndex) {
            0 -> {
                OutlinedTextField(
                    value = plainText,
                    onValueChange = { plainText = it },
                    label = { Text("Plain Text Content") },
                    placeholder = { Text("Enter any text, notes, or data") },
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("generator_text_input")
                )
            }
            1 -> {
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Website Address (URL)") },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("generator_url_input")
                )
            }
            2 -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = wifiSsid,
                        onValueChange = { wifiSsid = it },
                        label = { Text("Network Name (SSID) *") },
                        placeholder = { Text("Home_WiFi") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("generator_wifi_ssid")
                    )

                    OutlinedTextField(
                        value = wifiPass,
                        onValueChange = { wifiPass = it },
                        label = { Text("Password") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showWifiPass = !showWifiPass }) {
                                Icon(
                                    imageVector = if (showWifiPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("generator_wifi_pass")
                    )

                    // Security Type Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("WPA", "WEP", "Open").forEach { sec ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (wifiSecurity == sec) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { wifiSecurity = sec }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = sec,
                                        fontWeight = if (wifiSecurity == sec) FontWeight.Bold else FontWeight.Medium,
                                        color = if (wifiSecurity == sec) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { wifiHidden = !wifiHidden }
                    ) {
                        Checkbox(checked = wifiHidden, onCheckedChange = { wifiHidden = it })
                        Text("Hidden Network", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            3 -> {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+1 (555) 000-0000") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            4 -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = emailAddress,
                        onValueChange = { emailAddress = it },
                        label = { Text("Recipient Email *") },
                        placeholder = { Text("hello@example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emailSubject,
                        onValueChange = { emailSubject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emailBody,
                        onValueChange = { emailBody = it },
                        label = { Text("Message Body") },
                        minLines = 2,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            5 -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = smsNumber,
                        onValueChange = { smsNumber = it },
                        label = { Text("Phone Number *") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = smsMessage,
                        onValueChange = { smsMessage = it },
                        label = { Text("SMS Message") },
                        minLines = 2,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            6 -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contactOrg,
                        onValueChange = { contactOrg = it },
                        label = { Text("Company / Organization") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Generated QR Card & Preview
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isGenerating) {
                    Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (generatedBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(240.dp)
                            .padding(12.dp)
                    ) {
                        Image(
                            bitmap = generatedBitmap!!.asImageBitmap(),
                            contentDescription = "Generated QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons (Save to Gallery, Share Image, Copy Text)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val uri = QrGenerator.saveBitmapToGallery(context, generatedBitmap!!, "QR_${System.currentTimeMillis()}")
                                    if (uri != null) {
                                        Toast.makeText(context, "Saved to Pictures/OfflineQR", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("save_qr_button")
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save PNG", fontSize = 13.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                coroutineScope.launch {
                                    val uri = QrGenerator.saveBitmapForSharing(context, generatedBitmap!!)
                                    if (uri != null) {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Share QR Code"))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("share_qr_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 13.sp)
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("QR Payload", currentPayload)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied payload text", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy text")
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Enter details above to generate QR",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}
