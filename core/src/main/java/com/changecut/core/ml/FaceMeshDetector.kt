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
class FaceMeshDetector @Inject constructor(
    private val tensorFlowEngine: TensorFlowEngine
) {
    data class FaceLandmarks(
        val landmarks: List<Pair<Float, Float>>,
        val boundingBox: android.graphics.RectF? = null,
        val leftEye: List<Pair<Float, Float>> = emptyList(),
        val rightEye: List<Pair<Float, Float>> = emptyList(),
        val nose: Pair<Float, Float> = 0f to 0f,
        val mouth: List<Pair<Float, Float>> = emptyList()
    )

    fun detect(frame: Bitmap): FaceLandmarks? {
        if (!tensorFlowEngine.isAvailable()) return null

        val width = frame.width
        val height = frame.height
        val inputBuffer = bitmapToFloatBuffer(frame, width, height)

        val result = tensorFlowEngine.detectFaceLandmarks(inputBuffer, width, height)
            ?: return null

        val landmarks = result.landmarks.map {
            (it.first * width) to (it.second * height)
        }

        return FaceLandmarks(
            landmarks = landmarks,
            boundingBox = calculateBoundingBox(landmarks),
            leftEye = landmarks.filterIndexed { i, _ -> i in 33..133 },
            rightEye = landmarks.filterIndexed { i, _ -> i in 362..463 },
            nose = landmarks.getOrElse(1) { 0f to 0f },
            mouth = landmarks.filterIndexed { i, _ -> i in 61..291 }
        )
    }

    fun drawMesh(frame: Bitmap, landmarks: FaceLandmarks, color: Int = Color.GREEN): Bitmap {
        val output = frame.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            this.color = color
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        // Draw connections for face outline
        val faceConnections = listOf(
            10 to 338, 338 to 297, 297 to 332, 332 to 284,
            284 to 251, 251 to 389, 389 to 356, 356 to 454,
            454 to 323, 323 to 361, 361 to 288, 288 to 397,
            397 to 365, 365 to 379, 379 to 378, 378 to 400,
            400 to 377, 377 to 152, 152 to 148, 148 to 176,
            176 to 149, 149 to 150, 150 to 136, 136 to 172,
            172 to 58, 58 to 132, 132 to 93, 93 to 234,
            234 to 127, 127 to 162, 162 to 21, 21 to 54,
            54 to 103, 103 to 67, 67 to 109, 109 to 10
        )

        for ((start, end) in faceConnections) {
            val p1 = landmarks.landmarks.getOrNull(start) ?: continue
            val p2 = landmarks.landmarks.getOrNull(end) ?: continue
            canvas.drawLine(p1.first, p1.second, p2.first, p2.second, paint)
        }

        paint.style = Paint.Style.FILL
        paint.strokeWidth = 4f
        landmarks.landmarks.forEach { (x, y) ->
            canvas.drawCircle(x, y, 2f, paint)
        }

        return output
    }

    private fun calculateBoundingBox(landmarks: List<Pair<Float, Float>>): android.graphics.RectF {
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
        landmarks.forEach { (x, y) ->
            minX = minOf(minX, x); minY = minOf(minY, y)
            maxX = maxOf(maxX, x); maxY = maxOf(maxY, y)
        }
        return android.graphics.RectF(minX, minY, maxX, maxY)
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
