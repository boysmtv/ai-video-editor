package com.changecut.core.ffmpeg

import android.content.Context
import com.changecut.core.editor.EditorClip
import com.changecut.core.editor.FilterDef
import com.changecut.core.editor.KeyframePoint
import com.changecut.core.editor.KeyframeProperty
import com.changecut.core.editor.KeyframeSystem
import com.changecut.core.editor.MaskDef
import com.changecut.core.editor.MaskType
import com.changecut.core.editor.TextClipStyle
import com.changecut.core.editor.Track
import com.changecut.core.editor.TrackManager
import com.changecut.core.editor.TrackType
import com.changecut.core.editor.TransitionDef
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ffmpegExecutor: FfmpegExecutor,
    private val colorGradingEngine: ColorGradingEngine,
    private val animationEngine: AnimationEngine,
    private val stickerEngine: StickerEngine,
    private val audioEQEngine: AudioEQEngine,
    private val maskEngine: MaskEngine
) {
    private val cacheDir: File get() = File(context.cacheDir, "changecut")

    suspend fun trimVideo(
        inputPath: String,
        outputPath: String,
        startMs: Long,
        endMs: Long
    ): Result<String> = execute(
        "-i", inputPath,
        "-ss", formatTime(startMs),
        "-to", formatTime(endMs),
        "-c", "copy",
        "-y", outputPath
    ).map { outputPath }

    suspend fun splitVideo(
        inputPath: String,
        outputDir: String,
        splitPointsMs: List<Long>
    ): Result<List<String>> {
        val segments = mutableListOf<String>()
        var prevPoint = 0L
        for ((i, point) in splitPointsMs.withIndex()) {
            val segPath = "$outputDir/segment_$i.mp4"
            trimVideo(inputPath, segPath, prevPoint, point).onSuccess { segments.add(it) }
            prevPoint = point
        }
        val lastPath = "$outputDir/segment_${splitPointsMs.size}.mp4"
        trimVideo(inputPath, lastPath, prevPoint, Long.MAX_VALUE)
        return Result.success(segments)
    }

    suspend fun concatVideos(
        inputPaths: List<String>,
        outputPath: String
    ): Result<String> {
        val listFile = File(cacheDir, "concat_list.txt")
        listFile.parentFile?.mkdirs()
        listFile.writeText(inputPaths.joinToString("\n") { "file '$it'" })

        return execute(
            "-f", "concat",
            "-safe", "0",
            "-i", listFile.absolutePath,
            "-c", "copy",
            "-y", outputPath
        ).map { outputPath }
    }

    suspend fun mergeVideos(
        inputPaths: List<String>,
        outputPath: String,
        resolution: String = "1080x1920"
    ): Result<String> {
        val filter = inputPaths.mapIndexed { i, _ -> "[${i}:v]" }.joinToString("") +
                inputPaths.mapIndexed { i, _ -> "[${i}:a]" }.joinToString("") +
                "concat=n=${inputPaths.size}:v=1:a=1[outv][outa]"

        val args = mutableListOf<String>()
        inputPaths.forEach { path ->
            args.addAll(listOf("-i", path))
        }
        args.addAll(listOf(
            "-filter_complex", filter,
            "-map", "[outv]",
            "-map", "[outa]",
            "-s", resolution,
            "-y", outputPath
        ))

        return execute(*args.toTypedArray()).map { outputPath }
    }

    suspend fun exportVideo(
        inputPath: String,
        outputPath: String,
        width: Int = 1080,
        height: Int = 1920,
        fps: Int = 30,
        bitRate: String = "10M"
    ): Result<String> = execute(
        "-i", inputPath,
        "-vf", "scale=$width:$height:force_original_aspect_ratio=decrease,pad=$width:$height:(ow-iw)/2:(oh-ih)/2",
        "-r", fps.toString(),
        "-b:v", bitRate,
        "-c:a", "copy",
        "-y", outputPath
    ).map { outputPath }

    suspend fun exportTimeline(
        trackManager: TrackManager,
        outputPath: String,
        width: Int = 1080,
        height: Int = 1920,
        fps: Int = 30,
        bitRate: String = "15M",
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        val visibleTracks = trackManager.tracks.value.filter { it.isVisible }
        val videoTrack = visibleTracks.firstOrNull { it.type == TrackType.VIDEO && it.clips.isNotEmpty() }
            ?: return Result.failure(VideoException("No visible video track to export"))
        val orderedClips = videoTrack.clips.sortedBy { it.startOffsetUs }
        if (orderedClips.isEmpty()) {
            return Result.failure(VideoException("No clips to export"))
        }

        val tempDir = File(cacheDir, "timeline_${System.currentTimeMillis()}").apply { mkdirs() }
        val totalUnits = orderedClips.size + countGaps(orderedClips)
        var completedUnits = 0

        return try {
            val segmentPaths = mutableListOf<String>()
            val timelineSegments = mutableListOf<TimelineSegment>()
            var previousEndUs = 0L

            for ((index, clip) in orderedClips.withIndex()) {
                val gapUs = (clip.startOffsetUs - previousEndUs).coerceAtLeast(0L)
                if (gapUs > 33_000L) {
                    val gapPath = File(tempDir, "gap_$index.mp4").absolutePath
                    val gapResult = createGapSegment(
                        outputPath = gapPath,
                        durationUs = gapUs,
                        width = width,
                        height = height,
                        fps = fps
                    )
                    if (gapResult.isFailure) {
                        return Result.failure(gapResult.exceptionOrNull() ?: VideoException("Failed to create gap segment"))
                    }
                    segmentPaths.add(gapPath)
                    timelineSegments.add(TimelineSegment(path = gapPath, durationUs = gapUs, transitionOut = null))
                    completedUnits++
                    onProgress(completedUnits.toFloat() / totalUnits.toFloat())
                }

                val segmentPath = File(tempDir, "segment_$index.mp4").absolutePath
                val segmentResult = renderClipSegment(
                    clip = clip,
                    outputPath = segmentPath,
                    width = width,
                    height = height,
                    fps = fps,
                    bitRate = bitRate,
                    onProgress = { progress ->
                        val base = completedUnits.toFloat() / totalUnits.toFloat()
                        onProgress((base + (progress / totalUnits.toFloat())).coerceIn(0f, 1f))
                    }
                )
                if (segmentResult.isFailure) {
                    return Result.failure(segmentResult.exceptionOrNull() ?: VideoException("Failed to render clip segment"))
                }
                segmentPaths.add(segmentPath)
                timelineSegments.add(
                    TimelineSegment(
                        path = segmentPath,
                        durationUs = clipDurationUsForTimeline(clip),
                        transitionOut = clip.transitionOut
                    )
                )
                previousEndUs = maxOf(previousEndUs, clip.endOffsetUs)
                completedUnits++
                onProgress(completedUnits.toFloat() / totalUnits.toFloat())
            }

            val concatenatedPath = File(tempDir, "timeline_concat.mp4").absolutePath
            val concatResult = if (timelineSegments.any { it.transitionOut != null }) {
                applyTimelineTransitions(
                    segments = timelineSegments,
                    outputPath = concatenatedPath,
                    tempDir = tempDir
                )
            } else {
                concatSegments(segmentPaths, concatenatedPath)
            }
            if (concatResult.isFailure) {
                return Result.failure(concatResult.exceptionOrNull() ?: VideoException("Failed to compose timeline"))
            }

            val overlayTracks = visibleTracks.filter { it.type == TrackType.TEXT || it.type == TrackType.OVERLAY || it.type == TrackType.STICKER }
            val overlayClips = overlayTracks
                .withIndex()
                .flatMap { (trackIndex, track) ->
                    track.clips.map { clip -> Triple(trackIndex, track, clip) }
                }
                .sortedWith(
                    compareBy<Triple<Int, Track, EditorClip>> { it.first }
                        .thenBy { it.third.startOffsetUs }
                        .thenBy { it.third.endOffsetUs }
                )
                .map { (_, track, clip) -> track to clip }
            val overlayOutputPath = File(tempDir, "timeline_with_overlays.mp4").absolutePath
            val overlayResult = if (overlayClips.isNotEmpty()) {
                applyTimelineOverlays(
                    inputPath = concatenatedPath,
                    overlays = overlayClips,
                    outputPath = overlayOutputPath,
                    width = width,
                    height = height
                )
            } else {
                File(concatenatedPath).copyTo(File(overlayOutputPath), overwrite = true)
                Result.success(overlayOutputPath)
            }
            if (overlayResult.isFailure) {
                return Result.failure(overlayResult.exceptionOrNull() ?: VideoException("Failed to apply overlays"))
            }

            val adjustmentClips = visibleTracks
                .filter { it.type == TrackType.ADJUSTMENT }
                .flatMap { it.clips }
                .sortedBy { it.startOffsetUs }
            val adjustedOutputPath = File(tempDir, "timeline_adjusted.mp4").absolutePath
            val adjustmentResult = if (adjustmentClips.isNotEmpty()) {
                applyAdjustmentTracks(
                    inputPath = overlayOutputPath,
                    adjustments = adjustmentClips,
                    outputPath = adjustedOutputPath,
                    width = width,
                    height = height
                )
            } else {
                File(overlayOutputPath).copyTo(File(adjustedOutputPath), overwrite = true)
                Result.success(adjustedOutputPath)
            }
            if (adjustmentResult.isFailure) {
                return Result.failure(adjustmentResult.exceptionOrNull() ?: VideoException("Failed to apply adjustments"))
            }

            val audioClips = visibleTracks
                .filter { it.type == TrackType.AUDIO && !it.isMuted }
                .flatMap { it.clips }
                .sortedBy { it.startOffsetUs }
            val finalResult = if (audioClips.isNotEmpty()) {
                applyAdditionalAudioTracks(
                    inputPath = adjustedOutputPath,
                    audioClips = audioClips,
                    outputPath = outputPath
                )
            } else {
                File(adjustedOutputPath).copyTo(File(outputPath), overwrite = true)
                Result.success(outputPath)
            }

            onProgress(1f)
            finalResult
        } finally {
            tempDir.deleteRecursively()
        }
    }

    fun getMediaInfo(inputPath: String): MediaInfo? {
        return ffmpegExecutor.getMediaInfo(inputPath)
    }

    private suspend fun execute(vararg args: String): Result<Unit> {
        val command = ffmpegExecutor.buildCommand(*args)
        return ffmpegExecutor.execute(command).map { }
    }

    private suspend fun renderClipSegment(
        clip: EditorClip,
        outputPath: String,
        width: Int,
        height: Int,
        fps: Int,
        bitRate: String,
        onProgress: (Float) -> Unit
    ): Result<String> {
        val trimStartUs = clip.trimStartUs.coerceAtLeast(0L)
        val clipDurationUs = resolveClipSourceDurationUs(clip)
        val resolvedTransform = resolveStaticTransform(clip, clipDurationUs / 2L)
        val videoFilters = mutableListOf<String>()
        val audioFilters = mutableListOf<String>()

        val targetScaleW = (width * resolvedTransform.scaleX.coerceIn(0.05f, 2f)).toInt().coerceAtLeast(2)
        val targetScaleH = (height * resolvedTransform.scaleY.coerceIn(0.05f, 2f)).toInt().coerceAtLeast(2)
        val normalizedX = resolvedTransform.positionX.coerceIn(0f, 1f)
        val normalizedY = resolvedTransform.positionY.coerceIn(0f, 1f)
        val padXExpr = "(($width-iw)*$normalizedX)"
        val padYExpr = "(($height-ih)*$normalizedY)"

        videoFilters.add("scale=$targetScaleW:$targetScaleH:force_original_aspect_ratio=decrease")
        videoFilters.add("pad=$width:$height:$padXExpr:$padYExpr:color=black")

        if (resolvedTransform.rotation != 0f) {
            val radians = resolvedTransform.rotation * Math.PI / 180.0
            videoFilters.add("rotate=$radians:ow=rotw($radians):oh=roth($radians):fillcolor=black")
        }
        buildMaskVideoFilter(clip.mask, width, height)
            ?.let(videoFilters::add)
        if (resolvedTransform.opacity < 1f) {
            videoFilters.add("format=rgba")
            videoFilters.add("colorchannelmixer=aa=${resolvedTransform.opacity.coerceIn(0f, 1f)}")
            videoFilters.add("format=yuv420p")
        }
        clip.colorFilter
            ?.takeIf { it.isNotBlank() }
            ?.let(::resolveColorFilter)
            ?.let(videoFilters::add)
        clip.effectId
            ?.takeIf { it.isNotBlank() }
            ?.let(::resolveEffectFilter)
            ?.let(videoFilters::add)
        if (clip.colorGrade != null) {
            colorGradingEngine.buildFullColorGrade(clip.colorGrade, width, height)
                .takeIf { it.isNotBlank() }
                ?.let(videoFilters::add)
        }
        clip.animationIn?.let {
            animationEngine.buildInAnimation(it, it.durationMs, width, height)
                .takeIf { filter -> filter.isNotBlank() }
                ?.let(videoFilters::add)
        }
        clip.animationOut?.let {
            animationEngine.buildOutAnimation(
                animation = it,
                durationMs = it.durationMs,
                totalDurationMs = (clipDurationUs / 1000L).toInt(),
                width = width,
                height = height
            ).takeIf { filter -> filter.isNotBlank() }
                ?.let(videoFilters::add)
        }
        clip.sticker?.takeIf { it.emoji.isNotBlank() }?.let { sticker ->
            videoFilters.add(
                stickerEngine.buildEmojiFilter(
                    emoji = sticker.emoji,
                    x = 0.78f,
                    y = 0.12f,
                    width = width,
                    height = height
                )
            )
        }
        if (clip.freezeDurationMs > 0) {
            videoFilters.add("tpad=stop_mode=clone:stop_duration=${clip.freezeDurationMs / 1000f}")
        }
        buildTransitionFilters(clip, clipDurationUs)
            .forEach(videoFilters::add)

        if (clip.volume != 1f) {
            audioFilters.add("volume=${clip.volume}")
        }
        if (clip.speed != 1f) {
            videoFilters.add("setpts=${(1f / clip.speed.coerceAtLeast(0.01f))}*PTS")
            audioFilters.add(buildAtempoFilter(clip.speed))
        }
        if (clip.audioEQ != null) {
            audioEQEngine.buildFullAudioProcess(clip.audioEQ)
                .takeIf { it.isNotBlank() }
                ?.let(audioFilters::add)
        }

        val args = mutableListOf(
            "-ss", formatTime(trimStartUs / 1000L),
            "-i", clip.sourceUri,
            "-t", formatTime(clipDurationUs / 1000L),
            "-map", "0:v:0",
            "-map", "0:a?"
        )
        if (videoFilters.isNotEmpty()) {
            args.addAll(listOf("-vf", videoFilters.joinToString(",")))
        }
        if (audioFilters.isNotEmpty()) {
            args.addAll(listOf("-af", audioFilters.joinToString(",")))
        }
        args.addAll(
            listOf(
                "-r", fps.toString(),
                "-b:v", bitRate,
                "-c:v", "libx264",
                "-preset", "medium",
                "-crf", "23",
                "-c:a", "aac",
                "-b:a", "192k",
                "-pix_fmt", "yuv420p",
                "-shortest",
                "-y", outputPath
            )
        )
        val command = ffmpegExecutor.buildCommand(*args.toTypedArray())
        return ffmpegExecutor.executeWithProgress(command, onProgress).map { outputPath }
    }

    private suspend fun createGapSegment(
        outputPath: String,
        durationUs: Long,
        width: Int,
        height: Int,
        fps: Int
    ): Result<String> {
        val command = ffmpegExecutor.buildCommand(
            "-f", "lavfi",
            "-i", "color=c=black:s=${width}x$height:r=$fps",
            "-f", "lavfi",
            "-i", "anullsrc=channel_layout=stereo:sample_rate=44100",
            "-t", formatTime(durationUs / 1000L),
            "-c:v", "libx264",
            "-c:a", "aac",
            "-pix_fmt", "yuv420p",
            "-shortest",
            "-y", outputPath
        )
        return ffmpegExecutor.execute(command).map { outputPath }
    }

    private suspend fun concatSegments(
        segmentPaths: List<String>,
        outputPath: String
    ): Result<String> {
        val listFile = File(cacheDir, "timeline_concat_${System.currentTimeMillis()}.txt")
        listFile.parentFile?.mkdirs()
        listFile.writeText(segmentPaths.joinToString("\n") { "file '${it.replace("'", "'\\''")}'" })
        return try {
            execute(
                "-f", "concat",
                "-safe", "0",
                "-i", listFile.absolutePath,
                "-c", "copy",
                "-y", outputPath
            ).map { outputPath }
        } finally {
            listFile.delete()
        }
    }

    private suspend fun applyTimelineTransitions(
        segments: List<TimelineSegment>,
        outputPath: String,
        tempDir: File
    ): Result<String> {
        if (segments.isEmpty()) return Result.failure(VideoException("No timeline segments"))
        if (segments.size == 1) {
            File(segments.first().path).copyTo(File(outputPath), overwrite = true)
            return Result.success(outputPath)
        }

        var currentPath = segments.first().path
        var currentDurationUs = segments.first().durationUs
        var currentHasTransition = false

        for (index in 1 until segments.size) {
            val previous = segments[index - 1]
            val next = segments[index]
            val transition = previous.transitionOut
            if (transition == null) {
                val joinedPath = File(tempDir, "join_$index.mp4").absolutePath
                val concatResult = concatSegments(listOf(currentPath, next.path), joinedPath)
                if (concatResult.isFailure) return concatResult
                currentPath = joinedPath
                currentDurationUs += next.durationUs
                continue
            }

            val durationSec = (transition.durationMs.coerceAtLeast(100) / 1000f)
            val currentDurationSec = (currentDurationUs / 1_000_000f).coerceAtLeast(durationSec)
            val offsetSec = (currentDurationSec - durationSec).coerceAtLeast(0f)
            val transitionName = resolveTransitionName(transition.type)
            val transitionedPath = if (index == segments.lastIndex) outputPath else File(tempDir, "transition_$index.mp4").absolutePath

            val command = ffmpegExecutor.buildCommand(
                "-i", currentPath,
                "-i", next.path,
                "-filter_complex",
                "[0:v][1:v]xfade=transition=$transitionName:duration=$durationSec:offset=$offsetSec[v];" +
                        "[0:a][1:a]acrossfade=d=$durationSec[a]",
                "-map", "[v]",
                "-map", "[a]",
                "-c:v", "libx264",
                "-c:a", "aac",
                "-pix_fmt", "yuv420p",
                "-y", transitionedPath
            )
            val transitionResult = ffmpegExecutor.execute(command).map { transitionedPath }
            if (transitionResult.isFailure) return transitionResult
            currentPath = transitionedPath
            currentDurationUs = (currentDurationUs + next.durationUs - (transition.durationMs * 1000L)).coerceAtLeast(33_000L)
            currentHasTransition = true
        }

        if (!currentHasTransition && currentPath != outputPath) {
            File(currentPath).copyTo(File(outputPath), overwrite = true)
        }
        return Result.success(outputPath)
    }

    private suspend fun applyTimelineOverlays(
        inputPath: String,
        overlays: List<Pair<Track, EditorClip>>,
        outputPath: String,
        width: Int,
        height: Int
    ): Result<String> {
        val drawFilters = overlays.mapNotNull { (track, clip) ->
            buildDrawOverlayFilter(track, clip, width, height)
        }
        val mediaOverlays = overlays.filter { (track, clip) ->
            (track.type == TrackType.OVERLAY || track.type == TrackType.STICKER) && clip.sourceUri.isNotBlank()
        }
        if (drawFilters.isEmpty() && mediaOverlays.isEmpty()) {
            File(inputPath).copyTo(File(outputPath), overwrite = true)
            return Result.success(outputPath)
        }

        if (mediaOverlays.isEmpty()) {
            return execute(
                "-i", inputPath,
                "-vf", drawFilters.joinToString(","),
                "-c:a", "copy",
                "-y", outputPath
            ).map { outputPath }
        }

        val args = mutableListOf("-i", inputPath)
        mediaOverlays.forEach { (_, clip) ->
            args.addAll(listOf("-stream_loop", "-1", "-i", clip.sourceUri))
        }

        val filterParts = mutableListOf<String>()
        var currentLabel = "[base0]"
        filterParts.add("[0:v]setpts=PTS-STARTPTS${
            if (drawFilters.isNotEmpty()) "," + drawFilters.joinToString(",") else ""
        }$currentLabel")

        mediaOverlays.forEachIndexed { index, (track, clip) ->
            val inputIndex = index + 1
            val overlayLabel = "[ovr${index}]"
            val canvasLabel = "[ovc${index}]"
            val nextBaseLabel = "[base${index + 1}]"
            val posX = ((clip.positionX.coerceIn(0f, 1f)) * width).toInt()
            val posY = ((clip.positionY.coerceIn(0f, 1f)) * height).toInt()
            val overlayWidth = (width * clip.scaleX.coerceIn(0.05f, 2f)).toInt().coerceAtLeast(2)
            val overlayHeight = (height * clip.scaleY.coerceIn(0.05f, 2f)).toInt().coerceAtLeast(2)
            val startSec = clip.startOffsetUs / 1_000_000f
            val endSec = clip.endOffsetUs / 1_000_000f
            val enable = "between(t,$startSec,$endSec)"

            val overlayFilters = mutableListOf<String>()
            val trimStartSec = (clip.trimStartUs / 1_000_000f).coerceAtLeast(0f)
            val trimEndSec = if (clip.trimEndUs > clip.trimStartUs) clip.trimEndUs / 1_000_000f else null
            overlayFilters.add(
                if (trimEndSec != null) "trim=start=$trimStartSec:end=$trimEndSec"
                else "trim=start=$trimStartSec"
            )
            overlayFilters.add("setpts=PTS-STARTPTS")
            if (clip.speed != 1f) {
                overlayFilters.add("setpts=${(1f / clip.speed.coerceAtLeast(0.01f))}*PTS")
            }
            overlayFilters.add("scale=$overlayWidth:$overlayHeight:force_original_aspect_ratio=decrease")
            if (clip.rotation != 0f) {
                val radians = clip.rotation * Math.PI / 180.0
                overlayFilters.add("rotate=$radians:ow=rotw($radians):oh=roth($radians):fillcolor=black@0")
            }
            buildMaskVideoFilter(clip.mask, overlayWidth, overlayHeight)
                ?.let(overlayFilters::add)
            clip.colorFilter
                ?.takeIf { it.isNotBlank() }
                ?.let(::resolveColorFilter)
                ?.let(overlayFilters::add)
            clip.effectId
                ?.takeIf { it.isNotBlank() }
                ?.let(::resolveEffectFilter)
                ?.let(overlayFilters::add)
            clip.colorGrade?.let { grade ->
                colorGradingEngine.buildFullColorGrade(grade, overlayWidth, overlayHeight)
                    .takeIf { it.isNotBlank() }
                    ?.let(overlayFilters::add)
            }
            clip.animationIn?.let { animation ->
                animationEngine.buildInAnimation(animation, animation.durationMs, overlayWidth, overlayHeight)
                    .takeIf { it.isNotBlank() }
                    ?.let(overlayFilters::add)
            }
            clip.animationOut?.let { animation ->
                animationEngine.buildOutAnimation(
                    animation = animation,
                    durationMs = animation.durationMs,
                    totalDurationMs = ((clipDurationUsForTimeline(clip) / 1000L).coerceAtLeast(1L)).toInt(),
                    width = overlayWidth,
                    height = overlayHeight
                ).takeIf { it.isNotBlank() }
                    ?.let(overlayFilters::add)
            }
            overlayFilters.add("format=rgba")
            if (clip.opacity < 1f) {
                overlayFilters.add("colorchannelmixer=aa=${clip.opacity.coerceIn(0f, 1f)}")
            }

            filterParts.add("[${inputIndex}:v]${overlayFilters.joinToString(",")}$overlayLabel")
            filterParts.add("$overlayLabel pad=$width:$height:$posX:$posY:color=black@0$canvasLabel")

            if (clip.blendMode.ffmpegName != "normal") {
                filterParts.add("$currentLabel$canvasLabel blend=all_mode=${clip.blendMode.ffmpegName}:all_opacity=${clip.opacity.coerceIn(0f, 1f)}:enable='$enable'$nextBaseLabel")
            } else {
                filterParts.add("$currentLabel$canvasLabel overlay=0:0:enable='$enable'$nextBaseLabel")
            }
            currentLabel = nextBaseLabel
        }

        args.addAll(
            listOf(
                "-filter_complex", filterParts.joinToString(";"),
                "-map", currentLabel,
                "-map", "0:a?",
                "-c:v", "libx264",
                "-c:a", "aac",
                "-pix_fmt", "yuv420p",
                "-shortest",
                "-y", outputPath
            )
        )
        val command = ffmpegExecutor.buildCommand(*args.toTypedArray())
        return ffmpegExecutor.execute(command).map { outputPath }
    }

    private suspend fun applyAdjustmentTracks(
        inputPath: String,
        adjustments: List<EditorClip>,
        outputPath: String,
        width: Int,
        height: Int
    ): Result<String> {
        val filters = adjustments.mapNotNull { clip ->
            buildAdjustmentFilter(clip, width, height)
        }
        if (filters.isEmpty()) {
            File(inputPath).copyTo(File(outputPath), overwrite = true)
            return Result.success(outputPath)
        }
        return execute(
            "-i", inputPath,
            "-vf", filters.joinToString(","),
            "-c:a", "copy",
            "-y", outputPath
        ).map { outputPath }
    }

    private suspend fun applyAdditionalAudioTracks(
        inputPath: String,
        audioClips: List<EditorClip>,
        outputPath: String
    ): Result<String> {
        if (audioClips.isEmpty()) {
            File(inputPath).copyTo(File(outputPath), overwrite = true)
            return Result.success(outputPath)
        }

        val args = mutableListOf("-i", inputPath)
        audioClips.forEach { clip ->
            args.addAll(listOf("-i", clip.sourceUri))
        }

        val filterParts = mutableListOf<String>()
        val hasBaseAudio = ffmpegExecutor.hasAudioStream(inputPath)
        val mixInputs = mutableListOf<String>()
        if (hasBaseAudio) {
            filterParts.add("[0:a]anull[basea]")
            mixInputs.add("[basea]")
        }
        audioClips.forEachIndexed { index, clip ->
            val inputIndex = index + 1
            val label = "[aud$index]"
            val startMs = (clip.startOffsetUs / 1000L).coerceAtLeast(0L)
            val clipDurationMs = (clipDurationUsForTimeline(clip) / 1000L).coerceAtLeast(1L)
            val trimStartSec = (clip.trimStartUs / 1_000_000f).coerceAtLeast(0f)
            val trimEndSec = if (clip.trimEndUs > clip.trimStartUs) clip.trimEndUs / 1_000_000f else null
            val fadeInSec = (clip.audioFadeInMs / 1000f).coerceAtLeast(0f)
            val fadeOutDurationSec = (clip.audioFadeOutMs / 1000f).coerceAtLeast(0f)
            val chain = mutableListOf<String>()
            chain.add(
                if (trimEndSec != null) "atrim=$trimStartSec:$trimEndSec"
                else "atrim=start=$trimStartSec"
            )
            if (clip.speed != 1f) {
                chain.add(buildAtempoFilter(clip.speed))
            }
            chain.add("adelay=${startMs}|${startMs}")
            chain.add("volume=${clip.volume}")
            if (fadeInSec > 0f) {
                chain.add("afade=t=in:st=0:d=$fadeInSec")
            }
            if (fadeOutDurationSec > 0f) {
                val fadeStartSec = ((clipDurationMs / 1000f) - fadeOutDurationSec).coerceAtLeast(0f)
                chain.add("afade=t=out:st=$fadeStartSec:d=$fadeOutDurationSec")
            }
            if (clip.audioEQ != null) {
                audioEQEngine.buildFullAudioProcess(clip.audioEQ)
                    .takeIf { it.isNotBlank() }
                    ?.let(chain::add)
            }
            filterParts.add("[${inputIndex}:a]${chain.joinToString(",")}$label")
            mixInputs.add(label)
        }
        filterParts.add("${mixInputs.joinToString("")}amix=inputs=${mixInputs.size}:duration=longest:dropout_transition=0[aout]")

        args.addAll(
            listOf(
                "-filter_complex", filterParts.joinToString(";"),
                "-map", "0:v",
                "-map", "[aout]",
                "-c:v", "copy",
                "-c:a", "aac",
                "-shortest",
                "-y", outputPath
            )
        )
        val command = ffmpegExecutor.buildCommand(*args.toTypedArray())
        return ffmpegExecutor.execute(command).map { outputPath }
    }

    private fun buildDrawOverlayFilter(
        track: Track,
        clip: EditorClip,
        width: Int,
        height: Int
    ): String? {
        val startSec = clip.startOffsetUs / 1_000_000f
        val endSec = clip.endOffsetUs / 1_000_000f
        val enable = "enable='between(t,$startSec,$endSec)'"
        val escapedText = clip.textContent
            ?.replace("\\", "\\\\")
            ?.replace("'", "\\'")
            ?.replace(":", "\\:")

        return when {
            !escapedText.isNullOrBlank() -> buildTextOverlayFilter(
                text = escapedText,
                clip = clip,
                style = clip.textStyle,
                contentRole = clip.contentRole,
                enable = enable,
                width = width,
                height = height,
                positionX = clip.positionX,
                positionY = clip.positionY
            )
            clip.sticker?.emoji?.isNotBlank() == true -> {
                "${stickerEngine.buildEmojiFilter(clip.sticker.emoji, x = clip.positionX.coerceIn(0f, 1f), y = clip.positionY.coerceIn(0f, 1f), width = width, height = height)}:$enable"
            }
            (track.type == TrackType.OVERLAY || track.type == TrackType.STICKER) && clip.sourceUri.isNotBlank() -> null
            else -> null
        }
    }

    private fun buildAdjustmentFilter(
        clip: EditorClip,
        width: Int,
        height: Int
    ): String? {
        val enabledFilters = mutableListOf<String>()
        val enable = "between(t,${clip.startOffsetUs / 1_000_000f},${clip.endOffsetUs / 1_000_000f})"

        clip.colorGrade?.let { grade ->
            buildTimedFilter(
                colorGradingEngine.buildFullColorGrade(grade, width, height),
                enable
            )?.let(enabledFilters::add)
        }
        if (clip.opacity < 1f) {
            enabledFilters.add("colorchannelmixer=aa=${clip.opacity.coerceIn(0f, 1f)}:enable='$enable'")
        }
        return enabledFilters.takeIf { it.isNotEmpty() }?.joinToString(",")
    }

    private fun buildTextOverlayFilter(
        text: String,
        clip: EditorClip,
        style: TextClipStyle?,
        contentRole: String?,
        enable: String,
        width: Int,
        height: Int,
        positionX: Float = 0.5f,
        positionY: Float = 0.78f
    ): String {
        val role = contentRole?.lowercase()
        val defaultFontSize = when (role) {
            "title" -> 54
            "subtitle" -> 34
            "caption" -> 36
            else -> 42
        }
        val fontSize = style?.fontSize?.toInt() ?: defaultFontSize
        val colorHex = String.format("%06X", (style?.color ?: 0xFFFFFFFF) and 0xFFFFFF)
        val resolvedAlignment = style?.alignment ?: when (role) {
            "subtitle", "caption" -> 2
            "title" -> 1
            else -> 1
        }
        val defaultX = resolvedAlignment.let { alignment ->
            when (alignment) {
                3 -> 0.08f
                4 -> 0.72f
                else -> 0.5f
            }
        }
        val defaultY = when (role) {
            "title" -> 0.16f
            "subtitle" -> 0.86f
            "caption" -> 0.78f
            else -> when (resolvedAlignment) {
                0 -> 0.12f
                2 -> 0.88f
                else -> 0.78f
            }
        }
        val useLegacyDefaultPosition = positionX == 0f && positionY == 0f && !clip.textContent.isNullOrBlank()
        val resolvedX = if (useLegacyDefaultPosition) defaultX else positionX.coerceIn(0f, 1f)
        val resolvedY = if (useLegacyDefaultPosition) defaultY else positionY.coerceIn(0f, 1f)
        val posX = (resolvedX * width).toInt()
        val posY = (resolvedY * height).toInt()
        val shadow = if (style?.shadow == true) ":shadowx=2:shadowy=2:shadowcolor=black@0.5" else ""
        val outline = if (style?.outline == true) ":borderw=2:bordercolor=black@0.8" else ""
        val alphaExpr = buildOverlayAlphaExpression(clip)
        val alpha = ":alpha='$alphaExpr'"
        val background = style?.backgroundColor?.let { bg ->
            val bgHex = String.format("%06X", bg and 0xFFFFFF)
            ":box=1:boxcolor=0x${bgHex}@0.7:boxborderw=12"
        } ?: ""
        return "drawtext=text='$text':fontsize=$fontSize:fontcolor=0x$colorHex:x=$posX:y=$posY:$enable$alpha$shadow$outline$background"
    }

    private fun buildOverlayAlphaExpression(clip: EditorClip): String {
        val baseOpacity = clip.opacity.coerceIn(0f, 1f)
        val startSec = clip.startOffsetUs / 1_000_000f
        val endSec = clip.endOffsetUs / 1_000_000f
        val inDurationSec = (clip.transitionIn?.durationMs ?: 0).coerceAtLeast(0) / 1000f
        val outDurationSec = (clip.transitionOut?.durationMs ?: 0).coerceAtLeast(0) / 1000f

        var expr = baseOpacity.toString()
        if (clip.transitionIn?.type?.contains("fade", ignoreCase = true) == true && inDurationSec > 0f) {
            expr = "($expr)*if(lt(t,$startSec),0,if(lt(t,${startSec + inDurationSec}),(t-$startSec)/$inDurationSec,1))"
        }
        if (clip.transitionOut?.type?.contains("fade", ignoreCase = true) == true && outDurationSec > 0f) {
            val fadeStart = (endSec - outDurationSec).coerceAtLeast(startSec)
            expr = "($expr)*if(lt(t,$fadeStart),1,if(lt(t,$endSec),($endSec-t)/$outDurationSec,0))"
        }
        return expr
    }

    private fun buildMaskVideoFilter(
        mask: MaskDef?,
        width: Int,
        height: Int
    ): String? {
        mask ?: return null
        return when (mask.type) {
            MaskType.RECTANGLE -> {
                val cropW = (width * mask.width.coerceIn(0.05f, 1f)).toInt().coerceAtLeast(2)
                val cropH = (height * mask.height.coerceIn(0.05f, 1f)).toInt().coerceAtLeast(2)
                val cropX = ((width * mask.centerX) - cropW / 2).toInt().coerceIn(0, (width - cropW).coerceAtLeast(0))
                val cropY = ((height * mask.centerY) - cropH / 2).toInt().coerceIn(0, (height - cropH).coerceAtLeast(0))
                "crop=$cropW:$cropH:$cropX:$cropY,pad=$width:$height:(ow-iw)/2:(oh-ih)/2:color=black"
            }
            MaskType.RADIAL -> {
                val cx = (mask.centerX * width).toInt()
                val cy = (mask.centerY * height).toInt()
                val radius = ((minOf(width, height) * mask.width.coerceIn(0.05f, 1f)) / 2f).toInt().coerceAtLeast(1)
                val feather = mask.featherPx.coerceAtLeast(1f)
                "format=rgba,geq=r='r(X,Y)':g='g(X,Y)':b='b(X,Y)':a='255*(1-smoothstep($radius,${
                    radius + feather
                },hypot(X-$cx,Y-$cy)))',format=yuv420p"
            }
            else -> null
        }
    }

    private fun buildTimedFilter(
        filterChain: String,
        enableExpr: String
    ): String? {
        if (filterChain.isBlank()) return null
        return filterChain.split(",")
            .map { segment ->
                val trimmed = segment.trim()
                if (trimmed.isBlank()) null
                else if (trimmed.contains("enable=")) trimmed
                else "$trimmed:enable='$enableExpr'"
            }
            .filterNotNull()
            .joinToString(",")
    }

    private fun buildTransitionFilters(
        clip: EditorClip,
        clipDurationUs: Long
    ): List<String> {
        val filters = mutableListOf<String>()
        clip.transitionIn?.let { transition ->
            buildSingleTransitionFilter(
                transition = transition,
                clipDurationUs = clipDurationUs,
                isIn = true
            )?.let(filters::add)
        }
        clip.transitionOut?.let { transition ->
            buildSingleTransitionFilter(
                transition = transition,
                clipDurationUs = clipDurationUs,
                isIn = false
            )?.let(filters::add)
        }
        return filters
    }

    private fun buildSingleTransitionFilter(
        transition: TransitionDef,
        clipDurationUs: Long,
        isIn: Boolean
    ): String? {
        val durationSec = (transition.durationMs.coerceAtLeast(50) / 1000f)
        val clipDurationSec = (clipDurationUs / 1_000_000f).coerceAtLeast(durationSec)
        val normalizedType = transition.type.lowercase()
        return when {
            normalizedType.contains("fade") || normalizedType.contains("dissolve") -> {
                if (isIn) {
                    "fade=t=in:st=0:d=$durationSec"
                } else {
                    val startSec = (clipDurationSec - durationSec).coerceAtLeast(0f)
                    "fade=t=out:st=$startSec:d=$durationSec"
                }
            }
            normalizedType.contains("blur") -> {
                if (isIn) {
                    "boxblur=enable='lt(t,$durationSec)':luma_radius='if(lt(t,$durationSec),20*(1-t/$durationSec),0)'"
                } else {
                    val startSec = (clipDurationSec - durationSec).coerceAtLeast(0f)
                    "boxblur=enable='gte(t,$startSec)':luma_radius='if(gte(t,$startSec),20*(t-$startSec)/$durationSec,0)'"
                }
            }
            normalizedType.contains("zoom") -> {
                if (isIn) {
                    "scale=iw*(1+0.08*(1-min(t/$durationSec,1))):ih*(1+0.08*(1-min(t/$durationSec,1)))"
                } else {
                    "scale=iw*(1+0.08*max((t-${(clipDurationSec - durationSec).coerceAtLeast(0f)})/$durationSec,0)):ih*(1+0.08*max((t-${(clipDurationSec - durationSec).coerceAtLeast(0f)})/$durationSec,0))"
                }
            }
            normalizedType.contains("slide") || normalizedType.contains("wipe") -> {
                if (isIn) {
                    "crop=iw:ih:x='if(lt(t,$durationSec),(1-t/$durationSec)*iw*0.08,0)':y=0"
                } else {
                    val startSec = (clipDurationSec - durationSec).coerceAtLeast(0f)
                    "crop=iw:ih:x='if(gte(t,$startSec),((t-$startSec)/$durationSec)*iw*0.08,0)':y=0"
                }
            }
            else -> null
        }
    }

    private fun resolveStaticTransform(
        clip: EditorClip,
        sampleTimeUs: Long
    ): ResolvedTransform {
        val sample = sampleTimeUs.coerceAtLeast(0L)
        return ResolvedTransform(
            positionX = resolveKeyframedValue(
                clip = clip,
                property = KeyframeProperty.POSITION_X,
                defaultValue = clip.positionX,
                sampleTimeUs = sample
            ),
            positionY = resolveKeyframedValue(
                clip = clip,
                property = KeyframeProperty.POSITION_Y,
                defaultValue = clip.positionY,
                sampleTimeUs = sample
            ),
            scaleX = resolveKeyframedValue(
                clip = clip,
                property = KeyframeProperty.SCALE_X,
                defaultValue = clip.scaleX,
                sampleTimeUs = sample
            ),
            scaleY = resolveKeyframedValue(
                clip = clip,
                property = KeyframeProperty.SCALE_Y,
                defaultValue = clip.scaleY,
                sampleTimeUs = sample
            ),
            rotation = resolveKeyframedValue(
                clip = clip,
                property = KeyframeProperty.ROTATION,
                defaultValue = clip.rotation,
                sampleTimeUs = sample
            ),
            opacity = resolveKeyframedValue(
                clip = clip,
                property = KeyframeProperty.OPACITY,
                defaultValue = clip.opacity,
                sampleTimeUs = sample
            )
        )
    }

    private fun resolveKeyframedValue(
        clip: EditorClip,
        property: KeyframeProperty,
        defaultValue: Float,
        sampleTimeUs: Long
    ): Float {
        val points = clip.keyframes
            .filter { runCatching { KeyframeProperty.valueOf(it.property) }.getOrNull() == property }
            .map { KeyframePoint(timeUs = it.timeUs, value = it.value, easing = it.easing) }
        return if (points.isEmpty()) defaultValue else KeyframeSystem.interpolate(points, sampleTimeUs)
    }

    private fun resolveColorFilter(filterId: String): String? {
        if (filterId.startsWith("chromakey:", ignoreCase = true)) {
            val payload = filterId.substringAfter("chromakey:")
            val parts = payload.split(":")
            val color = parts.firstOrNull()
                ?.removePrefix("0x")
                ?.removePrefix("0X")
                ?.removePrefix("#")
                ?.padStart(6, '0')
                ?: return null
            val similarity = parts.getOrNull(1)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.3f
            val blend = parts.getOrNull(2)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.15f
            return "colorkey=0x$color:$similarity:$blend"
        }
        return FilterEngine.filters
            .firstOrNull { it.id == filterId || it.name.equals(filterId, ignoreCase = true) }
            ?.ffmpegFilter
            ?.takeIf { it.isNotBlank() }
    }

    private fun resolveEffectFilter(effectId: String): String? {
        return effectFilterMap[effectId]
    }

    private fun resolveTransitionName(type: String): String {
        return when (type.lowercase()) {
            "fade", "dissolve" -> "fade"
            "slide", "slide_left" -> "slideleft"
            "slide_right" -> "slideright"
            "slide_up" -> "slideup"
            "slide_down" -> "slidedown"
            "wipe", "wipe_left" -> "wipeleft"
            "wipe_right" -> "wiperight"
            "wipe_up" -> "wipeup"
            "wipe_down" -> "wipedown"
            "zoom", "zoom_in" -> "zoomin"
            "zoom_out" -> "zoomout"
            "radial" -> "radial"
            "blur" -> "blur"
            "glitch" -> "glitch"
            else -> "fade"
        }
    }

    private fun clipDurationUsForTimeline(clip: EditorClip): Long {
        return resolveClipSourceDurationUs(clip) + (clip.freezeDurationMs * 1000L)
    }

    private fun resolveClipSourceDurationUs(clip: EditorClip): Long {
        if (clip.trimEndUs > clip.trimStartUs) {
            return clip.trimEndUs - clip.trimStartUs
        }
        if (clip.durationUs > 0) {
            return clip.durationUs
        }
        val mediaInfo = getMediaInfo(clip.sourceUri)
        return ((mediaInfo?.durationMs ?: 0L) * 1000L).coerceAtLeast(33_000L)
    }

    private fun buildAtempoFilter(speed: Float): String {
        val chain = mutableListOf<String>()
        var remaining = speed.coerceAtLeast(0.01f)
        while (remaining > 2f) {
            chain.add("atempo=2.0")
            remaining /= 2f
        }
        while (remaining < 0.5f) {
            chain.add("atempo=0.5")
            remaining /= 0.5f
        }
        chain.add("atempo=$remaining")
        return chain.joinToString(",")
    }

    private fun countGaps(clips: List<EditorClip>): Int {
        var count = 0
        var previousEndUs = 0L
        for (clip in clips) {
            if ((clip.startOffsetUs - previousEndUs) > 33_000L) {
                count++
            }
            previousEndUs = maxOf(previousEndUs, clip.endOffsetUs)
        }
        return count
    }

    private fun formatTime(ms: Long): String {
        if (ms == Long.MAX_VALUE) return "9999:59:59.999"
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        val millis = ms % 1000
        return String.format("%02d:%02d:%02d.%03d", h, m, s, millis)
    }
}

