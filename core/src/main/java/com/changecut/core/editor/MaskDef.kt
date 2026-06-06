package com.changecut.core.editor

enum class MaskType {
    LINEAR, RADIAL, RECTANGLE, HEART, STAR, CUSTOM
}

enum class BlendMode(
    val displayName: String,
    val ffmpegName: String
) {
    NORMAL("Normal", "normal"),
    SCREEN("Screen", "screen"),
    MULTIPLY("Multiply", "multiply"),
    OVERLAY("Overlay", "overlay"),
    ADD("Add", "addition"),
    DARKEN("Darken", "darken"),
    LIGHTEN("Lighten", "lighten"),
    DIFFERENCE("Difference", "difference"),
    HARD_LIGHT("Hard Light", "hardlight"),
    SOFT_LIGHT("Soft Light", "softlight"),
    EXCLUSION("Exclusion", "exclusion"),
    DIVIDE("Divide", "divide")
}

data class MaskDef(
    val type: MaskType = MaskType.RECTANGLE,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val width: Float = 0.8f,
    val height: Float = 0.8f,
    val rotation: Float = 0f,
    val featherPx: Float = 0f,
    val invert: Boolean = false,
    val points: List<Pair<Float, Float>> = emptyList()
)
