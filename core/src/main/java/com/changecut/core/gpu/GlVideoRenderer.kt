package com.changecut.core.gpu

import android.opengl.GLES11Ext
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.changecut.core.editor.EditorClip
import com.changecut.core.editor.MaskType
import com.changecut.core.editor.Track
import com.changecut.core.editor.TrackType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlVideoRenderer @Inject constructor() : GLSurfaceView.Renderer {

    private var viewWidth = 0
    private var viewHeight = 0
    private val quad = GlShaderProgram.createFullscreenQuad()
    private var oesShader: GlShaderProgram? = null
    private var tracks: List<Track> = emptyList()
    private var currentTimeUs: Long = 0L
    private var isPlaying: Boolean = false

    private var decoders = mutableMapOf<String, VideoFrameDecoder>()
    private var activeLayers: List<RenderLayer> = emptyList()
    private var videoReady = false

    private val OES_TEXTURE_UNIT = GLES31.GL_TEXTURE0
    private val OES_TEXTURE_UNIT_INDEX = 0
    private val transformMatrix = FloatArray(16)
    private val maxActiveLayers = 4

    private data class RenderLayer(
        val clip: EditorClip,
        val decoder: VideoFrameDecoder,
        val relativeTimeUs: Long,
        val opacity: Float
    )

    fun updateTimeline(newTracks: List<Track>, timeUs: Long, playing: Boolean) {
        tracks = newTracks
        currentTimeUs = timeUs
        isPlaying = playing

        manageDecoders(newTracks)
        scheduleFrame(timeUs)
    }

    private fun manageDecoders(newTracks: List<Track>) {
        val visualTracks = newTracks.filter {
            it.isVisible && (it.type == TrackType.VIDEO || it.type == TrackType.OVERLAY || it.type == TrackType.STICKER)
        }
        val neededIds = visualTracks.flatMap { it.clips }.map { it.id }.toSet()
        val currentIds = decoders.keys.toSet()

        (currentIds - neededIds).forEach { unloadClip(it) }
        visualTracks.flatMap { it.clips }.forEach { clip ->
            if (clip.id !in decoders && clip.sourceUri.isNotEmpty()) {
                loadClip(clip.sourceUri, clip.id)
            }
        }
    }

    private fun loadClip(sourceUri: String, clipId: String) {
        try {
            val decoder = VideoFrameDecoder(sourceUri)
            decoder.prepare()
            decoders[clipId] = decoder
        } catch (_: Exception) { }
    }

    private fun unloadClip(clipId: String) {
        decoders.remove(clipId)?.release()
    }

    private fun scheduleFrame(timeUs: Long) {
        val layers = findActiveVisualClips(timeUs)
            .mapNotNull { clip ->
                val decoder = decoders[clip.id] ?: return@mapNotNull null
                val sourceTimeUs = resolveSourceTimeUs(clip, timeUs, decoder.durationUs)
                decoder.seekTo(sourceTimeUs)
                decoder.renderFrame(sourceTimeUs)
                RenderLayer(
                    clip = clip,
                    decoder = decoder,
                    relativeTimeUs = (timeUs - clip.startOffsetUs).coerceAtLeast(0L),
                    opacity = resolveOpacity(clip, timeUs).coerceIn(0f, 1f)
                )
            }
            .filter { it.opacity > 0f }

        activeLayers = layers
        videoReady = layers.isNotEmpty()
    }

    private fun findActiveVisualClips(timeUs: Long): List<EditorClip> {
        val visualTracks = tracks
            .withIndex()
            .filter { (_, track) ->
                track.isVisible && (track.type == TrackType.VIDEO || track.type == TrackType.OVERLAY || track.type == TrackType.STICKER)
            }

        return visualTracks
            .mapNotNull { (trackIndex, track) ->
                track.clips
                    .sortedByDescending { it.startOffsetUs }
                    .firstOrNull { clip ->
                        timeUs in clip.startOffsetUs until clip.endOffsetUs && clip.sourceUri.isNotBlank()
                    }
                    ?.let { clip -> trackIndex to clip }
            }
            .sortedBy { it.first }
            .map { it.second }
            .takeLast(maxActiveLayers)
    }

    private fun resolveSourceTimeUs(
        clip: EditorClip,
        timelineTimeUs: Long,
        decoderDurationUs: Long
    ): Long {
        val timelineElapsedUs = (timelineTimeUs - clip.startOffsetUs).coerceAtLeast(0L)
        val sourceElapsedUs = (timelineElapsedUs * clip.speed.coerceAtLeast(0.01f)).toLong()
        val trimStartUs = clip.trimStartUs.coerceAtLeast(0L)
        val trimEndUs = if (clip.trimEndUs > trimStartUs) clip.trimEndUs else decoderDurationUs.coerceAtLeast(trimStartUs)
        return (trimStartUs + sourceElapsedUs).coerceIn(trimStartUs, trimEndUs.coerceAtLeast(trimStartUs))
    }

    override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        GLES31.glClearColor(0f, 0f, 0f, 1f)
        GLES31.glDisable(GLES31.GL_DEPTH_TEST)
        GLES31.glEnable(GLES31.GL_BLEND)
        GLES31.glBlendFunc(GLES31.GL_SRC_ALPHA, GLES31.GL_ONE_MINUS_SRC_ALPHA)

        oesShader = GlShaderProgram(
            vertexSource = """
                #version 300 es
                in vec4 aPosition;
                in vec2 aTexCoord;
                out vec2 vTexCoord;
                uniform mat4 uTransform;
                void main() {
                    gl_Position = uTransform * aPosition;
                    vTexCoord = aTexCoord;
                }
            """.trimIndent(),
            fragmentSource = """
                #version 300 es
                #extension GL_OES_EGL_image_external_essl3 : require
                precision highp float;
                in vec2 vTexCoord;
                out vec4 fragColor;
                uniform samplerExternalOES uTexture;
                uniform float uOpacity;
                uniform int uMaskType;
                uniform vec2 uMaskCenter;
                uniform vec2 uMaskSize;
                uniform float uMaskFeather;
                uniform float uMaskRotation;
                uniform float uMaskInvert;
                uniform int uUseChromaKey;
                uniform vec3 uChromaColor;
                uniform float uChromaSimilarity;
                uniform float uChromaSmoothness;
                uniform float uChromaSpill;
                uniform float uHue;
                uniform float uSaturation;
                uniform float uLightness;
                uniform float uVignette;
                void main() {
                    vec4 color = texture(uTexture, vTexCoord);
                    if (uUseChromaKey == 1) {
                        float diff = length(color.rgb - uChromaColor);
                        float chromaMask = smoothstep(uChromaSimilarity, uChromaSimilarity + uChromaSmoothness, diff);
                        color.rgb = mix(color.rgb, vec3(0.0), (1.0 - chromaMask) * uChromaSpill);
                        color = vec4(color.rgb * chromaMask, color.a * chromaMask);
                    }

                    float c = cos(uHue * 3.14159);
                    float s = sin(uHue * 3.14159);
                    mat3 hueRot = mat3(
                        0.299 + 0.701*c + 0.168*s, 0.587 - 0.587*c + 0.330*s, 0.114 - 0.114*c - 0.497*s,
                        0.299 - 0.299*c - 0.328*s, 0.587 + 0.413*c + 0.035*s, 0.114 - 0.114*c + 0.292*s,
                        0.299 - 0.299*c + 1.25*s, 0.587 - 0.587*c - 1.05*s, 0.114 + 0.886*c - 0.203*s
                    );
                    color.rgb = hueRot * color.rgb;
                    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                    color.rgb = mix(vec3(gray), color.rgb, clamp(1.0 + uSaturation, 0.0, 2.0));
                    color.rgb = clamp(color.rgb * (1.0 + uLightness * 0.5), 0.0, 1.0);
                    float vignetteDist = distance(vTexCoord, vec2(0.5, 0.5)) * 2.0;
                    float vignette = 1.0 - clamp(uVignette, 0.0, 1.0) * vignetteDist * vignetteDist;
                    color.rgb *= vignette;

                    float maskVal = 1.0;
                    if (uMaskType == 1) {
                        vec2 pos = vTexCoord - uMaskCenter;
                        vec2 halfSize = uMaskSize * 0.5;
                        vec2 dist = abs(pos) - halfSize;
                        maskVal = 1.0 - smoothstep(-uMaskFeather * 0.002, uMaskFeather * 0.002, max(dist.x, dist.y));
                    } else if (uMaskType == 2) {
                        vec2 dir = vec2(cos(uMaskRotation), sin(uMaskRotation));
                        float proj = dot(vTexCoord - uMaskCenter, dir);
                        maskVal = 1.0 - smoothstep(-uMaskFeather * 0.002, uMaskFeather * 0.002, proj);
                    } else if (uMaskType == 3) {
                        float dist = distance(vTexCoord, uMaskCenter);
                        float radius = uMaskSize.x * 0.5;
                        maskVal = 1.0 - smoothstep(radius - uMaskFeather * 0.002, radius + uMaskFeather * 0.002, dist);
                    }
                    maskVal = mix(maskVal, 1.0 - maskVal, uMaskInvert);
                    fragColor = vec4(color.rgb, color.a * uOpacity * maskVal);
                }
            """.trimIndent()
        )
    }

    override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, w: Int, h: Int) {
        viewWidth = w; viewHeight = h
        GLES31.glViewport(0, 0, w, h)
    }

    override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)

        if (videoReady && activeLayers.isNotEmpty()) {
            val shader = oesShader ?: return
            shader.use()
            shader.setInt("uTexture", OES_TEXTURE_UNIT_INDEX)
            activeLayers.forEach { layer ->
                val textureId = layer.decoder.textureId
                if (textureId < 0) return@forEach
                shader.setMat4("uTransform", buildTransformMatrix(layer.clip, layer.relativeTimeUs))
                shader.setFloat("uOpacity", layer.opacity)
                bindClipVisualState(shader, layer.clip)

                GLES31.glActiveTexture(OES_TEXTURE_UNIT)
                GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
                drawQuad(shader)
            }
        }
    }

    private fun drawQuad(shader: GlShaderProgram) {
        val posLoc = shader.getAttrib("aPosition")
        val texLoc = shader.getAttrib("aTexCoord")

        GLES31.glEnableVertexAttribArray(posLoc)
        GLES31.glEnableVertexAttribArray(texLoc)

        quad.position(0)
        GLES31.glVertexAttribPointer(posLoc, 2, GLES31.GL_FLOAT, false, 16, quad)
        quad.position(2)
        GLES31.glVertexAttribPointer(texLoc, 2, GLES31.GL_FLOAT, false, 16, quad)

        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4)

        GLES31.glDisableVertexAttribArray(posLoc)
        GLES31.glDisableVertexAttribArray(texLoc)
    }

    fun releaseAll() {
        decoders.values.forEach { it.release() }
        decoders.clear()
        oesShader?.delete()
        activeLayers = emptyList()
        videoReady = false
    }

    private fun buildTransformMatrix(clip: EditorClip, relativeTimeUs: Long): FloatArray {
        Matrix.setIdentityM(transformMatrix, 0)
        val positionX = resolveAnimatedValue(clip, "POSITION_X", relativeTimeUs, clip.positionX).coerceIn(0f, 1f)
        val positionY = resolveAnimatedValue(clip, "POSITION_Y", relativeTimeUs, clip.positionY).coerceIn(0f, 1f)
        val rotation = resolveAnimatedValue(clip, "ROTATION", relativeTimeUs, clip.rotation)
        val scaleX = resolveAnimatedValue(clip, "SCALE_X", relativeTimeUs, clip.scaleX).coerceIn(0.05f, 4f)
        val scaleY = resolveAnimatedValue(clip, "SCALE_Y", relativeTimeUs, clip.scaleY).coerceIn(0.05f, 4f)
        val translateX = ((positionX - 0.5f) * 2f)
        val translateY = ((0.5f - positionY) * 2f)
        Matrix.translateM(transformMatrix, 0, translateX, translateY, 0f)
        Matrix.rotateM(transformMatrix, 0, rotation, 0f, 0f, 1f)
        Matrix.scaleM(transformMatrix, 0, scaleX, scaleY, 1f)
        return transformMatrix.copyOf()
    }

    private fun resolveOpacity(clip: EditorClip, timeUs: Long): Float {
        val relativeTimeUs = (timeUs - clip.startOffsetUs).coerceAtLeast(0L)
        val keyedOpacity = resolveAnimatedValue(clip, "OPACITY", relativeTimeUs, clip.opacity)
        return keyedOpacity * resolveTransitionOpacity(clip, relativeTimeUs)
    }

    private fun resolveAnimatedValue(
        clip: EditorClip,
        property: String,
        relativeTimeUs: Long,
        fallback: Float
    ): Float {
        val propertyKeyframes = clip.keyframes
            .filter { it.property.equals(property, ignoreCase = true) }
            .sortedBy { it.timeUs }
        if (propertyKeyframes.isEmpty()) return fallback
        if (propertyKeyframes.size == 1) return propertyKeyframes.first().value

        val next = propertyKeyframes.firstOrNull { it.timeUs >= relativeTimeUs } ?: return propertyKeyframes.last().value
        val prev = propertyKeyframes.lastOrNull { it.timeUs <= relativeTimeUs } ?: return next.value
        if (prev.id == next.id || next.timeUs == prev.timeUs) return next.value

        val progress = ((relativeTimeUs - prev.timeUs).toFloat() / (next.timeUs - prev.timeUs).toFloat())
            .coerceIn(0f, 1f)
        return prev.value + ((next.value - prev.value) * progress)
    }

    private fun resolveTransitionOpacity(clip: EditorClip, relativeTimeUs: Long): Float {
        val clipDurationUs = clip.durationUs.coerceAtLeast(1L)
        val inFactor = clip.transitionIn
            ?.takeIf { it.type.contains("fade", ignoreCase = true) || it.type.contains("dissolve", ignoreCase = true) }
            ?.let { transition ->
                val durationUs = (transition.durationMs.coerceAtLeast(1) * 1000L).coerceAtMost(clipDurationUs)
                (relativeTimeUs.toFloat() / durationUs.toFloat()).coerceIn(0f, 1f)
            } ?: 1f
        val outFactor = clip.transitionOut
            ?.takeIf { it.type.contains("fade", ignoreCase = true) || it.type.contains("dissolve", ignoreCase = true) }
            ?.let { transition ->
                val durationUs = (transition.durationMs.coerceAtLeast(1) * 1000L).coerceAtMost(clipDurationUs)
                ((clipDurationUs - relativeTimeUs).toFloat() / durationUs.toFloat()).coerceIn(0f, 1f)
            } ?: 1f
        return minOf(inFactor, outFactor)
    }

    private fun bindClipVisualState(shader: GlShaderProgram, clip: EditorClip) {
        val mask = clip.mask
        val maskType = when (mask?.type) {
            MaskType.RECTANGLE -> 1
            MaskType.LINEAR -> 2
            MaskType.RADIAL -> 3
            else -> 0
        }
        shader.setInt("uMaskType", maskType)
        shader.setVec2("uMaskCenter", mask?.centerX ?: 0.5f, mask?.centerY ?: 0.5f)
        shader.setVec2("uMaskSize", mask?.width ?: 1f, mask?.height ?: 1f)
        shader.setFloat("uMaskFeather", mask?.featherPx ?: 0f)
        shader.setFloat("uMaskRotation", (((mask?.rotation ?: 0f) * Math.PI) / 180.0).toFloat())
        shader.setFloat("uMaskInvert", if (mask?.invert == true) 1f else 0f)

        val chromaSettings = parseChromaKeySettings(clip.colorFilter)
        val chromaColor = chromaSettings?.first
        shader.setInt("uUseChromaKey", if (clip.effectId == "chromakey" && chromaColor != null) 1 else 0)
        shader.setVec3(
            "uChromaColor",
            chromaColor?.getOrNull(0) ?: 0f,
            chromaColor?.getOrNull(1) ?: 1f,
            chromaColor?.getOrNull(2) ?: 0f
        )
        shader.setFloat("uChromaSimilarity", chromaSettings?.second ?: 0.4f)
        shader.setFloat("uChromaSmoothness", chromaSettings?.third ?: 0.1f)
        shader.setFloat("uChromaSpill", 0.1f)

        val grade = clip.colorGrade
        shader.setFloat("uHue", grade?.hslHue ?: 0f)
        shader.setFloat("uSaturation", grade?.hslSaturation ?: 0f)
        shader.setFloat("uLightness", grade?.hslLightness ?: 0f)
        shader.setFloat("uVignette", grade?.vignetteIntensity ?: 0f)
    }

    private fun parseChromaKeySettings(colorFilter: String?): Triple<FloatArray, Float, Float>? {
        val raw = colorFilter
            ?.takeIf { it.startsWith("chromakey:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?: return null
        val parts = raw.split(":")
        val colorRaw = parts.firstOrNull()
            ?.removePrefix("#")
            ?.removePrefix("0x")
            ?.removePrefix("0X")
            ?.trim()
            ?: return null
        if (colorRaw.length != 6) return null
        return runCatching {
            Triple(
                floatArrayOf(
                    colorRaw.substring(0, 2).toInt(16) / 255f,
                    colorRaw.substring(2, 4).toInt(16) / 255f,
                    colorRaw.substring(4, 6).toInt(16) / 255f
                ),
                parts.getOrNull(1)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.4f,
                parts.getOrNull(2)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.1f
            )
        }.getOrNull()
    }
}
