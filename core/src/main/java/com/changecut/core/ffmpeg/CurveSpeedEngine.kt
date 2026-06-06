package com.changecut.core.ffmpeg

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurveSpeedEngine @Inject constructor() {

    data class SpeedPoint(
        val timeUs: Long,
        val speed: Float
    )

    fun buildCurveSpeedFilter(points: List<SpeedPoint>, totalDurationUs: Long): String {
        if (points.size < 2) {
            val speed = points.firstOrNull()?.speed ?: 1f
            return "setpts=${1 / speed}*PTS"
        }
        val segments = buildSegments(points, totalDurationUs)
        val concatParts = mutableListOf<String>()
        segments.forEachIndexed { i, seg ->
            concatParts.add("[0:v]trim=start=${seg.startUs / 1e6}:end=${seg.endUs / 1e6},setpts=${1 / seg.speed}*PTS[v${i}]")
            concatParts.add("[0:a]atrim=start=${seg.startUs / 1e6}:end=${seg.endUs / 1e6},atempo=${seg.speed.coerceIn(0.5f, 2.0f)}[a${i}]")
        }
        val vConcat = (0 until segments.size).joinToString("") { "[v${it}]" } + "concat=n=${segments.size}:v=1:a=0"
        val aConcat = (0 until segments.size).joinToString("") { "[a${it}]" } + "concat=n=${segments.size}:v=0:a=1"
        return "${concatParts.joinToString(";")};$vConcat;$aConcat"
    }

    fun buildReverseFilter(): String = "reverse,areverse"

    fun buildFreezeFrameFilter(freezeTimeUs: Long, freezeDurationMs: Int): String {
        val freezeSec = freezeTimeUs / 1e6
        val durSec = freezeDurationMs / 1000f
        return "trim=start=0:end=$freezeSec[v1];[v1]loop=loop=${(durSec * 30).toInt()}:size=1:start=0[freeze];" +
                "trim=start=$freezeSec[v2];[freeze][v2]concat=n=2"
    }

    private data class SpeedSegment(
        val startUs: Long,
        val endUs: Long,
        val speed: Float
    )

    private fun buildSegments(points: List<SpeedPoint>, totalDurationUs: Long): List<SpeedSegment> {
        if (points.size < 2) return listOf(SpeedSegment(0, totalDurationUs, points.firstOrNull()?.speed ?: 1f))
        val sorted = points.sortedBy { it.timeUs }
        val segments = mutableListOf<SpeedSegment>()
        for (i in 0 until sorted.size - 1) {
            segments.add(SpeedSegment(sorted[i].timeUs, sorted[i + 1].timeUs, sorted[i].speed))
        }
        val last = sorted.last()
        if (last.timeUs < totalDurationUs) {
            segments.add(SpeedSegment(last.timeUs, totalDurationUs, last.speed))
        }
        return segments
    }
}
