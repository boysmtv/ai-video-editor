package com.changecut.core.ffmpeg

import com.changecut.core.editor.StickerDef
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StickerEngine @Inject constructor() {

    fun buildOverlayFilter(
        imagePath: String,
        x: Float,
        y: Float,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Float = 0f,
        opacity: Float = 1f,
        width: Int = 1920,
        height: Int = 1080
    ): String {
        val parts = mutableListOf<String>()
        val scaledW = (width * scaleX).toInt()
        val scaledH = (height * scaleY).toInt()
        val posX = (x * width).toInt()
        val posY = (y * height).toInt()

        parts.add("movie='$imagePath'")
        if (scaledW > 0 && scaledH > 0) {
            parts.add("scale=$scaledW:$scaledH")
        }
        if (rotation != 0f) {
            parts.add("rotate=${rotation * kotlin.math.PI / 180}:ow=rotw(${rotation * kotlin.math.PI / 180}):oh=roth(${rotation * kotlin.math.PI / 180})")
        }
        if (opacity < 1f) {
            parts.add("format=rgba,colorchannelmixer=aa=${opacity}")
        }
        parts.add("overlay=$posX:$posY")
        return parts.joinToString(",")
    }

    fun buildEmojiFilter(
        emoji: String,
        fontSize: Int = 64,
        x: Float,
        y: Float,
        width: Int = 1920,
        height: Int = 1080
    ): String {
        val posX = (x * width).toInt()
        val posY = (y * height).toInt()
        return "drawtext=text='$emoji':fontsize=$fontSize:x=$posX:y=$posY:fontcolor=white"
    }

    fun buildMultiOverlay(
        overlays: List<OverlayDef>
    ): String {
        if (overlays.isEmpty()) return ""
        return overlays.joinToString(";") { it.toFilterString() }
    }

    data class OverlayDef(
        val type: OverlayType,
        val imagePath: String? = null,
        val emoji: String? = null,
        val x: Float = 0.5f,
        val y: Float = 0.5f,
        val scale: Float = 0.15f,
        val rotation: Float = 0f,
        val opacity: Float = 1f
    ) {
        enum class OverlayType { IMAGE, EMOJI, TEXT }

        fun toFilterString(): String = when (type) {
            OverlayType.IMAGE -> "[1:v]scale=${(1920 * scale).toInt()}:-1[ovr];[0:v][ovr]overlay=${(1920 * x).toInt()}:${(1080 * y).toInt()}"
            OverlayType.EMOJI -> "drawtext=text='$emoji':fontsize=${(64 * scale).toInt()}:x=${(1920 * x).toInt()}:y=${(1080 * y).toInt()}:fontcolor=white@$opacity"
            OverlayType.TEXT -> "drawtext=text='$emoji':fontsize=${(64 * scale).toInt()}:x=${(1920 * x).toInt()}:y=${(1080 * y).toInt()}:fontcolor=white@$opacity"
        }
    }
}
