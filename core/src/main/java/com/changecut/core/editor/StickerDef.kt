package com.changecut.core.editor

data class StickerDef(
    val id: String,
    val name: String,
    val category: StickerCategory,
    val emoji: String = "",
    val assetPath: String? = null,
    val isAnimated: Boolean = false
)

enum class StickerCategory {
    EMOJI, SHAPE, ARROW, CALLOUT, BADGE, CUSTOM
}

data class AnimationDef(
    val id: String,
    val name: String,
    val type: AnimationType,
    val direction: AnimationDirection = AnimationDirection.NONE,
    val durationMs: Int = 500,
    val easing: EasingType = EasingType.EASE_OUT
)

enum class AnimationType {
    FADE, SLIDE, ZOOM, BOUNCE, ROTATE, BLUR, WOBBLE, FLIP, NONE
}

enum class AnimationDirection {
    NONE, LEFT, RIGHT, TOP, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

data class ColorGradeDef(
    val hslHue: Float = 0f,
    val hslSaturation: Float = 0f,
    val hslLightness: Float = 0f,
    val redCurve: List<Float> = listOf(0f, 0.5f, 1f),
    val greenCurve: List<Float> = listOf(0f, 0.5f, 1f),
    val blueCurve: List<Float> = listOf(0f, 0.5f, 1f),
    val rgbCurve: List<Float> = listOf(0f, 0.5f, 1f),
    val lutPath: String? = null,
    val lutIntensity: Float = 1f,
    val vignetteIntensity: Float = 0f,
    val autoColor: Boolean = false
)

data class AudioEQDef(
    val lowGain: Float = 0f,
    val midGain: Float = 0f,
    val highGain: Float = 0f,
    val lowFreq: Float = 250f,
    val midFreq: Float = 1000f,
    val highFreq: Float = 8000f,
    val compressorThreshold: Float = 0f,
    val compressorRatio: Float = 1f,
    val duckingAmount: Float = 0f,
    val limiterThreshold: Float = 0f,
    val limiterRelease: Float = 100f
)

object StickerCatalog {
    val emojis: List<StickerDef> = listOf(
        StickerDef("emoji_heart", "Heart", StickerCategory.EMOJI, "❤️"),
        StickerDef("emoji_fire", "Fire", StickerCategory.EMOJI, "🔥"),
        StickerDef("emoji_star", "Star", StickerCategory.EMOJI, "⭐"),
        StickerDef("emoji_crown", "Crown", StickerCategory.EMOJI, "👑"),
        StickerDef("emoji_rocket", "Rocket", StickerCategory.EMOJI, "🚀"),
        StickerDef("emoji_clap", "Clap", StickerCategory.EMOJI, "👏"),
        StickerDef("emoji_100", "100", StickerCategory.EMOJI, "💯"),
        StickerDef("emoji_ok", "OK", StickerCategory.EMOJI, "👌"),
        StickerDef("emoji_thumbsup", "Thumbs Up", StickerCategory.EMOJI, "👍"),
        StickerDef("emoji_thumbsdown", "Thumbs Down", StickerCategory.EMOJI, "👎"),
        StickerDef("emoji_laugh", "Laugh", StickerCategory.EMOJI, "😄"),
        StickerDef("emoji_cry", "Cry", StickerCategory.EMOJI, "😢"),
        StickerDef("emoji_angry", "Angry", StickerCategory.EMOJI, "😡"),
        StickerDef("emoji_love", "Love", StickerCategory.EMOJI, "😍"),
        StickerDef("emoji_wow", "Wow", StickerCategory.EMOJI, "😮"),
        StickerDef("emoji_money", "Money", StickerCategory.EMOJI, "💰"),
        StickerDef("emoji_music", "Music", StickerCategory.EMOJI, "🎵"),
        StickerDef("emoji_trophy", "Trophy", StickerCategory.EMOJI, "🏆"),
        StickerDef("emoji_target", "Target", StickerCategory.EMOJI, "🎯"),
        StickerDef("emoji_party", "Party", StickerCategory.EMOJI, "🎉"),
    )

