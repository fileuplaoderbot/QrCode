package com.example.decoder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.EnumMap

object ImageDecoder {

    suspend fun decodeFromUri(context: Context, uri: Uri): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            // Read bytes once to handle single-read content providers safely
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null

            if (bytes.isEmpty()) return@withContext null

            // Inspect EXIF orientation
            val rotationDegrees = try {
                val exif = ExifInterface(ByteArrayInputStream(bytes))
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } catch (ignored: Throwable) {
                0f
            }

            // Decode bounds
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext null

            // We test standard scale, and if needed, half scale
            val candidateBitmaps = mutableListOf<Bitmap>()

            // 1. Scaled to max 1200px
            var sampleSize = 1
            val maxDim = 1400
            while (origWidth / sampleSize > maxDim || origHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)

            if (decodedBitmap != null) {
                if (rotationDegrees != 0f) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees) }
                    val rotated = Bitmap.createBitmap(decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true)
                    if (rotated != decodedBitmap) {
                        decodedBitmap.recycle()
                        decodedBitmap = rotated
                    }
                }
                candidateBitmaps.add(decodedBitmap)
            }

            // Also try full resolution if sampleSize > 1 and image isn't gigantically huge
            if (sampleSize > 1 && origWidth <= 2400 && origHeight <= 2400) {
                val fullOptions = BitmapFactory.Options().apply {
                    inSampleSize = 1
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                var fullBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, fullOptions)
                if (fullBmp != null) {
                    if (rotationDegrees != 0f) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees) }
                        val rotated = Bitmap.createBitmap(fullBmp, 0, 0, fullBmp.width, fullBmp.height, matrix, true)
                        if (rotated != fullBmp) {
                            fullBmp.recycle()
                            fullBmp = rotated
                        }
                    }
                    candidateBitmaps.add(fullBmp)
                }
            }

            // Iterate candidate bitmaps and attempt multi-stage decode
            for (bmp in candidateBitmaps) {
                val result = decodeWithMultipleStrategies(bmp)
                if (result != null) {
                    // Clean up bitmaps
                    candidateBitmaps.forEach { it.recycle() }
                    return@withContext result
                }
            }

            candidateBitmaps.forEach { it.recycle() }
            null
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    private fun decodeWithMultipleStrategies(bitmap: Bitmap): Pair<String, String>? {
        // Try direct first
        val direct = decodeSingleOrientation(bitmap)
        if (direct != null) return direct

        // If direct fails, try 90, 180, 270 degree rotations
        val angles = floatArrayOf(90f, 180f, 270f)
        for (angle in angles) {
            val matrix = Matrix().apply { postRotate(angle) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val res = decodeSingleOrientation(rotated)
            if (rotated != bitmap) {
                rotated.recycle()
            }
            if (res != null) return res
        }

        return null
    }

    private fun decodeSingleOrientation(bitmap: Bitmap): Pair<String, String>? {
        val width = bitmap.width
        val height = bitmap.height
        val rawPixels = IntArray(width * height)
        bitmap.getPixels(rawPixels, 0, width, 0, 0, width, height)

        // Composite alpha over pure white background (fixes transparent PNGs / downloaded QR codes)
        val cleanPixels = IntArray(width * height)
        for (i in 0 until width * height) {
            val pixel = rawPixels[i]
            val a = (pixel ushr 24) and 0xFF
            if (a < 255) {
                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF
                val outR = (r * a + 255 * (255 - a)) / 255
                val outG = (g * a + 255 * (255 - a)) / 255
                val outB = (b * a + 255 * (255 - a)) / 255
                cleanPixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            } else {
                cleanPixels[i] = pixel
            }
        }

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
                put(DecodeHintType.ALSO_INVERTED, java.lang.Boolean.TRUE)
            }
            setHints(hints)
        }

        val source = RGBLuminanceSource(width, height, cleanPixels)

        // Strategy 1: HybridBinarizer (Standard)
        try {
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(binaryBitmap)
            return Pair(result.text, result.barcodeFormat.name)
        } catch (ignored: Throwable) {} finally {
            reader.reset()
        }

        // Strategy 2: GlobalHistogramBinarizer (High contrast / monochrome)
        try {
            val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
            val result = reader.decodeWithState(binaryBitmap)
            return Pair(result.text, result.barcodeFormat.name)
        } catch (ignored: Throwable) {} finally {
            reader.reset()
        }

        // Strategy 3: Inverted HybridBinarizer (Dark mode / white on black)
        try {
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source.invert()))
            val result = reader.decodeWithState(binaryBitmap)
            return Pair(result.text, result.barcodeFormat.name)
        } catch (ignored: Throwable) {} finally {
            reader.reset()
        }

        // Strategy 4: Center-crop (focused on center 70% where users frame codes)
        if (width > 200 && height > 200) {
            try {
                val cropLeft = (width * 0.15f).toInt()
                val cropTop = (height * 0.15f).toInt()
                val cropWidth = (width * 0.70f).toInt()
                val cropHeight = (height * 0.70f).toInt()
                val croppedSource = source.crop(cropLeft, cropTop, cropWidth, cropHeight)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(croppedSource))
                val result = reader.decodeWithState(binaryBitmap)
                return Pair(result.text, result.barcodeFormat.name)
            } catch (ignored: Throwable) {} finally {
                reader.reset()
            }
        }

        return null
    }

    fun decodeFromBitmap(bitmap: Bitmap): Pair<String, String>? {
        return decodeWithMultipleStrategies(bitmap)
    }
}
