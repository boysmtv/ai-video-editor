package com.changecut.core.cache

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheCleaner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L
        private const val PROJECT_CACHE_PREFIX = "project_"
    }

    fun getCacheSize(): Long {
        return calculateDirSize(context.cacheDir)
    }

    fun getProjectCacheSize(projectId: String): Long {
        val projectDir = getProjectCacheDir(projectId)
        return if (projectDir.exists()) calculateDirSize(projectDir) else 0L
    }

    fun cleanCache(): Boolean {
        return try {
            val now = System.currentTimeMillis()
            cleanDirectory(context.cacheDir, now - CACHE_EXPIRY_MS)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun cleanProjectCache(projectId: String): Boolean {
        return try {
            val projectDir = getProjectCacheDir(projectId)
            if (projectDir.exists()) {
                projectDir.deleteRecursively()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun cleanAllCache(): Boolean {
        return try {
            cleanDirectory(context.cacheDir, Long.MAX_VALUE)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun cleanMediaCache(): Boolean {
        return try {
            val mediaDir = File(context.cacheDir, "media")
            if (mediaDir.exists()) {
                mediaDir.deleteRecursively()
                mediaDir.mkdirs()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun cleanVoiceOverCache(): Boolean {
        return try {
            val voiceDir = File(context.cacheDir, "voiceover")
            if (voiceDir.exists()) {
                voiceDir.deleteRecursively()
                voiceDir.mkdirs()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun cleanThumbnailCache(): Boolean {
        return try {
            val thumbDir = File(context.cacheDir, "thumbnails")
            if (thumbDir.exists()) {
                thumbDir.deleteRecursively()
                thumbDir.mkdirs()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes < 0L -> "0 B"
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024L * 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun getCacheBreakdown(): Map<String, String> {
        val breakdown = mutableMapOf<String, String>()
        val subDirs = context.cacheDir.listFiles { f -> f.isDirectory } ?: emptyArray()

        for (dir in subDirs) {
            val size = calculateDirSize(dir)
            if (size > 0) {
                breakdown[dir.name] = formatSize(size)
            }
        }

        val totalSize = calculateDirSize(context.cacheDir)
        breakdown["total"] = formatSize(totalSize)

        return breakdown
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L

        var size = 0L
        val files = dir.listFiles() ?: return size

        for (file in files) {
            size += if (file.isDirectory) {
                calculateDirSize(file)
            } else {
                file.length()
            }
        }

        return size
    }

    private fun cleanDirectory(dir: File, expiryTime: Long) {
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                cleanDirectory(file, expiryTime)
                if (file.listFiles()?.isEmpty() == true) {
                    file.delete()
                }
            } else {
                if (file.lastModified() < expiryTime || expiryTime == Long.MAX_VALUE) {
                    file.delete()
                }
            }
        }
    }

    private fun getProjectCacheDir(projectId: String): File {
        return File(context.cacheDir, "$PROJECT_CACHE_PREFIX$projectId")
    }
}
