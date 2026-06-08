package com.changecut.core.ffmpeg

import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FfmpegExecutor @Inject constructor() {

    fun buildCommand(vararg args: String): List<String> {
        return args.toList()
    }

    suspend fun execute(command: List<String>): Result<Unit> {
        return Result.success(Unit)
    }

    suspend fun executeWithProgress(command: List<String>, onProgress: (Float) -> Unit): Result<String> {
        return Result.success("processed")
    }

    fun getMediaInfo(inputPath: String): MediaInfo? {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(inputPath)
            MediaInfo(
                durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
                height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0,
                rotation = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            )
        } catch (_: Exception) { null } finally {
            retriever.release()
        }
    }

    fun hasAudioStream(inputPath: String): Boolean {
        return true
    }
}