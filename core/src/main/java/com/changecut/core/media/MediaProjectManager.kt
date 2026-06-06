package com.changecut.core.media

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaProjectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getProjectDir(projectId: String): File {
        val dir = File(context.filesDir, "projects/$projectId")
        dir.mkdirs()
        return dir
    }

    fun getMediaDir(projectId: String): File {
        val dir = File(getProjectDir(projectId), "media")
        dir.mkdirs()
        return dir
    }

    fun getExportDir(projectId: String): File {
        val dir = File(getProjectDir(projectId), "export")
        dir.mkdirs()
        return dir
    }

    fun getTimelineStateFile(projectId: String): File {
        return File(getProjectDir(projectId), "timeline_state.json")
    }

    fun getThumbnailDir(projectId: String): File {
        val dir = File(getProjectDir(projectId), "thumbnails")
        dir.mkdirs()
        return dir
    }

    fun clearProject(projectId: String) {
        getProjectDir(projectId).deleteRecursively()
    }
}
