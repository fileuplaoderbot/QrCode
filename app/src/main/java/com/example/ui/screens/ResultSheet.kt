package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ScanEntity
import com.example.model.ContentType
import com.example.model.ParsedPayload
import com.example.model.WifiData
import com.example.viewmodel.MainViewModel
import com.example.wifi.WifiUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultSheet(
    payload: ParsedPayload,
    existingEntityId: Long?,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val allScans by viewModel.allScans.collectAsStateWithLifecycle()
    val currentEntity = remember(allScans, existingEntityId) {
        allScans.find { it.id == existingEntityId || it.content == payload.rawText }
    }
    val isFavorite = currentEntity?.isFavorite ?: false

    var showShareWifiDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("result_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                CategoryChip(type = payload.type)

                // Favorite Toggle
                IconButton(
                    onClick = {
                        if (currentEntity != null) {
                            viewModel.toggleFavorite(currentEntity)
                        } else {
                            // Insert first if not yet in database
                            val newEntity = ScanEntity(
                                content = payload.rawText,
                                rawFormat = payload.format,
                                contentType = payload.type.name,
                                title = payload.title,
                                timestamp = System.currentTimeMillis(),
                                isFavorite = true,
                                isSensitive = payload.isSensitive
                            )
                            viewModel.onCodeScanned(payload.rawText, payload.format)
                        }
                    },
                    modifier = Modifier.testTag("favorite_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Title
            Text(
                text = payload.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Subtitle
            if (payload.displaySubtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = payload.displaySubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Specific Card for Content Type
            when (payload.type) {
                ContentType.WIFI -> {
                    payload.wifiData?.let { wifi ->
                        WifiDetailsCard(
                            wifi = wifi,
                            onConnect = {
                                WifiUtils.requestWifiConnection(
                                    context = context,
                                    wifiData = wifi,
                                    onPromptShown = {
                                        Toast.makeText(context, "Confirm Wi-Fi connection in system dialog", Toast.LENGTH_LONG).show()
                                    },
                                    onFailed = { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            onCopyPassword = {
                                copyToClipboard(context, wifi.password, "Wi-Fi Password")
                            },
                            onCopySsid = {
                                copyToClipboard(context, wifi.ssid, "SSID")
                            },
                            onShareWithWarning = {
                                showShareWifiDialog = true
                            }
                        )
                    }
                }
                ContentType.URL -> {
                    UrlDetailsCard(url = payload.safeUrl ?: payload.rawText, context = context)
                }
                ContentType.PHONE -> {
                    PhoneDetailsCard(number = payload.phoneNumber ?: payload.rawText, context = context)
                }
                ContentType.EMAIL -> {
                    payload.emailData?.let { email ->
                        EmailDetailsCard(email = email, context = context)
                    }
                }
                ContentType.SMS -> {
                    payload.smsData?.let { sms ->
                        SmsDetailsCard(sms = sms, context = context)
                    }
                }
                ContentType.GEO -> {
                    payload.geoData?.let { geo ->
                        GeoDetailsCard(geo = geo, context = context)
                    }
                }
                ContentType.VCARD -> {
                    payload.vCardData?.let { vcard ->
                        VCardDetailsCard(vcard = vcard, context = context)
                    }
                }
                else -> {
                    RawContentCard(text = payload.rawText, format = payload.format)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Global Actions Row (Copy, Share, Scan Again)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        copyToClipboard(context, payload.rawText, "Scanned Content")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_button")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }

                FilledTonalButton(
                    onClick = {
                        shareText(context, payload.rawText)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("scan_again_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Wi-Fi Share Warning Dialog
    if (showShareWifiDialog && payload.wifiData != null) {
        val wifi = payload.wifiData
        AlertDialog(
            onDismissRequest = { showShareWifiDialog = false },
            title = { Text("Share Wi-Fi Credentials?") },
            text = {
                Text(
                    "You are about to share the Wi-Fi network name (${wifi.ssid}) along with its plain-text password. Ensure you only share with trusted recipients."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showShareWifiDialog = false
                        val shareText = "Wi-Fi Network: ${wifi.ssid}\nPassword: ${wifi.password}\nSecurity: ${wifi.securityType}"
                        shareText(context, shareText)
                    }
                ) {
                    Text("Proceed & Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareWifiDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CategoryChip(type: ContentType) {
    val (icon, color) = when (type) {
        ContentType.URL -> Icons.Default.Language to Color(0xFF0284C7)
        ContentType.WIFI -> Icons.Default.Wifi to Color(0xFF059669)
        ContentType.PHONE -> Icons.Default.Call to Color(0xFF10B981)
        ContentType.EMAIL -> Icons.Default.Email to Color(0xFF8B5CF6)
        ContentType.SMS -> Icons.Default.Sms to Color(0xFFF59E0B)
        ContentType.GEO -> Icons.Default.LocationOn to Color(0xFFEF4444)
        ContentType.VCARD -> Icons.Default.Person to Color(0xFF6366F1)
        ContentType.PRODUCT -> Icons.Default.QrCode to Color(0xFFEC4899)
        else -> Icons.Default.QrCode to MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = type.displayName,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun WifiDetailsCard(
    wifi: WifiData,
    onConnect: () -> Unit,
    onCopyPassword: () -> Unit,
    onCopySsid: () -> Unit,
    onShareWithWarning: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Network Name (SSID)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = wifi.ssid,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onCopySsid) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy SSID", modifier = Modifier.size(18.dp))
                }
            }

            if (wifi.password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Password",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (showPassword) wifi.password else "••••••••••••",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onCopyPassword) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy password", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Connect")
                }

                FilledTonalButton(
                    onClick = onShareWithWarning,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Wi-Fi")
                }
            }
        }
    }
}

@Composable
fun UrlDetailsCard(url: String, context: Context) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    openSafeUrl(context, url)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("open_link_button")
            ) {
                Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Link in Browser")
            }
        }
    }
}

@Composable
fun PhoneDetailsCard(number: String, context: Context) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = number,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call")
                }

                FilledTonalButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SMS")
                }
            }
        }
    }
}

@Composable
fun EmailDetailsCard(email: com.example.model.EmailData, context: Context) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Recipient: ${email.address}", fontWeight = FontWeight.Bold)
            if (email.subject.isNotEmpty()) {
                Text(text = "Subject: ${email.subject}", style = MaterialTheme.typography.bodySmall)
            }
            if (email.body.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = email.body, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${email.address}")
                        if (email.subject.isNotEmpty()) putExtra(Intent.EXTRA_SUBJECT, email.subject)
                        if (email.body.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, email.body)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send Email")
            }
        }
    }
}

