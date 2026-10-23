package com.example.model

import java.net.URI
import java.util.Locale

enum class ContentType(val displayName: String) {
    URL("Website URL"),
    WIFI("Wi-Fi Network"),
    PHONE("Phone Number"),
    EMAIL("Email Address"),
    SMS("SMS Message"),
    GEO("Location / Coordinates"),
    VCARD("Contact Card"),
    EVENT("Calendar Event"),
    PRODUCT("Barcode / Product"),
    TEXT("Plain Text")
}

data class WifiData(
    val ssid: String,
    val password: String = "",
    val securityType: String = "WPA", // WPA, WEP, nopass
    val isHidden: Boolean = false
)

data class VCardData(
    val fullName: String,
    val phone: String = "",
    val email: String = "",
    val organization: String = "",
    val title: String = ""
)

data class GeoData(
    val latitude: Double,
    val longitude: Double,
    val query: String = ""
)

data class EmailData(
    val address: String,
    val subject: String = "",
    val body: String = ""
)

data class SmsData(
    val number: String,
    val message: String = ""
)

data class ParsedPayload(
    val rawText: String,
    val format: String,
    val type: ContentType,
    val title: String,
    val displaySubtitle: String,
    val wifiData: WifiData? = null,
    val vCardData: VCardData? = null,
    val geoData: GeoData? = null,
    val emailData: EmailData? = null,
    val smsData: SmsData? = null,
    val safeUrl: String? = null,
    val phoneNumber: String? = null,
    val isSensitive: Boolean = false
) {
    companion object {
        fun parse(rawText: String, format: String = "QR_CODE"): ParsedPayload {
            val trimmed = rawText.trim()

            // 1. Wi-Fi
            if (trimmed.startsWith("WIFI:", ignoreCase = true) || trimmed.startsWith("wifi:", ignoreCase = true)) {
                val wifi = parseWifiPayload(trimmed)
                return ParsedPayload(
                    rawText = rawText,
                    format = format,
                    type = ContentType.WIFI,
                    title = if (wifi.ssid.isNotEmpty()) wifi.ssid else "Wi-Fi Network",
                    displaySubtitle = "Security: ${wifi.securityType} • ${if (wifi.password.isNotEmpty()) "Password Protected" else "Open"}",
                    wifiData = wifi,
                    isSensitive = true
                )
            }

            // 2. vCard Contact
            if (trimmed.contains("BEGIN:VCARD", ignoreCase = true)) {
                val vcard = parseVCard(trimmed)
                return ParsedPayload(
                    rawText = rawText,
                    format = format,
                    type = ContentType.VCARD,
                    title = if (vcard.fullName.isNotEmpty()) vcard.fullName else "Contact Card",
                    displaySubtitle = listOfNotNull(
                        vcard.phone.takeIf { it.isNotEmpty() },
                        vcard.email.takeIf { it.isNotEmpty() },
                        vcard.organization.takeIf { it.isNotEmpty() }
                    ).joinToString(" • ").ifEmpty { "vCard Info" },
                    vCardData = vcard,
                    phoneNumber = vcard.phone.takeIf { it.isNotEmpty() },
                    isSensitive = false
                )
            }

            // 3. Calendar Event
            if (trimmed.contains("BEGIN:VEVENT", ignoreCase = true)) {
                val summary = extractVEventField(trimmed, "SUMMARY")
                return ParsedPayload(
                    rawText = rawText,
                    format = format,
                    type = ContentType.EVENT,
                    title = summary.ifEmpty { "Calendar Event" },
                    displaySubtitle = "iCalendar Event",
                    isSensitive = false
                )
            }

            // 4. URL (Strict validation: only http and https, reject javascript:, file:, data:, intent:)
            if (isValidHttpUrl(trimmed)) {
                val cleanUrl = trimmed
                val host = try {
                    URI(cleanUrl).host ?: cleanUrl
                } catch (e: Exception) {
                    cleanUrl
                }
                return ParsedPayload(
                    rawText = rawText,
                    format = format,
                    type = ContentType.URL,
                    title = host,
                    displaySubtitle = cleanUrl,
                    safeUrl = cleanUrl
                )
            }

            // 5. Phone / Tel
            if (trimmed.startsWith("tel:", ignoreCase = true)) {
                val number = trimmed.substring(4).trim()
                return ParsedPayload(
                    rawText = rawText,
                    format = format,
                    type = ContentType.PHONE,
                    title = number,
                    displaySubtitle = "Phone Number",
                    phoneNumber = number
                )
            } else if (isPurePhoneNumber(trimmed)) {
                return ParsedPayload(
                    rawText = rawText,
                    format = format,
                    type = ContentType.PHONE,
                    title = trimmed,
                    displaySubtitle = "Phone Number",
                    phoneNumber = trimmed
                )
            }

            // 6. Email / Mailto
            if (trimmed.startsWith("mailto:", ignoreCase = true)) {
                val emailData = parseMailto(trimmed)
                return ParsedPayload(
                    rawText = rawText,
                    format = format,
                    type = ContentType.EMAIL,
                    title = emailData.address,
                    displaySubtitle = if (emailData.subject.isNotEmpty()) "Subject: ${emailData.subject}" else "Email Address",
                    emailData = emailData
                )
            } else if (isEmailAddress(trimmed)) {
                val emailData = EmailData(address = trimmed)
                return ParsedPayload(
                    rawText = rawText,
                    format = format,
                    type = ContentType.EMAIL,
                    title = trimmed,
                    displaySubtitle = "Email Address",
                    emailData = emailData
                )
            }

            // 7. SMS / SMSTO
            if (trimmed.startsWith("smsto:", ignoreCase = true) || trimmed.startsWith("sms:", ignoreCase = true)) {
                val smsData = parseSms(trimmed)
                return ParsedPayload(
                    rawText = rawText,
                    format = format,
                    type = ContentType.SMS,
                    title = smsData.number,
                    displaySubtitle = if (smsData.message.isNotEmpty()) smsData.message else "SMS Message",
                    smsData = smsData,
                    phoneNumber = smsData.number
                )
            }

            // 8. Geo location
            if (trimmed.startsWith("geo:", ignoreCase = true)) {
                val geo = parseGeo(trimmed)
                return ParsedPayload(
                    rawText = rawText,
                    format = format,
                    type = ContentType.GEO,
                    title = "${geo.latitude}, ${geo.longitude}",
                    displaySubtitle = if (geo.query.isNotEmpty()) geo.query else "Geographic Coordinates",
                    geoData = geo
                )
            }

            // 9. Product Barcode formats (EAN, UPC, Code 128, etc.)
            if (format.contains("EAN") || format.contains("UPC") || format.contains("CODE_") || format.contains("ITF")) {
                return ParsedPayload(
                    rawText = rawText,
                    format = format,
                    type = ContentType.PRODUCT,
                    title = rawText,
                    displaySubtitle = "Barcode ($format)"
                )
            }

            // 10. Default Plain Text
            val firstLine = trimmed.lines().firstOrNull() ?: trimmed
            val truncatedTitle = if (firstLine.length > 40) firstLine.take(37) + "..." else firstLine
            return ParsedPayload(
                rawText = rawText,
                format = format,
                type = ContentType.TEXT,
                title = truncatedTitle.ifEmpty { "Plain Text" },
                displaySubtitle = "${rawText.length} characters"
            )
        }

        private fun isValidHttpUrl(str: String): Boolean {
            val lower = str.lowercase(Locale.ROOT)
            if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
                return false
            }
            if (lower.startsWith("javascript:") || lower.startsWith("file:") || lower.startsWith("content:")) {
                return false
            }
            return try {
                val uri = URI(str)
                uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank()
            } catch (e: Exception) {
                false
            }
        }

        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        private fun isEmailAddress(str: String): Boolean {
            return EMAIL_REGEX.matches(str)
        }

        private fun isPurePhoneNumber(str: String): Boolean {
            val stripped = str.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
            if (stripped.length in 7..16 && (stripped.startsWith("+") || stripped.all { it.isDigit() })) {
                return true
            }
            return false
        }

        fun parseWifiPayload(raw: String): WifiData {
            var ssid = ""
            var password = ""
            var type = "WPA"
            var hidden = false

            // Standard WIFI:T:WPA;S:MySSID;P:MyPass;H:false;;
            // Note: need to handle escaped characters like \; and \: and \\
            val content = if (raw.startsWith("WIFI:", ignoreCase = true)) raw.substring(5) else raw
            val tokens = splitWifiTokens(content)
            for (token in tokens) {
                if (token.startsWith("S:", ignoreCase = true)) {
                    ssid = unescapeWifi(token.substring(2))
                } else if (token.startsWith("P:", ignoreCase = true)) {
                    password = unescapeWifi(token.substring(2))
                } else if (token.startsWith("T:", ignoreCase = true)) {
                    val rawType = token.substring(2).uppercase(Locale.ROOT)
                    type = when (rawType) {
                        "WPA", "WPA2", "WPA3", "WPA/WPA2" -> "WPA"
                        "WEP" -> "WEP"
                        "NOPASS" -> "None"
                        else -> rawType
                    }
                } else if (token.startsWith("H:", ignoreCase = true)) {
                    hidden = token.substring(2).equals("true", ignoreCase = true)
                }
            }

            return WifiData(
                ssid = ssid,
                password = password,
                securityType = type,
                isHidden = hidden
            )
        }

        private fun splitWifiTokens(str: String): List<String> {
            val list = mutableListOf<String>()
            val sb = java.lang.StringBuilder()
            var escaped = false

            for (ch in str) {
                if (escaped) {
                    sb.append(ch)
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                    sb.append(ch)
                } else if (ch == ';') {
                    val token = sb.toString().trim()
                    if (token.isNotEmpty()) list.add(token)
                    sb.clear()
                } else {
                    sb.append(ch)
                }
            }
            val remaining = sb.toString().trim()
            if (remaining.isNotEmpty()) list.add(remaining)
            return list
        }

        private fun unescapeWifi(text: String): String {
            val sb = StringBuilder()
            var escaped = false
            for (ch in text) {
                if (escaped) {
                    sb.append(ch)
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else {
                    sb.append(ch)
                }
            }
            return sb.toString()
        }

        private fun parseVCard(text: String): VCardData {
            var fn = ""
            var tel = ""
            var email = ""
            var org = ""
            var title = ""

            for (line in text.lines()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("FN:", ignoreCase = true)) {
                    fn = trimmed.substring(3).trim()
                } else if (trimmed.startsWith("N:", ignoreCase = true) && fn.isEmpty()) {
                    val parts = trimmed.substring(2).split(";")
                    fn = parts.reversed().filter { it.isNotBlank() }.joinToString(" ")
                } else if (trimmed.startsWith("TEL", ignoreCase = true)) {
                    val idx = trimmed.indexOf(":")
                    if (idx != -1 && tel.isEmpty()) {
                        tel = trimmed.substring(idx + 1).trim()
                    }
                } else if (trimmed.startsWith("EMAIL", ignoreCase = true)) {
                    val idx = trimmed.indexOf(":")
                    if (idx != -1 && email.isEmpty()) {
                        email = trimmed.substring(idx + 1).trim()
                    }
                } else if (trimmed.startsWith("ORG:", ignoreCase = true)) {
                    org = trimmed.substring(4).trim()
                } else if (trimmed.startsWith("TITLE:", ignoreCase = true)) {
                    title = trimmed.substring(6).trim()
                }
            }
            return VCardData(fullName = fn, phone = tel, email = email, organization = org, title = title)
        }

        private fun extractVEventField(text: String, field: String): String {
            for (line in text.lines()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("$field:", ignoreCase = true)) {
                    return trimmed.substring(field.length + 1).trim()
                }
            }
            return ""
        }

        private fun parseMailto(raw: String): EmailData {
            val withoutScheme = raw.substring(7)
            val parts = withoutScheme.split("?", limit = 2)
            val address = parts[0].trim()
            var subject = ""
            var body = ""

            if (parts.size > 1) {
                val queryParams = parts[1].split("&")
                for (param in queryParams) {
                    val kv = param.split("=", limit = 2)
                    if (kv.isNotEmpty()) {
                        val key = kv[0].lowercase(Locale.ROOT)
                        val value = if (kv.size > 1) java.net.URLDecoder.decode(kv[1], "UTF-8") else ""
                        if (key == "subject") subject = value
                        if (key == "body") body = value
                    }
                }
            }
            return EmailData(address = address, subject = subject, body = body)
        }

        private fun parseSms(raw: String): SmsData {
            val colonIdx = raw.indexOf(":")
            val payload = if (colonIdx != -1) raw.substring(colonIdx + 1) else raw
            return if (payload.contains("?")) {
                val parts = payload.split("?", limit = 2)
                val number = parts[0].trim()
                var message = ""
                val queryParams = parts[1].split("&")
                for (param in queryParams) {
                    val kv = param.split("=", limit = 2)
                    if (kv.isNotEmpty() && kv[0].equals("body", ignoreCase = true)) {
                        message = if (kv.size > 1) java.net.URLDecoder.decode(kv[1], "UTF-8") else ""
                    }
                }
                SmsData(number = number, message = message)
            } else if (payload.contains(":")) {
                val parts = payload.split(":", limit = 2)
                SmsData(number = parts[0].trim(), message = parts.getOrElse(1) { "" }.trim())
            } else {
                SmsData(number = payload.trim(), message = "")
            }
        }

        private fun parseGeo(raw: String): GeoData {
            val coords = raw.substring(4).split("?", limit = 2)
            val latLng = coords[0].split(",")
            val lat = latLng.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val lng = latLng.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            var query = ""
            if (coords.size > 1 && coords[1].startsWith("q=")) {
                query = java.net.URLDecoder.decode(coords[1].substring(2), "UTF-8")
            }
            return GeoData(latitude = lat, longitude = lng, query = query)
        }
    }
}
