package com.changecut.core.media

import com.changecut.core.editor.AnimationDef
import com.changecut.core.editor.AnimationDirection
import com.changecut.core.editor.AnimationType
import com.changecut.core.editor.AudioEQDef
import com.changecut.core.editor.BlendMode
import com.changecut.core.editor.ClipGroup
import com.changecut.core.editor.ColorGradeDef
import com.changecut.core.editor.EasingType
import com.changecut.core.editor.EditorClip
import com.changecut.core.editor.Keyframe
import com.changecut.core.editor.MaskDef
import com.changecut.core.editor.MaskType
import com.changecut.core.editor.StickerCategory
import com.changecut.core.editor.StickerDef
import com.changecut.core.editor.TextClipStyle
import com.changecut.core.editor.Track
import com.changecut.core.editor.TrackType
import com.changecut.core.editor.TransitionDef
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectTimelineStore @Inject constructor(
    private val mediaProjectManager: MediaProjectManager
) {
    fun save(projectId: String, tracks: List<Track>, groups: List<ClipGroup>) {
        save(projectId = projectId, tracks = tracks, groups = groups, zoomLevel = 1f, snapEnabled = true)
    }

    fun save(projectId: String, tracks: List<Track>, groups: List<ClipGroup>, zoomLevel: Float, snapEnabled: Boolean) {
        val file = mediaProjectManager.getTimelineStateFile(projectId)
        val json = JSONObject().apply {
            put("tracks", JSONArray(tracks.map { it.toJson() }))
            put("groups", JSONArray(groups.map { it.toJson() }))
            put("zoomLevel", zoomLevel.toDouble())
            put("snapEnabled", snapEnabled)
        }
        file.writeText(json.toString())
    }

    fun load(projectId: String): TimelineState? {
        val file = mediaProjectManager.getTimelineStateFile(projectId)
        if (!file.exists()) return null
        return runCatching {
            val json = JSONObject(file.readText())
            TimelineState(
                tracks = json.optJSONArray("tracks")?.toTrackList().orEmpty(),
                groups = json.optJSONArray("groups")?.toGroupList().orEmpty(),
                zoomLevel = json.optDouble("zoomLevel", 1.0).toFloat(),
                snapEnabled = json.optBoolean("snapEnabled", true)
            )
        }.getOrNull()
    }

    data class TimelineState(
        val tracks: List<Track>,
        val groups: List<ClipGroup>,
        val zoomLevel: Float,
        val snapEnabled: Boolean
    )

    private fun ClipGroup.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("isExpanded", isExpanded)
        put("clipIds", JSONArray(clipIds))
    }

    private fun Track.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type.name)
        put("label", label)
        put("isVisible", isVisible)
        put("isMuted", isMuted)
        put("isLocked", isLocked)
        put("clips", JSONArray(clips.map { it.toJson() }))
    }

    private fun EditorClip.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("sourceUri", sourceUri)
        put("label", label)
        put("contentRole", contentRole)
        put("startOffsetUs", startOffsetUs)
        put("endOffsetUs", endOffsetUs)
        put("trimStartUs", trimStartUs)
        put("trimEndUs", trimEndUs)
        put("volume", volume.toDouble())
        put("audioFadeInMs", audioFadeInMs)
        put("audioFadeOutMs", audioFadeOutMs)
        put("speed", speed.toDouble())
        put("rotation", rotation.toDouble())
        put("scaleX", scaleX.toDouble())
        put("scaleY", scaleY.toDouble())
        put("positionX", positionX.toDouble())
        put("positionY", positionY.toDouble())
        put("opacity", opacity.toDouble())
        put("textContent", textContent)
        put("textStyle", textStyle?.toJson())
        put("effectId", effectId)
        put("transitionIn", transitionIn?.toJson())
        put("transitionOut", transitionOut?.toJson())
        put("keyframes", JSONArray(keyframes.map { it.toJson() }))
        put("colorFilter", colorFilter)
        put("mask", mask?.toJson())
        put("blendMode", blendMode.name)
        put("sticker", sticker?.toJson())
        put("animationIn", animationIn?.toJson())
        put("animationOut", animationOut?.toJson())
        put("colorGrade", colorGrade?.toJson())
        put("audioEQ", audioEQ?.toJson())
        put("freezeDurationMs", freezeDurationMs)
    }

    private fun TextClipStyle.toJson(): JSONObject = JSONObject().apply {
        put("fontName", fontName)
        put("fontSize", fontSize.toDouble())
        put("color", color)
        put("backgroundColor", backgroundColor)
        put("alignment", alignment)
        put("bold", bold)
        put("italic", italic)
        put("shadow", shadow)
        put("outline", outline)
        put("outlineColor", outlineColor)
        put("animationIn", animationIn)
        put("animationOut", animationOut)
    }

    private fun TransitionDef.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type)
        put("durationMs", durationMs)
    }

    private fun Keyframe.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("property", property)
        put("timeUs", timeUs)
        put("value", value.toDouble())
        put("easing", easing.name)
    }

    private fun MaskDef.toJson(): JSONObject = JSONObject().apply {
        put("type", type.name)
        put("centerX", centerX.toDouble())
        put("centerY", centerY.toDouble())
        put("width", width.toDouble())
        put("height", height.toDouble())
        put("rotation", rotation.toDouble())
        put("featherPx", featherPx.toDouble())
        put("invert", invert)
        put("points", JSONArray(points.map { point ->
            JSONArray().apply {
                put(point.first.toDouble())
                put(point.second.toDouble())
            }
        }))
    }

    private fun StickerDef.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("category", category.name)
        put("emoji", emoji)
        put("assetPath", assetPath)
        put("isAnimated", isAnimated)
    }

    private fun AnimationDef.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("type", type.name)
        put("direction", direction.name)
        put("durationMs", durationMs)
        put("easing", easing.name)
    }

    private fun ColorGradeDef.toJson(): JSONObject = JSONObject().apply {
        put("hslHue", hslHue.toDouble())
        put("hslSaturation", hslSaturation.toDouble())
        put("hslLightness", hslLightness.toDouble())
        put("redCurve", JSONArray(redCurve.map { it.toDouble() }))
        put("greenCurve", JSONArray(greenCurve.map { it.toDouble() }))
        put("blueCurve", JSONArray(blueCurve.map { it.toDouble() }))
        put("rgbCurve", JSONArray(rgbCurve.map { it.toDouble() }))
        put("lutPath", lutPath)
        put("lutIntensity", lutIntensity.toDouble())
        put("vignetteIntensity", vignetteIntensity.toDouble())
        put("autoColor", autoColor)
    }

    private fun AudioEQDef.toJson(): JSONObject = JSONObject().apply {
        put("lowGain", lowGain.toDouble())
        put("midGain", midGain.toDouble())
        put("highGain", highGain.toDouble())
        put("lowFreq", lowFreq.toDouble())
        put("midFreq", midFreq.toDouble())
        put("highFreq", highFreq.toDouble())
        put("compressorThreshold", compressorThreshold.toDouble())
        put("compressorRatio", compressorRatio.toDouble())
        put("duckingAmount", duckingAmount.toDouble())
    }

    private fun JSONArray.toGroupList(): List<ClipGroup> =
        (0 until length()).map { index ->
            val json = getJSONObject(index)
            ClipGroup(
                id = json.getString("id"),
                clipIds = json.optJSONArray("clipIds")?.toStringList().orEmpty(),
                name = json.optString("name", "Group"),
                isExpanded = json.optBoolean("isExpanded", true)
            )
        }

    private fun JSONArray.toTrackList(): List<Track> =
        (0 until length()).map { getJSONObject(it).toTrack() }

    private fun JSONObject.toTrack(): Track = Track(
        id = getString("id"),
        type = TrackType.valueOf(optString("type", TrackType.VIDEO.name)),
        label = optString("label", "Track"),
        clips = optJSONArray("clips")?.toClipList().orEmpty(),
        isVisible = optBoolean("isVisible", true),
        isMuted = optBoolean("isMuted", false),
        isLocked = optBoolean("isLocked", false)
    )

    private fun JSONArray.toClipList(): List<EditorClip> =
        (0 until length()).map { getJSONObject(it).toClip() }

    private fun JSONObject.toClip(): EditorClip = EditorClip(
        id = getString("id"),
        sourceUri = optString("sourceUri", ""),
        label = optString("label", "Clip"),
        contentRole = optString("contentRole").takeIf { it.isNotBlank() },
        startOffsetUs = optLong("startOffsetUs", 0L),
        endOffsetUs = optLong("endOffsetUs", 0L),
        trimStartUs = optLong("trimStartUs", 0L),
        trimEndUs = optLong("trimEndUs", 0L),
        volume = optDouble("volume", 1.0).toFloat(),
        audioFadeInMs = optInt("audioFadeInMs", 0),
        audioFadeOutMs = optInt("audioFadeOutMs", 0),
        speed = optDouble("speed", 1.0).toFloat(),
        rotation = optDouble("rotation", 0.0).toFloat(),
        scaleX = optDouble("scaleX", 1.0).toFloat(),
        scaleY = optDouble("scaleY", 1.0).toFloat(),
        positionX = optDouble("positionX", 0.0).toFloat(),
        positionY = optDouble("positionY", 0.0).toFloat(),
        opacity = optDouble("opacity", 1.0).toFloat(),
        textContent = optString("textContent").takeIf { it.isNotBlank() },
        textStyle = optJSONObject("textStyle")?.toTextClipStyle(),
        effectId = optString("effectId").takeIf { it.isNotBlank() },
        transitionIn = optJSONObject("transitionIn")?.toTransitionDef(),
        transitionOut = optJSONObject("transitionOut")?.toTransitionDef(),
        keyframes = optJSONArray("keyframes")?.toKeyframeList().orEmpty(),
        colorFilter = optString("colorFilter").takeIf { it.isNotBlank() },
        mask = optJSONObject("mask")?.toMaskDef(),
        blendMode = runCatching { BlendMode.valueOf(optString("blendMode", BlendMode.NORMAL.name)) }.getOrDefault(BlendMode.NORMAL),
        sticker = optJSONObject("sticker")?.toStickerDef(),
        animationIn = optJSONObject("animationIn")?.toAnimationDef(),
        animationOut = optJSONObject("animationOut")?.toAnimationDef(),
        colorGrade = optJSONObject("colorGrade")?.toColorGradeDef(),
        audioEQ = optJSONObject("audioEQ")?.toAudioEQDef(),
        freezeDurationMs = optInt("freezeDurationMs", 0)
    )

    private fun JSONObject.toTextClipStyle(): TextClipStyle = TextClipStyle(
        fontName = optString("fontName", "Default"),
        fontSize = optDouble("fontSize", 24.0).toFloat(),
        color = optLong("color", 0xFFFFFFFFL),
        backgroundColor = if (has("backgroundColor") && !isNull("backgroundColor")) optLong("backgroundColor") else null,
        alignment = optInt("alignment", 1),
        bold = optBoolean("bold", false),
        italic = optBoolean("italic", false),
        shadow = optBoolean("shadow", false),
        outline = optBoolean("outline", false),
        outlineColor = optLong("outlineColor", 0xFF000000L),
        animationIn = optString("animationIn").takeIf { it.isNotBlank() },
        animationOut = optString("animationOut").takeIf { it.isNotBlank() }
    )

    private fun JSONObject.toTransitionDef(): TransitionDef = TransitionDef(
        id = getString("id"),
        type = optString("type", "fade"),
        durationMs = optInt("durationMs", 300)
    )

    private fun JSONArray.toKeyframeList(): List<Keyframe> =
        (0 until length()).map { index ->
            val json = getJSONObject(index)
            Keyframe(
                id = json.getString("id"),
                property = json.getString("property"),
                timeUs = json.optLong("timeUs", 0L),
                value = json.optDouble("value", 0.0).toFloat(),
                easing = runCatching { EasingType.valueOf(json.optString("easing", EasingType.LINEAR.name)) }.getOrDefault(EasingType.LINEAR)
            )
        }

    private fun JSONObject.toMaskDef(): MaskDef = MaskDef(
        type = runCatching { MaskType.valueOf(optString("type", MaskType.RECTANGLE.name)) }.getOrDefault(MaskType.RECTANGLE),
        centerX = optDouble("centerX", 0.5).toFloat(),
        centerY = optDouble("centerY", 0.5).toFloat(),
        width = optDouble("width", 0.8).toFloat(),
        height = optDouble("height", 0.8).toFloat(),
        rotation = optDouble("rotation", 0.0).toFloat(),
        featherPx = optDouble("featherPx", 0.0).toFloat(),
        invert = optBoolean("invert", false),
        points = optJSONArray("points")?.let { array ->
            (0 until array.length()).mapNotNull { pointIndex ->
                val pair = array.optJSONArray(pointIndex) ?: return@mapNotNull null
                pair.optDouble(0, 0.0).toFloat() to pair.optDouble(1, 0.0).toFloat()
            }
        }.orEmpty()
    )

    private fun JSONObject.toStickerDef(): StickerDef = StickerDef(
        id = getString("id"),
        name = optString("name", "Sticker"),
        category = runCatching { StickerCategory.valueOf(optString("category", StickerCategory.EMOJI.name)) }.getOrDefault(StickerCategory.EMOJI),
        emoji = optString("emoji", ""),
        assetPath = optString("assetPath").takeIf { it.isNotBlank() },
        isAnimated = optBoolean("isAnimated", false)
    )

    private fun JSONObject.toAnimationDef(): AnimationDef = AnimationDef(
        id = getString("id"),
        name = optString("name", "Animation"),
        type = runCatching { AnimationType.valueOf(optString("type", AnimationType.NONE.name)) }.getOrDefault(AnimationType.NONE),
        direction = runCatching { AnimationDirection.valueOf(optString("direction", AnimationDirection.NONE.name)) }.getOrDefault(AnimationDirection.NONE),
        durationMs = optInt("durationMs", 500),
        easing = runCatching { EasingType.valueOf(optString("easing", EasingType.EASE_OUT.name)) }.getOrDefault(EasingType.EASE_OUT)
    )

    private fun JSONObject.toColorGradeDef(): ColorGradeDef = ColorGradeDef(
        hslHue = optDouble("hslHue", 0.0).toFloat(),
        hslSaturation = optDouble("hslSaturation", 0.0).toFloat(),
        hslLightness = optDouble("hslLightness", 0.0).toFloat(),
        redCurve = optJSONArray("redCurve")?.toFloatList() ?: listOf(0f, 0.5f, 1f),
        greenCurve = optJSONArray("greenCurve")?.toFloatList() ?: listOf(0f, 0.5f, 1f),
        blueCurve = optJSONArray("blueCurve")?.toFloatList() ?: listOf(0f, 0.5f, 1f),
        rgbCurve = optJSONArray("rgbCurve")?.toFloatList() ?: listOf(0f, 0.5f, 1f),
        lutPath = optString("lutPath").takeIf { it.isNotBlank() },
        vignetteIntensity = optDouble("vignetteIntensity", 0.0).toFloat(),
        autoColor = optBoolean("autoColor", false)
    )

    private fun JSONObject.toAudioEQDef(): AudioEQDef = AudioEQDef(
        lowGain = optDouble("lowGain", 0.0).toFloat(),
        midGain = optDouble("midGain", 0.0).toFloat(),
        highGain = optDouble("highGain", 0.0).toFloat(),
        lowFreq = optDouble("lowFreq", 250.0).toFloat(),
        midFreq = optDouble("midFreq", 1000.0).toFloat(),
        highFreq = optDouble("highFreq", 8000.0).toFloat(),
        compressorThreshold = optDouble("compressorThreshold", 0.0).toFloat(),
        compressorRatio = optDouble("compressorRatio", 1.0).toFloat(),
        duckingAmount = optDouble("duckingAmount", 0.0).toFloat()
    )

    private fun JSONArray.toFloatList(): List<Float> =
        (0 until length()).map { optDouble(it, 0.0).toFloat() }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).mapNotNull { optString(it).takeIf { value -> value.isNotBlank() } }
}