data class MediaInfo(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotation: Int
)

class VideoException(message: String) : Exception(message)

private data class ResolvedTransform(
    val positionX: Float,
    val positionY: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotation: Float,
    val opacity: Float
)

private data class TimelineSegment(
    val path: String,
    val durationUs: Long,
    val transitionOut: TransitionDef?
)

private val effectFilterMap: Map<String, String> = mapOf(
    "glitch_1" to "noise=alls=20:allf=t+u",
    "glitch_2" to "noise=alls=40:allf=t+u",
    "shake_1" to "crop=iw-10:ih-10:x='5*sin(t*12)':y='5*cos(t*10)'",
    "shake_2" to "crop=iw-20:ih-20:x='10*sin(t*18)':y='10*cos(t*14)'",
    "zoom_1" to "scale=iw*1.08:ih*1.08",
    "zoom_2" to "scale=iw*0.92:ih*0.92",
    "pulse_1" to "scale=iw*(1+0.04*sin(2*PI*t)):ih*(1+0.04*sin(2*PI*t))",
    "blur_1" to "boxblur=2:1",
    "blur_2" to "boxblur=5:3",
    "blur_3" to "boxblur=10:5",
    "blur_4" to "gblur=sigma=3",
    "shatter_1" to "tblend=all_mode=difference,edgedetect=low=0.1:high=0.4",
    "blend_overlay" to "curves=master='0/0 0.5/0.58 1/1'",
    "blend_screen" to "eq=brightness=0.04:saturation=1.08",
    "blend_multiply" to "eq=brightness=-0.04:contrast=1.08",
    "filter_vintage" to "colorchannelmixer=.393:.769:.189:.349:.686:.168:.272:.534:.131",
    "filter_grayscale" to "colorchannelmixer=.3:.4:.3:0:.3:.4:.3:0:.3:.4:.3",
    "filter_sepia" to "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131",
    "filter_vivid" to "eq=saturation=2.0:contrast=1.2",
    "filter_cool" to "colorbalance=rs=-0.2:gs=-0.1:bs=0.3",
    "filter_warm" to "colorbalance=rs=0.3:gs=0.1:bs=-0.2",
    "filter_dramatic" to "eq=contrast=1.5:brightness=-0.1",
    "filter_noir" to "hue=s=0,eq=contrast=1.3:brightness=-0.05",
    "filter_fade" to "curves=master='0/0 0.5/0.4 1/1'"
)
