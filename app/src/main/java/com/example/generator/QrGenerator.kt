package com.example.generator

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.EnumMap

object QrGenerator {

    suspend fun generateQrBitmap(
        content: String,
        size: Int = 800,
        errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
        fgColor: Int = Color.BLACK,
        bgColor: Int = Color.WHITE
    ): Bitmap = withContext(Dispatchers.Default) {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel)
            put(EncodeHintType.MARGIN, 1) // 1 quiet zone block
        }

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) fgColor else bgColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap
    }

    fun buildWifiPayload(ssid: String, pass: String, type: String, hidden: Boolean): String {
        val escapedSsid = escapeWifiString(ssid)
        val escapedPass = escapeWifiString(pass)
        val sec = when (type.uppercase()) {
            "WEP" -> "WEP"
            "NONE", "NOPASS", "OPEN" -> "nopass"
            else -> "WPA"
        }
        val hiddenPart = if (hidden) "H:true;" else ""
        return "WIFI:T:$sec;S:$escapedSsid;P:$escapedPass;$hiddenPart;"
    }

    private fun escapeWifiString(str: String): String {
        val sb = StringBuilder()
        for (c in str) {
            if (c == '\\' || c == ';' || c == ',' || c == ':' || c == '"') {
                sb.append('\\')
            }
            sb.append(c)
        }
        return sb.toString()
    }

    fun buildVCardPayload(
        name: String,
        phone: String,
        email: String,
        org: String,
        title: String
    ): String {
        return buildString {
            append("BEGIN:VCARD\n")
            append("VERSION:3.0\n")
            append("FN:$name\n")
            if (org.isNotBlank()) append("ORG:$org\n")
            if (title.isNotBlank()) append("TITLE:$title\n")
            if (phone.isNotBlank()) append("TEL;TYPE=CELL:$phone\n")
            if (email.isNotBlank()) append("EMAIL;TYPE=INTERNET:$email\n")
            append("END:VCARD")
        }
    }

    fun buildEmailPayload(address: String, subject: String, body: String): String {
        val encodedSubject = java.net.URLEncoder.encode(subject, "UTF-8").replace("+", "%20")
        val encodedBody = java.net.URLEncoder.encode(body, "UTF-8").replace("+", "%20")
        return "mailto:$address?subject=$encodedSubject&body=$encodedBody"
    }

    fun buildSmsPayload(number: String, message: String): String {
        val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8").replace("+", "%20")
        return "smsto:$number:$encodedMessage"
    }

    suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Uri? = withContext(Dispatchers.IO) {
        val filename = "OfflineQR_${System.currentTimeMillis()}.png"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OfflineQR")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext null

                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
                uri
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/OfflineQR"
                val file = File(imagesDir)
                if (!file.exists()) file.mkdirs()
                val imageFile = File(imagesDir, filename)
                FileOutputStream(imageFile).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                Uri.fromFile(imageFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveBitmapForSharing(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        try {
            val cachePath = File(context.cacheDir, "shared_qr")
            if (!cachePath.exists()) cachePath.mkdirs()
            val file = File(cachePath, "qr_share.png")
            val outputStream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            // Using FileProvider or content uri
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
