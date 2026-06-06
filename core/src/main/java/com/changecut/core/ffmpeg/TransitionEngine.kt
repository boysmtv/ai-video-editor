package com.changecut.core.ffmpeg

import android.content.Context
import com.changecut.core.editor.TransitionDefFull
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransitionEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ffmpegExecutor: FfmpegExecutor
) {
    private val cacheDir: File get() = File(context.cacheDir, "changecut_transitions")

    suspend fun applyTransition(
        clip1Path: String,
        clip2Path: String,
        transitionId: String,
        durationMs: Int,
        outputPath: String
    ): Result<String> {
        val def = transitions.find { it.id == transitionId } ?: return Result.failure(
            VideoException("Unknown transition: $transitionId")
        )
        val durationSec = (durationMs / 1000f).coerceIn(0.1f, 5f)
        val filterCommand = def.ffmpegFilter
            .replace(":duration=X", ":duration=$durationSec")
        return execute(
            "-i", clip1Path,
            "-i", clip2Path,
            "-filter_complex", filterCommand,
            "-c:a", "copy",
            "-y", outputPath
        ).map { outputPath }
    }

    suspend fun applyAllTransitions(
        clipPaths: List<String>,
        transitionIds: List<String>,
        durationsMs: List<Int>,
        outputPath: String
    ): Result<String> {
        if (clipPaths.size < 2) return Result.success(clipPaths.firstOrNull() ?: outputPath)
        if (!cacheDir.exists()) cacheDir.mkdirs()

        var currentPath = clipPaths.first()
        for (i in 1 until clipPaths.size) {
            val transitionId = transitionIds.getOrElse(i - 1) { "fade" }
            val durationMs = durationsMs.getOrElse(i - 1) { 500 }
            val intermediatePath = if (i == clipPaths.size - 1) {
                outputPath
            } else {
                File(cacheDir, "transition_$i.mp4").absolutePath
            }
            val result = applyTransition(currentPath, clipPaths[i], transitionId, durationMs, intermediatePath)
            if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
            currentPath = intermediatePath
        }
        return Result.success(currentPath)
    }

    fun getAllTransitions(): List<TransitionDefFull> = transitions

    private suspend fun execute(vararg args: String): Result<Unit> {
        val command = ffmpegExecutor.buildCommand(*args)
        return ffmpegExecutor.execute(command).map { }
    }

    companion object {
        val transitions: List<TransitionDefFull> = listOf(
            TransitionDefFull("fade", "Fade", "xfade=transition=fade:duration=X:offset=0"),
            TransitionDefFull("dissolve", "Dissolve", "xfade=transition=fade:duration=X:offset=0"),
            TransitionDefFull("slide_left", "Slide Left", "xfade=transition=slideleft:duration=X:offset=0"),
            TransitionDefFull("slide_right", "Slide Right", "xfade=transition=slideright:duration=X:offset=0"),
            TransitionDefFull("slide_up", "Slide Up", "xfade=transition=slideup:duration=X:offset=0"),
            TransitionDefFull("slide_down", "Slide Down", "xfade=transition=slidedown:duration=X:offset=0"),
            TransitionDefFull("wipe_left", "Wipe Left", "xfade=transition=wipeleft:duration=X:offset=0"),
            TransitionDefFull("wipe_right", "Wipe Right", "xfade=transition=wiperight:duration=X:offset=0"),
            TransitionDefFull("wipe_up", "Wipe Up", "xfade=transition=wipeup:duration=X:offset=0"),
            TransitionDefFull("wipe_down", "Wipe Down", "xfade=transition=wipedown:duration=X:offset=0"),
            TransitionDefFull("zoom_in", "Zoom In", "xfade=transition=zoomin:duration=X:offset=0"),
            TransitionDefFull("zoom_out", "Zoom Out", "xfade=transition=zoomout:duration=X:offset=0"),
            TransitionDefFull("radial", "Radial", "xfade=transition=radial:duration=X:offset=0"),
            TransitionDefFull("smooth_left", "Smooth Left", "xfade=transition=smoothleft:duration=X:offset=0"),
            TransitionDefFull("smooth_right", "Smooth Right", "xfade=transition=smoothright:duration=X:offset=0"),
            TransitionDefFull("smooth_up", "Smooth Up", "xfade=transition=smoothup:duration=X:offset=0"),
            TransitionDefFull("smooth_down", "Smooth Down", "xfade=transition=smoothdown:duration=X:offset=0"),
            TransitionDefFull("circle_open", "Circle Open", "xfade=transition=circleopen:duration=X:offset=0"),
            TransitionDefFull("circle_close", "Circle Close", "xfade=transition=circleclose:duration=X:offset=0"),
            TransitionDefFull("rect_open", "Rect Open", "xfade=transition=rectopen:duration=X:offset=0"),
            TransitionDefFull("rect_close", "Rect Close", "xfade=transition=rectclose:duration=X:offset=0"),
            TransitionDefFull("dissolve_soft", "Soft Dissolve", "xfade=transition=softdissolve:duration=X:offset=0"),
            TransitionDefFull("dissolve_pixel", "Pixel Dissolve", "xfade=transition=pixeldissolve:duration=X:offset=0"),
            TransitionDefFull("dissolve_h", "Horizontal Dissolve", "xfade=transition=hlslit:duration=X:offset=0"),
            TransitionDefFull("dissolve_v", "Vertical Dissolve", "xfade=transition=vlslit:duration=X:offset=0"),
            TransitionDefFull("cube_3d", "3D Cube", "xfade=transition=cube:duration=X:offset=0"),
            TransitionDefFull("page_curl_left", "Page Curl Left", "xfade=transition=pagecurl:duration=X:offset=0"),
            TransitionDefFull("page_curl_right", "Page Curl Right", "xfade=transition=pagecurl:duration=X:offset=0"),
            TransitionDefFull("ripple", "Ripple", "xfade=transition=ripple:duration=X:offset=0"),
            TransitionDefFull("swirl", "Swirl", "xfade=transition=swirl:duration=X:offset=0"),
            TransitionDefFull("squeeze_h", "Horiz Squeeze", "xfade=transition=hsqueeze:duration=X:offset=0"),
            TransitionDefFull("squeeze_v", "Vert Squeeze", "xfade=transition=vsqueeze:duration=X:offset=0"),
            TransitionDefFull("blur", "Blur", "xfade=transition=blur:duration=X:offset=0"),
            TransitionDefFull("glitch", "Glitch", "xfade=transition=glitch:duration=X:offset=0"),
            TransitionDefFull("burn", "Burn", "xfade=transition=burn:duration=X:offset=0"),
            TransitionDefFull("fade_grayscale", "Fade Grayscale", "xfade=transition=fadegrays:duration=X:offset=0"),
            TransitionDefFull("gradient_wipe", "Gradient Wipe", "xfade=transition=gradfade:duration=X:offset=0"),
            TransitionDefFull("door", "Door", "xfade=transition=door:duration=X:offset=0"),
            TransitionDefFull("pinwheel", "Pinwheel", "xfade=transition=pinwheel:duration=X:offset=0"),
            TransitionDefFull("cross_blur", "Cross Blur", "xfade=transition=diffuse:duration=X:offset=0"),
            TransitionDefFull("lens_flash", "Lens Flash", "xfade=transition=hlslit:duration=X:offset=0"),
            TransitionDefFull("matrix", "Matrix", "xfade=transition=slitlize:duration=X:offset=0"),
            TransitionDefFull("wedge", "Wedge", "xfade=transition=wedge:duration=X:offset=0"),
            TransitionDefFull("diamond", "Diamond", "xfade=transition=dissolve:duration=X:offset=0"),
            TransitionDefFull("water_drop", "Water Drop", "xfade=transition=droplet:duration=X:offset=0"),
            TransitionDefFull("inverted", "Inverted", "xfade=transition=fadeblack:duration=X:offset=0"),
            TransitionDefFull("wind", "Wind", "xfade=transition=wind:duration=X:offset=0"),
            TransitionDefFull("time_warp", "Time Warp", "xfade=transition=timewarp:duration=X:offset=0"),
            TransitionDefFull("kaleido", "Kaleidoscope", "xfade=transition=kaleidoscope:duration=X:offset=0"),
        )
    }
}