@Composable
fun SmsDetailsCard(sms: com.example.model.SmsData, context: Context) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Number: ${sms.number}", fontWeight = FontWeight.Bold)
            if (sms.message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = sms.message, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${sms.number}")).apply {
                        if (sms.message.isNotEmpty()) putExtra("sms_body", sms.message)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compose SMS")
            }
        }
    }
}

@Composable
fun GeoDetailsCard(geo: com.example.model.GeoData, context: Context) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Coordinates: ${geo.latitude}, ${geo.longitude}", fontWeight = FontWeight.Bold)
            if (geo.query.isNotEmpty()) {
                Text(text = "Query: ${geo.query}", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val uri = Uri.parse("geo:${geo.latitude},${geo.longitude}?q=${geo.latitude},${geo.longitude}(${geo.query})")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open in Maps")
            }
        }
    }
}

@Composable
fun VCardDetailsCard(vcard: com.example.model.VCardData, context: Context) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = vcard.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (vcard.organization.isNotEmpty()) Text(text = vcard.organization, style = MaterialTheme.typography.bodySmall)
            if (vcard.phone.isNotEmpty()) Text(text = "Phone: ${vcard.phone}", style = MaterialTheme.typography.bodyMedium)
            if (vcard.email.isNotEmpty()) Text(text = "Email: ${vcard.email}", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        type = ContactsContract.RawContacts.CONTENT_TYPE
                        putExtra(ContactsContract.Intents.Insert.NAME, vcard.fullName)
                        if (vcard.phone.isNotEmpty()) putExtra(ContactsContract.Intents.Insert.PHONE, vcard.phone)
                        if (vcard.email.isNotEmpty()) putExtra(ContactsContract.Intents.Insert.EMAIL, vcard.email)
                        if (vcard.organization.isNotEmpty()) putExtra(ContactsContract.Intents.Insert.COMPANY, vcard.organization)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add to Contacts")
            }
        }
    }
}

@Composable
fun RawContentCard(text: String, format: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Format: $format",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share via").apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(shareIntent)
}

private fun openSafeUrl(context: Context, url: String) {
    if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
        Toast.makeText(context, "Blocked unsafe link format", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No web browser app available", Toast.LENGTH_SHORT).show()
    }
}
