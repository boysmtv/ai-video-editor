package com.changecut.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

data class ScoreBreakdown(
    val stability: Double = 0.0,
    val brightness: Double = 0.0,
    val sharpness: Double = 0.0,
    val motion: Double = 0.0,
    val faceDetection: Double = 0.0
) {
    val total: Double get() = (stability + brightness + sharpness + motion + faceDetection) / 5.0
}

data class ScoredClip(
    val videoPath: String,
    val startTimeUs: Long,
    val endTimeUs: Long,
    val durationUs: Long,
    val score: ScoreBreakdown,
    val thumbnail: Bitmap? = null
)

@Singleton
class AiBestMomentPicker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val ANALYSIS_FRAME_INTERVAL_US = 100_000L
        private const val SAMPLE_COUNT = 20
        private const val MOTION_THRESHOLD = 0.08
        private const val FACE_ASPECT_RATIO = 0.15
    }

    fun analyzeClips(videoPaths: List<String>): List<ScoredClip> {
        return videoPaths.flatMap { path -> analyzeSingleVideo(path) }
    }

    private fun analyzeSingleVideo(videoPath: String): List<ScoredClip> {
        val retriever = MediaMetadataRetriever()
        val clips = mutableListOf<ScoredClip>()

        try {
            retriever.setDataSource(videoPath)
            val durationStr = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )
            val durationMs = durationStr?.toLongOrNull() ?: return clips
            val durationUs = durationMs * 1000L

            val segmentDuration = durationUs / SAMPLE_COUNT

            for (i in 0 until SAMPLE_COUNT) {
                val startUs = i * segmentDuration
                val endUs = ((i + 1) * segmentDuration).coerceAtMost(durationUs)

                val frame1 = retriever.getFrameAtTime(
                    startUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                val frame2 = retriever.getFrameAtTime(
                    (startUs + segmentDuration / 2).coerceAtMost(durationUs),
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )

                if (frame1 != null) {
                    val score = scoreSegment(frame1, frame2, frame1)
                    clips.add(
                        ScoredClip(
                            videoPath = videoPath,
                            startTimeUs = startUs,
                            endTimeUs = endUs,
                            durationUs = segmentDuration,
                            score = score,
                            thumbnail = frame1
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        return clips
    }

    private fun scoreSegment(
        frame1: Bitmap,
        frame2: Bitmap?,
        thumbnail: Bitmap
    ): ScoreBreakdown {
        val width = thumbnail.width
        val height = thumbnail.height
        val pixels = IntArray(width * height)
        thumbnail.getPixels(pixels, 0, width, 0, 0, width, height)

        val stability = computeStability(frame1, frame2)
        val brightness = computeBrightness(pixels)
        val sharpness = computeSharpness(pixels, width, height)
        val motion = computeMotionScore(frame1, frame2)
        val faceScore = detectFaces(pixels, width, height)

        return ScoreBreakdown(
            stability = stability,
            brightness = brightness,
            sharpness = sharpness,
            motion = motion,
            faceDetection = faceScore
        )
    }

    private fun computeStability(frame1: Bitmap, frame2: Bitmap?): Double {
        if (frame2 == null) return 0.5

        val w = minOf(frame1.width, frame2.width)
        val h = minOf(frame1.height, frame2.height)
        val p1 = IntArray(w * h)
        val p2 = IntArray(w * h)
        frame1.getPixels(p1, 0, w, 0, 0, w, h)
        frame2.getPixels(p2, 0, w, 0, 0, w, h)

        var diffSum = 0.0
        for (i in p1.indices) {
            val dr = abs(((p1[i] shr 16) and 0xFF) - ((p2[i] shr 16) and 0xFF))
            val dg = abs(((p1[i] shr 8) and 0xFF) - ((p2[i] shr 8) and 0xFF))
            val db = abs((p1[i] and 0xFF) - (p2[i] and 0xFF))
            diffSum += (dr + dg + db) / (3.0 * 255.0)
        }

        val avgDiff = diffSum / p1.size
        return (1.0 - avgDiff).coerceIn(0.0, 1.0)
    }

    private fun computeBrightness(pixels: IntArray): Double {
        var totalLuminance = 0.0
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val luminance = 0.299 * r + 0.587 * g + 0.114 * b
            totalLuminance += luminance
        }
        val avg = totalLuminance / pixels.size
        return when {
            avg < 30.0 -> 0.1
            avg > 220.0 -> 0.3
            else -> avg / 255.0
        }
    }

    private fun computeSharpness(pixels: IntArray, width: Int, height: Int): Double {
        val gray = IntArray(pixels.size) { i ->
            val p = pixels[i]
            ((0.299 * ((p shr 16) and 0xFF) +
                    0.587 * ((p shr 8) and 0xFF) +
                    0.114 * (p and 0xFF))).toInt()
        }

        var laplacianSum = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val center = gray[idx].toDouble()
                val top = gray[idx - width].toDouble()
                val bottom = gray[idx + width].toDouble()
                val left = gray[idx - 1].toDouble()
                val right = gray[idx + 1].toDouble()
                val laplacian = abs(4 * center - top - bottom - left - right)
                laplacianSum += laplacian
                count++
            }
        }

        val avgLaplacian = laplacianSum / count
        return (avgLaplacian / 255.0).coerceIn(0.0, 1.0)
    }

    private fun computeMotionScore(frame1: Bitmap, frame2: Bitmap?): Double {
        if (frame2 == null) return 0.5

        val w = minOf(frame1.width, frame2.width)
        val h = minOf(frame1.height, frame2.height)
        val p1 = IntArray(w * h)
        val p2 = IntArray(w * h)
        frame1.getPixels(p1, 0, w, 0, 0, w, h)
        frame2.getPixels(p2, 0, w, 0, 0, w, h)

        val gridSize = 8
        val cellW = w / gridSize
        val cellH = h / gridSize
        var motionCount = 0

        for (gy in 0 until gridSize) {
            for (gx in 0 until gridSize) {
                var cellDiff = 0.0
                var cellPixels = 0
                for (y in gy * cellH until (gy + 1) * cellH) {
                    for (x in gx * cellW until (gx + 1) * cellW) {
                        val idx = y * w + x
                        val dr = abs(((p1[idx] shr 16) and 0xFF) - ((p2[idx] shr 16) and 0xFF))
                        val dg = abs(((p1[idx] shr 8) and 0xFF) - ((p2[idx] shr 8) and 0xFF))
                        val db = abs((p1[idx] and 0xFF) - (p2[idx] and 0xFF))
                        cellDiff += (dr + dg + db) / (3.0 * 255.0)
                        cellPixels++
                    }
                }
                if (cellDiff / cellPixels > MOTION_THRESHOLD) {
                    motionCount++
                }
            }
        }

        val motionRatio = motionCount.toDouble() / (gridSize * gridSize)
        return motionRatio.coerceIn(0.0, 1.0)
    }

    private fun detectFaces(pixels: IntArray, width: Int, height: Int): Double {
        val gray = IntArray(pixels.size) { i ->
            val p = pixels[i]
            ((0.299 * ((p shr 16) and 0xFF) +
                    0.587 * ((p shr 8) and 0xFF) +
                    0.114 * (p and 0xFF))).toInt()
        }

        var skinPixels = 0
        for (i in pixels.indices) {
            val r = (pixels[i] shr 16) and 0xFF
            val g = (pixels[i] shr 8) and 0xFF
            val b = pixels[i] and 0xFF
            if (isSkinColor(r, g, b)) skinPixels++
        }

        val skinRatio = skinPixels.toDouble() / pixels.size
        val idealRatio = FACE_ASPECT_RATIO
        return if (skinRatio >= idealRatio * 0.5) {
            (skinRatio / (idealRatio * 2)).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
    }

    private fun isSkinColor(r: Int, g: Int, b: Int): Boolean {
        return r > 95 && g > 40 && b > 20 &&
                r > g && r > b &&
                (r - g).coerceAtLeast(g - r) > 15 &&
                r > 100 && g > 60 && b > 40
    }

    fun pickBestMoments(
        scoredClips: List<ScoredClip>,
        maxDurationUs: Long
    ): List<ScoredClip> {
        val sorted = scoredClips.sortedByDescending { it.score.total }
        val selected = mutableListOf<ScoredClip>()
        var accumulated = 0L

        for (clip in sorted) {
            if (accumulated >= maxDurationUs) break
            val duration = clip.durationUs.coerceAtMost(maxDurationUs - accumulated)
            if (duration > 0) {
                selected.add(
                    clip.copy(
                        endTimeUs = clip.startTimeUs + duration,
                        durationUs = duration
                    )
                )
                accumulated += duration
            }
        }

        return selected.sortedBy { it.startTimeUs }
    }

    fun pickIntro(scoredClips: List<ScoredClip>): ScoredClip? {
        val firstHalf = scoredClips
            .filter { it.startTimeUs < scoredClips.maxOf { c -> c.endTimeUs } / 2 }
        return firstHalf.maxByOrNull { it.score.total }
    }
}
