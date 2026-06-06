package com.changecut.core.media

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class MediaFile(
        val uri: Uri,
        val name: String,
        val size: Long,
        val mimeType: String,
        val durationMs: Long = 0
    )

    fun queryMediaFromUri(uri: Uri): MediaFile? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIdx >= 0) c.getString(nameIdx) else "unknown"
                    val size = if (sizeIdx >= 0) c.getLong(sizeIdx) else 0L
                    val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
                    MediaFile(uri, name, size, mimeType)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun queryVideoFromUri(uri: Uri): MediaFile? = queryMediaFromUri(uri)

    fun copyToCache(uri: Uri, fileName: String): java.io.File? {
        return try {
            val cacheDir = java.io.File(context.cacheDir, "media")
            cacheDir.mkdirs()
            val outFile = java.io.File(cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            outFile
        } catch (e: Exception) {
            null
        }
    }

    fun buildImportedFileName(uri: Uri, prefix: String, mimeType: String, fallbackExtension: String): String {
        val sourceName = queryMediaFromUri(uri)?.name.orEmpty()
        val sourceExtension = sourceName.substringAfterLast('.', "").lowercase()
        val extension = when {
            sourceExtension.isNotBlank() -> sourceExtension
            mimeType.startsWith("video/") -> mimeType.removePrefix("video/").substringBefore('+').ifBlank { fallbackExtension }
            mimeType.startsWith("audio/") -> mimeType.removePrefix("audio/").substringBefore('+').ifBlank { fallbackExtension }
            mimeType.startsWith("image/") -> mimeType.removePrefix("image/").substringBefore('+').ifBlank { fallbackExtension }
            else -> fallbackExtension
        }.replace(Regex("[^a-z0-9]"), "").ifBlank { fallbackExtension }
        return "${prefix}_${java.util.UUID.randomUUID()}.$extension"
    }

    fun isVideoSupported(mimeType: String): Boolean {
        return mimeType.startsWith("video/") && SUPPORTED_VIDEO_TYPES.any { mimeType.contains(it) }
    }

    fun isAudioSupported(mimeType: String): Boolean {
        return mimeType.startsWith("audio/") && SUPPORTED_AUDIO_TYPES.any { mimeType.contains(it) }
    }

    fun isImageSupported(mimeType: String): Boolean {
        return mimeType.startsWith("image/") && SUPPORTED_IMAGE_TYPES.any { mimeType.contains(it) }
    }

    companion object {
        val SUPPORTED_VIDEO_TYPES = listOf("mp4", "webm", "mkv", "mov", "avi", "3gp")
        val SUPPORTED_AUDIO_TYPES = listOf("mp3", "wav", "aac", "ogg", "flac", "m4a")
        val SUPPORTED_IMAGE_TYPES = listOf("jpeg", "jpg", "png", "webp", "bmp")
    }
}
