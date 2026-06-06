package com.changecut.core.editor

import androidx.compose.runtime.Immutable

enum class KeyframeProperty {
    POSITION_X,
    POSITION_Y,
    SCALE_X,
    SCALE_Y,
    ROTATION,
    OPACITY,
    VOLUME
}

@Immutable
data class KeyframePoint(
    val timeUs: Long,
    val value: Float,
    val easing: EasingType = EasingType.LINEAR
)

@Immutable
data class AnimatedValue(
    val timeUs: Long,
    val properties: Map<KeyframeProperty, Float>
)

object KeyframeSystem {

    fun interpolate(keyframes: List<KeyframePoint>, timeUs: Long): Float {
        if (keyframes.isEmpty()) return 0f
        if (keyframes.size == 1) return keyframes.first().value

        val sorted = keyframes.sortedBy { it.timeUs }
        if (timeUs <= sorted.first().timeUs) return sorted.first().value
        if (timeUs >= sorted.last().timeUs) return sorted.last().value

        val before = sorted.lastOrNull { it.timeUs <= timeUs } ?: sorted.first()
        val after = sorted.firstOrNull { it.timeUs > timeUs } ?: sorted.last()

        if (before.timeUs == after.timeUs) return before.value

        val t = (timeUs - before.timeUs).toFloat() / (after.timeUs - before.timeUs)
        val easedT = applyEasing(t, after.easing)

        return before.value + (after.value - before.value) * easedT
    }

    fun animatePosition(
        keyframesX: List<KeyframePoint>,
        keyframesY: List<KeyframePoint>,
        timeUs: Long
    ): Pair<Float, Float> {
        return Pair(
            interpolate(keyframesX, timeUs),
            interpolate(keyframesY, timeUs)
        )
    }

    fun animateScale(
        keyframesX: List<KeyframePoint>,
        keyframesY: List<KeyframePoint>,
        timeUs: Long
    ): Pair<Float, Float> {
        return Pair(
            interpolate(keyframesX, timeUs),
            interpolate(keyframesY, timeUs)
        )
    }

    private fun applyEasing(t: Float, easing: EasingType): Float {
        val clamped = t.coerceIn(0f, 1f)
        return when (easing) {
            EasingType.LINEAR -> clamped
            EasingType.EASE_IN -> clamped * clamped * clamped
            EasingType.EASE_OUT -> {
                val o = 1f - clamped
                1f - o * o * o
            }
            EasingType.EASE_IN_OUT -> {
                if (clamped < 0.5f) {
                    4f * clamped * clamped * clamped
                } else {
                    val o = 2f * clamped - 2f
                    1f + o * o * o
                }
            }
        }
    }
}
