package com.changecut.core.ml

import android.graphics.PointF
import android.graphics.RectF
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class ObjectTracker @Inject constructor() {
    data class TrackedPoint(
        val position: PointF,
        val confidence: Float,
        val featureId: Int
    )

    data class TrackedObject(
        val id: Int,
        val boundingBox: RectF,
        val points: List<TrackedPoint>,
        val velocityX: Float = 0f,
        val velocityY: Float = 0f
    )

    private var nextObjectId = 0
    private val trackedObjects = mutableListOf<TrackedObject>()
    private var previousFrame: FloatArray? = null
    private var previousWidth = 0
    private var previousHeight = 0

    private val featureExtractor = ShiTomasiFeatureDetector()

    fun track(
        currentFrame: android.graphics.Bitmap,
        selectRect: RectF? = null
    ): List<TrackedObject> {
        val width = currentFrame.width
        val height = currentFrame.height
        val grayFrame = bitmapToGrayscale(currentFrame)

        val features = featureExtractor.detect(grayFrame, width, height, selectRect)

        if (trackedObjects.isEmpty() && features.isNotEmpty()) {
            val obj = TrackedObject(
                id = nextObjectId++,
                boundingBox = RectF(selectRect ?: RectF(0f, 0f, width.toFloat(), height.toFloat())),
                points = features
            )
            trackedObjects.clear()
            trackedObjects.add(obj)
        } else if (trackedObjects.isNotEmpty() && features.isNotEmpty()) {
            val previousGray = previousFrame
            if (previousGray != null) {
                val updated = trackedObjects.map { obj ->
                    val newPoints = estimateFlow(
                        previousGray, grayFrame,
                        previousWidth, previousHeight,
                        width, height,
                        obj.points
                    )
                    val avgX = newPoints.map { it.position.x }.average().toFloat()
                    val avgY = newPoints.map { it.position.y }.average().toFloat()
                    val minX = newPoints.minOf { it.position.x }
                    val maxX = newPoints.maxOf { it.position.x }
                    val minY = newPoints.minOf { it.position.y }
                    val maxY = newPoints.maxOf { it.position.y }

                    obj.copy(
                        boundingBox = RectF(minX, minY, maxX, maxY),
                        points = newPoints,
                        velocityX = avgX - obj.boundingBox.centerX(),
                        velocityY = avgY - obj.boundingBox.centerY()
                    )
                }
                trackedObjects.clear()
                trackedObjects.addAll(updated)
            }
        }

        previousFrame = grayFrame
        previousWidth = width
        previousHeight = height

        return trackedObjects.toList()
    }

    fun reset() {
        trackedObjects.clear()
        nextObjectId = 0
        previousFrame = null
    }

    private fun estimateFlow(
        prev: FloatArray, curr: FloatArray,
        prevW: Int, prevH: Int,
        currW: Int, currH: Int,
        points: List<TrackedPoint>
    ): List<TrackedPoint> {
        return points.mapNotNull { point ->
            val x = (point.position.x / prevW * currW).toInt().coerceIn(1, currW - 2)
            val y = (point.position.y / prevH * currH).toInt().coerceIn(1, currH - 2)

            val blockSize = 5
            var minSsd = Float.MAX_VALUE
            var bestDx = 0f; var bestDy = 0f
            val searchRadius = 10

            for (dy in -searchRadius..searchRadius) {
                for (dx in -searchRadius..searchRadius) {
                    var ssd = 0f
                    for (by in -blockSize..blockSize) {
                        for (bx in -blockSize..blockSize) {
                            val prevIdx = ((y + by) * prevW + (x + bx)).coerceIn(0, prev.size - 1)
                            val currIdx = ((y + by + dy) * currW + (x + bx + dx)).coerceIn(0, curr.size - 1)
                            val diff = prev[prevIdx] - curr[currIdx]
                            ssd += diff * diff
                        }
                    }
                    if (ssd < minSsd) {
                        minSsd = ssd; bestDx = dx.toFloat(); bestDy = dy.toFloat()
                    }
                }
            }

            val confidence = 1.0f / (1.0f + minSsd / (blockSize * blockSize * 4f))
            if (confidence > 0.3f) {
                TrackedPoint(
                    position = PointF(
                        point.position.x + bestDx * currW / prevW,
                        point.position.y + bestDy * currH / prevH
                    ),
                    confidence = confidence,
                    featureId = point.featureId
                )
            } else null
        }
    }

    private fun bitmapToGrayscale(frame: android.graphics.Bitmap): FloatArray {
        val w = frame.width; val h = frame.height
        val pixels = IntArray(w * h)
        frame.getPixels(pixels, 0, w, 0, 0, w, h)
        return FloatArray(w * h) { i ->
            val p = pixels[i]
            (0.299f * ((p shr 16) and 0xFF) +
             0.587f * ((p shr 8) and 0xFF) +
             0.114f * (p and 0xFF)) / 255f
        }
    }
}

class ShiTomasiFeatureDetector {
    fun detect(gray: FloatArray, width: Int, height: Int, roi: RectF? = null): List<ObjectTracker.TrackedPoint> {
        val features = mutableListOf<ObjectTracker.TrackedPoint>()
        val minDistance = 20
        val qualityLevel = 0.01f
        val maxCorners = 100
        val blockSize = 3

        val response = FloatArray(width * height)
        val k = 0.04f

        for (y in blockSize until height - blockSize) {
            for (x in blockSize until width - blockSize) {
                if (roi != null && !roi.contains(x.toFloat(), y.toFloat())) continue

                var ixx = 0f; var iyy = 0f; var ixy = 0f

                for (dy in -blockSize..blockSize) {
                    for (dx in -blockSize..blockSize) {
                        val px = ((y + dy) * width + (x + dx)).coerceIn(0, gray.size - 1)
                        if (px <= 0 || px >= width) continue
                        val ix = gray[px + 1] - gray[px - 1]
                        val iy = if (px + width < gray.size && px - width >= 0)
                            gray[px + width] - gray[px - width] else 0f
                        ixx += ix * ix; iyy += iy * iy; ixy += ix * iy
                    }
                }

                val det = ixx * iyy - ixy * ixy
                val trace = ixx + iyy
                response[y * width + x] = det - k * trace * trace
            }
        }

        val maxResponse = response.maxOrNull() ?: 0f
        val threshold = maxResponse * qualityLevel

        for (y in blockSize until height - blockSize step 3) {
            for (x in blockSize until width - blockSize step 3) {
                val idx = y * width + x
                if (response[idx] > threshold) {
                    var isMax = true
                    for (dy in -minDistance..minDistance step 3) {
                        for (dx in -minDistance..minDistance step 3) {
                            val ny = y + dy; val nx = x + dx
                            if (ny in 0 until height && nx in 0 until width) {
                                if (response[ny * width + nx] > response[idx]) {
                                    isMax = false; break
                                }
                            }
                        }
                        if (!isMax) break
                    }
                    if (isMax && features.size < maxCorners) {
                        features.add(ObjectTracker.TrackedPoint(
                            position = PointF(x.toFloat(), y.toFloat()),
                            confidence = response[idx] / maxResponse,
                            featureId = features.size
                        ))
                    }
                }
            }
        }

        return features
    }
}
