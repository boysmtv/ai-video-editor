package com.changecut.core.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortraitSegmentation @Inject constructor(
    private val tensorFlowEngine: TensorFlowEngine
) {
    fun segment(frame: Bitmap): Bitmap {
        if (!tensorFlowEngine.isAvailable()) return frame

        val width = frame.width
        val height = frame.height
        val inputBuffer = bitmapToFloatBuffer(frame, width, height)

        val result = tensorFlowEngine.runPortraitSegmentation(inputBuffer, width, height)
            ?: return frame

        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(maskBitmap)
        val paint = Paint()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = (y * width + x) * 4
                val confidence = result.mask.getFloat(idx)
                val alpha = (confidence * 255).toInt().coerceIn(0, 255)
                paint.color = Color.argb(alpha, 255, 255, 255)
                canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
            }
        }

        result.mask.rewind()
        return maskBitmap
    }

    fun removeBackground(frame: Bitmap, replacementColor: Int = Color.TRANSPARENT): Bitmap {
        val mask = segment(frame)
        val output = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(replacementColor)

        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                val maskAlpha = mask.getPixel(x, y) shr 24 and 0xFF
                if (maskAlpha > 128) {
                    val pixel = frame.getPixel(x, y)
                    output.setPixel(x, y, pixel)
                }
            }
        }
        return output
    }

    private fun bitmapToFloatBuffer(bitmap: Bitmap, width: Int, height: Int): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(width * height * 3 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (pixel in pixels) {
            buffer.put(((pixel shr 16) and 0xFF) / 255.0f)
            buffer.put(((pixel shr 8) and 0xFF) / 255.0f)
            buffer.put((pixel and 0xFF) / 255.0f)
        }
        buffer.position(0)
        return buffer
    }
}
