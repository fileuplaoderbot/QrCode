package com.example.decoder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.EnumMap

object ImageDecoder {

    suspend fun decodeFromUri(context: Context, uri: Uri): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            // First decode bounds
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val maxDimension = 1600
            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (bitmap == null) return@withContext null

            val result = decodeFromBitmap(bitmap)
            bitmap.recycle()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun decodeFromBitmap(bitmap: Bitmap): Pair<String, String>? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        val reader = MultiFormatReader().apply {
            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(
                    DecodeHintType.POSSIBLE_FORMATS,
                    listOf(
                        BarcodeFormat.QR_CODE,
                        BarcodeFormat.DATA_MATRIX,
                        BarcodeFormat.AZTEC,
                        BarcodeFormat.PDF_417,
                        BarcodeFormat.EAN_13,
                        BarcodeFormat.EAN_8,
                        BarcodeFormat.UPC_A,
                        BarcodeFormat.UPC_E,
                        BarcodeFormat.CODE_128,
                        BarcodeFormat.CODE_39,
                        BarcodeFormat.CODE_93,
                        BarcodeFormat.CODABAR,
                        BarcodeFormat.ITF
                    )
                )
                put(DecodeHintType.TRY_HARDER, java.lang.Boolean.TRUE)
                put(DecodeHintType.CHARACTER_SET, "UTF-8")
            }
            setHints(hints)
        }

        return try {
            val result = reader.decodeWithState(binaryBitmap)
            Pair(result.text, result.barcodeFormat.name)
        } catch (e: NotFoundException) {
            null
        } catch (e: Exception) {
            null
        } finally {
            reader.reset()
        }
    }
}
