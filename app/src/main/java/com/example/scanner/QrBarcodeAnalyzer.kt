package com.example.scanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap

class QrBarcodeAnalyzer(
    private val onResultFound: (text: String, format: String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
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

    private var lastScannedText: String? = null
    private var lastScannedTimestamp: Long = 0L
    private val throttleIntervalMs = 1200L

    override fun analyze(image: ImageProxy) {
        try {
            val yPlane = image.planes.getOrNull(0) ?: return
            val buffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val width = image.width
            val height = image.height

            // Handle row stride correctly to avoid buffer overflow or crop mismatch
            val data: ByteArray
            if (rowStride == width) {
                data = ByteArray(buffer.remaining())
                buffer.get(data)
            } else {
                data = ByteArray(width * height)
                val row = ByteArray(rowStride)
                var destPos = 0
                for (i in 0 until height) {
                    val toRead = minOf(rowStride, buffer.remaining())
                    if (toRead <= 0) break
                    buffer.get(row, 0, toRead)
                    System.arraycopy(row, 0, data, destPos, width)
                    destPos += width
                }
            }

            val source = PlanarYUVLuminanceSource(
                data,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )

            val bitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val result = reader.decodeWithState(bitmap)
                val text = result.text
                val format = result.barcodeFormat?.name ?: "QR_CODE"
                val now = System.currentTimeMillis()

                if (!text.isNullOrBlank()) {
                    if (text != lastScannedText || (now - lastScannedTimestamp) > throttleIntervalMs) {
                        lastScannedText = text
                        lastScannedTimestamp = now
                        onResultFound(text, format)
                    }
                }
            } catch (e: NotFoundException) {
                // No QR/barcode in this frame
            } catch (t: Throwable) {
                // Ignore transient frame parsing errors
            } finally {
                reader.reset()
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            try {
                image.close()
            } catch (ignored: Throwable) {}
        }
    }

    fun resetThrottle() {
        lastScannedText = null
        lastScannedTimestamp = 0L
    }
}
