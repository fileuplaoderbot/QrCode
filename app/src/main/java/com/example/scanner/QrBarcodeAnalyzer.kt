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
import java.nio.ByteBuffer
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
            val buffer: ByteBuffer = image.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            val width = image.width
            val height = image.height

            // ZXing PlanarYUVLuminanceSource expects YUV luminance data
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
                val format = result.barcodeFormat.name
                val now = System.currentTimeMillis()

                if (text != null && text.isNotBlank()) {
                    if (text != lastScannedText || (now - lastScannedTimestamp) > throttleIntervalMs) {
                        lastScannedText = text
                        lastScannedTimestamp = now
                        onResultFound(text, format)
                    }
                }
            } catch (e: NotFoundException) {
                // No barcode/QR found in this frame
            } catch (e: Exception) {
                // Ignore decoding errors on blurry frames
            } finally {
                reader.reset()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            image.close()
        }
    }

    fun resetThrottle() {
        lastScannedText = null
        lastScannedTimestamp = 0L
    }
}
