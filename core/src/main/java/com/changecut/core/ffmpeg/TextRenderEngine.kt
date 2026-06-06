package com.changecut.core.ffmpeg

import android.content.Context
import com.changecut.core.editor.TextClipStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextRenderEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ffmpegExecutor: FfmpegExecutor
) {
    suspend fun renderText(
        videoPath: String,
        text: String,
        outputPath: String,
        style: TextClipStyle
    ): Result<String> {
        val escaped = text
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace(":", "\\:")
        val fontSize = style.fontSize.toInt()
        val colorHex = String.format("%06X", style.color and 0xFFFFFF)
        val shadowFilter = if (style.shadow) ":shadowx=2:shadowy=2:shadowcolor=black@0.5" else ""
        val borderFilter = if (style.outline) ":borderw=2:bordercolor=black@0.8" else ""
        val styleFlags = buildString {
            if (style.bold) append(":style=bold")
            if (style.italic) append(":style=italic")
        }
        val positionStr = when (style.alignment) {
            0 -> "x=(w-text_w)/2:y=10"
            1 -> "x=(w-text_w)/2:y=(h-text_h)/2"
            2 -> "x=(w-text_w)/2:y=h-text_h-10"
            3 -> "x=10:y=(h-text_h)/2"
            4 -> "x=w-text_w-10:y=(h-text_h)/2"
            else -> "x=(w-text_w)/2:y=(h-text_h)/2"
        }
        val drawtext = "drawtext=text='$escaped':fontsize=$fontSize:fontcolor=0x$colorHex" +
                ":$positionStr$shadowFilter$borderFilter$styleFlags"
        val animFilter = when (style.animationIn) {
            "fade_in" -> ",fade=t=in:st=0:d=0.5"
            "slide_up" -> ",drawtext=text='$escaped':fontsize=$fontSize:fontcolor=0x$colorHex:x=(w-text_w)/2:y=h:t=0.5"
            else -> ""
        }
        val filter = "$drawtext$animFilter"
        return execute(
            "-i", videoPath,
            "-vf", filter,
            "-c:a", "copy",
            "-y", outputPath
        ).map { outputPath }
    }

    suspend fun burnSubtitles(
        videoPath: String,
        subtitlePath: String,
        outputPath: String
    ): Result<String> {
        val ext = File(subtitlePath).extension.lowercase()
        val codec = when (ext) {
            "ass" -> "ass"
            "srt" -> "subtitles"
            else -> "subtitles"
        }
        val escaped = subtitlePath.replace("\\", "\\\\").replace(":", "\\:")
        return execute(
            "-i", videoPath,
            "-vf", "$codec='$escaped'",
            "-c:a", "copy",
            "-y", outputPath
        ).map { outputPath }
    }

    suspend fun renderTextWithSubtitles(
        videoPath: String,
        text: String,
        subtitlePath: String,
        outputPath: String,
        style: TextClipStyle
    ): Result<String> {
        val escaped = text
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace(":", "\\:")
        val fontSize = style.fontSize.toInt()
        val colorHex = String.format("%06X", style.color and 0xFFFFFF)
        val shadowFilter = if (style.shadow) ":shadowx=2:shadowy=2:shadowcolor=black@0.5" else ""
        val borderFilter = if (style.outline) ":borderw=2:bordercolor=black@0.8" else ""
        val styleFlags = buildString {
            if (style.bold) append(":style=bold")
            if (style.italic) append(":style=italic")
        }
        val drawtext = "drawtext=text='$escaped':fontsize=$fontSize:fontcolor=0x$colorHex" +
                ":x=(w-text_w)/2:y=h-text_h-60$shadowFilter$borderFilter$styleFlags"
        val subEscaped = subtitlePath.replace("\\", "\\\\").replace(":", "\\:")
        val filter = "$drawtext,subtitles='$subEscaped'"
        return execute(
            "-i", videoPath,
            "-vf", filter,
            "-c:a", "copy",
            "-y", outputPath
        ).map { outputPath }
    }

    private suspend fun execute(vararg args: String): Result<Unit> {
        val command = ffmpegExecutor.buildCommand(*args)
        return ffmpegExecutor.execute(command).map { }
    }
}
