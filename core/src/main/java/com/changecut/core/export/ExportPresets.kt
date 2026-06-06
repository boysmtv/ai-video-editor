package com.changecut.core.export

import kotlinx.serialization.Serializable

@Serializable
data class ExportPreset(
    val name: String,
    val width: Int,
    val height: Int,
    val bitrate: String,
    val fps: Int,
    val format: String = "mp4"
) {
    val resolution: String get() = "${width}x${height}"

    fun isPortrait(): Boolean = height > width

    fun isLandscape(): Boolean = width > height

    fun isSquare(): Boolean = width == height

    val aspectRatio: String get() {
        val gcd = gcd(width, height)
        return "${width / gcd}:${height / gcd}"
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    companion object {
        val UHD_4K = ExportPreset(
            name = "UHD 4K",
            width = 3840,
            height = 2160,
            bitrate = "50M",
            fps = 60,
            format = "mp4"
        )

        val FULL_HD = ExportPreset(
            name = "Full HD",
            width = 1920,
            height = 1080,
            bitrate = "20M",
            fps = 30,
            format = "mp4"
        )

        val HD = ExportPreset(
            name = "HD",
            width = 1280,
            height = 720,
            bitrate = "10M",
            fps = 30,
            format = "mp4"
        )

        val SD = ExportPreset(
            name = "SD",
            width = 854,
            height = 480,
            bitrate = "5M",
            fps = 24,
            format = "mp4"
        )

        val SOCIAL = ExportPreset(
            name = "Social Media",
            width = 1080,
            height = 1920,
            bitrate = "15M",
            fps = 30,
            format = "mp4"
        )

        val DEFAULT: ExportPreset get() = FULL_HD

        val allPresets: List<ExportPreset> = listOf(UHD_4K, FULL_HD, HD, SD, SOCIAL)

        fun fromName(name: String): ExportPreset? = allPresets.find { it.name == name }
    }
}

object ExportCommandBuilder {

    fun buildExportCommand(
        inputs: List<String>,
        preset: ExportPreset,
        outputPath: String,
        additionalFilters: String = ""
    ): String {
        val args = mutableListOf<String>()

        for (input in inputs) {
            args.addAll(listOf("-i", "\"$input\""))
        }

        val scaleFilter = buildScaleFilter(preset, additionalFilters)

        if (inputs.size == 1) {
            args.addAll(
                listOf(
                    "-vf", "\"$scaleFilter\"",
                    "-r", preset.fps.toString(),
                    "-b:v", preset.bitrate,
                    "-c:v", "libx264",
                    "-preset", "medium",
                    "-crf", "23",
                    "-c:a", "aac",
                    "-b:a", "192k",
                    "-pix_fmt", "yuv420p",
                    "-y",
                    "\"$outputPath\""
                )
            )
        } else {
            val concatFilter = buildConcatFilter(inputs.size, scaleFilter)
            args.addAll(
                listOf(
                    "-filter_complex", "\"$concatFilter\"",
                    "-map", "[outv]",
                    "-map", "[outa]",
                    "-r", preset.fps.toString(),
                    "-b:v", preset.bitrate,
                    "-c:v", "libx264",
                    "-preset", "medium",
                    "-crf", "23",
                    "-c:a", "aac",
                    "-b:a", "192k",
                    "-pix_fmt", "yuv420p",
                    "-y",
                    "\"$outputPath\""
                )
            )
        }

        return "ffmpeg ${args.joinToString(" ")}"
    }

    private fun buildScaleFilter(preset: ExportPreset, additional: String): String {
        val base = "scale=${preset.width}:${preset.height}:" +
                "force_original_aspect_ratio=decrease," +
                "pad=${preset.width}:${preset.height}:(ow-iw)/2:(oh-ih)/2:color=black"
        return if (additional.isNotBlank()) {
            "$base,$additional"
        } else {
            base
        }
    }

    private fun buildConcatFilter(inputCount: Int, scaleFilter: String): String {
        val videoInputs = (0 until inputCount).joinToString("") { "[${it}:v]" }
        val audioInputs = (0 until inputCount).joinToString("") { "[${it}:a]" }
        val scaled = (0 until inputCount).joinToString(";") { i ->
            "[${i}:v]${scaleFilter}[v${i}]"
        }
        val scaledInputs = (0 until inputCount).joinToString("") { "[v${it}]" }
        return "${scaled};${scaledInputs}${audioInputs}concat=n=${inputCount}:v=1:a=1[outv][outa]"
    }

    fun buildTrimCommand(
        inputPath: String,
        outputPath: String,
        startMs: Long,
        endMs: Long,
        preset: ExportPreset
    ): String {
        val start = formatTime(startMs)
        val duration = formatTime(endMs - startMs)
        val scale = buildScaleFilter(preset, "")

        return "ffmpeg -i \"$inputPath\" " +
                "-ss $start -t $duration " +
                "-vf \"$scale\" " +
                "-r ${preset.fps} " +
                "-b:v ${preset.bitrate} " +
                "-c:v libx264 -preset medium -crf 23 " +
                "-c:a aac -b:a 192k " +
                "-pix_fmt yuv420p " +
                "-y \"$outputPath\""
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        val millis = ms % 1000
        return String.format("%02d:%02d:%02d.%03d", h, m, s, millis)
    }
}