    val shapes: List<StickerDef> = listOf(
        StickerDef("shape_circle", "Circle", StickerCategory.SHAPE, "○"),
        StickerDef("shape_square", "Square", StickerCategory.SHAPE, "□"),
        StickerDef("shape_triangle", "Triangle", StickerCategory.SHAPE, "△"),
        StickerDef("shape_diamond", "Diamond", StickerCategory.SHAPE, "◇"),
        StickerDef("shape_arrow_up", "Arrow Up", StickerCategory.ARROW, "↑"),
        StickerDef("shape_arrow_down", "Arrow Down", StickerCategory.ARROW, "↓"),
        StickerDef("shape_arrow_left", "Arrow Left", StickerCategory.ARROW, "←"),
        StickerDef("shape_arrow_right", "Arrow Right", StickerCategory.ARROW, "→"),
        StickerDef("shape_arrow_corner", "Corner Arrow", StickerCategory.ARROW, "↗"),
        StickerDef("shape_callout", "Callout", StickerCategory.CALLOUT, "💬"),
    )

    val allStickers: List<StickerDef> = emojis + shapes

    fun getByCategory(category: StickerCategory): List<StickerDef> =
        allStickers.filter { it.category == category }
}

object AnimationCatalog {
    val inAnimations: List<AnimationDef> = listOf(
        AnimationDef("fade_in", "Fade In", AnimationType.FADE),
        AnimationDef("slide_left_in", "Slide Left", AnimationType.SLIDE, AnimationDirection.LEFT),
        AnimationDef("slide_right_in", "Slide Right", AnimationType.SLIDE, AnimationDirection.RIGHT),
        AnimationDef("slide_top_in", "Slide Top", AnimationType.SLIDE, AnimationDirection.TOP),
        AnimationDef("slide_bottom_in", "Slide Bottom", AnimationType.SLIDE, AnimationDirection.BOTTOM),
        AnimationDef("zoom_in", "Zoom In", AnimationType.ZOOM),
        AnimationDef("bounce_in", "Bounce In", AnimationType.BOUNCE),
        AnimationDef("rotate_in", "Rotate In", AnimationType.ROTATE),
        AnimationDef("blur_in", "Blur In", AnimationType.BLUR),
        AnimationDef("wobble_in", "Wobble In", AnimationType.WOBBLE),
        AnimationDef("flip_in", "Flip In", AnimationType.FLIP),
    )

    val outAnimations: List<AnimationDef> = listOf(
        AnimationDef("fade_out", "Fade Out", AnimationType.FADE),
        AnimationDef("slide_left_out", "Slide Left", AnimationType.SLIDE, AnimationDirection.LEFT),
        AnimationDef("slide_right_out", "Slide Right", AnimationType.SLIDE, AnimationDirection.RIGHT),
        AnimationDef("slide_top_out", "Slide Top", AnimationType.SLIDE, AnimationDirection.TOP),
        AnimationDef("slide_bottom_out", "Slide Bottom", AnimationType.SLIDE, AnimationDirection.BOTTOM),
        AnimationDef("zoom_out", "Zoom Out", AnimationType.ZOOM),
        AnimationDef("bounce_out", "Bounce Out", AnimationType.BOUNCE),
        AnimationDef("rotate_out", "Rotate Out", AnimationType.ROTATE),
        AnimationDef("blur_out", "Blur Out", AnimationType.BLUR),
        AnimationDef("flip_out", "Flip Out", AnimationType.FLIP),
    )
}

enum class VideoEffectType {
    VHS, GLITCH, NEON, MIRROR, KALEIDOSCOPE, PIXELATE, MOSAIC, RIPPLE, SWIRL, LENS_BLUR
}

data class VideoEffectDef(
    val id: String,
    val type: VideoEffectType,
    val intensity: Float = 1f,
    val parameters: Map<String, Float> = emptyMap()
)

object VideoEffectCatalog {
    val allEffects: List<VideoEffectDef> = listOf(
        VideoEffectDef("vhs", VideoEffectType.VHS, 1f),
        VideoEffectDef("glitch", VideoEffectType.GLITCH, 1f),
        VideoEffectDef("neon", VideoEffectType.NEON, 1f),
        VideoEffectDef("mirror", VideoEffectType.MIRROR, 1f),
        VideoEffectDef("kaleidoscope", VideoEffectType.KALEIDOSCOPE, 1f),
        VideoEffectDef("pixelate", VideoEffectType.PIXELATE, 1f),
        VideoEffectDef("mosaic", VideoEffectType.MOSAIC, 1f)
    )

    val speedPresets: List<Pair<String, Float>> = listOf(
        "0.1x" to 0.1f,
        "0.25x" to 0.25f,
        "0.5x" to 0.5f,
        "0.75x" to 0.75f,
        "Normal" to 1f,
        "1.25x" to 1.25f,
        "1.5x" to 1.5f,
        "2x" to 2f,
        "3x" to 3f,
        "5x" to 5f,
        "10x" to 10f,
        "25x" to 25f,
        "50x" to 50f,
        "100x" to 100f
    )
}
