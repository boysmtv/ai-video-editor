package com.changecut.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.changecut.core.media.MediaProjectManager
import com.changecut.domain.model.Project
import com.changecut.domain.usecase.project.DeleteProjectUseCase
import com.changecut.domain.usecase.project.GetProjectByIdUseCase
import com.changecut.domain.usecase.project.GetProjectsUseCase
import com.changecut.domain.usecase.project.SaveProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val projects: List<ProjectItem> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    getProjects: GetProjectsUseCase,
    private val getProjectById: GetProjectByIdUseCase,
    private val saveProject: SaveProjectUseCase,
    private val deleteProject: DeleteProjectUseCase,
    private val mediaProjectManager: MediaProjectManager
) : ViewModel() {

    val state: StateFlow<HomeUiState> = getProjects()
        .map { projects -> HomeUiState(projects = projects.map { it.toProjectItem() }) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    fun renameProject(projectId: String, newName: String) {
        val cleanedName = newName.trim()
        if (cleanedName.isBlank()) return

        viewModelScope.launch {
            runCatching {
                val project = getProjectById(projectId) ?: return@runCatching
                saveProject(project.copy(name = cleanedName, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun duplicateProject(projectId: String) {
        viewModelScope.launch {
            runCatching {
                val source = getProjectById(projectId) ?: return@runCatching
                val now = System.currentTimeMillis()
                val duplicate = source.copy(
                    id = UUID.randomUUID().toString(),
                    name = "${source.name} Copy",
                    createdAt = now,
                    updatedAt = now
                )
                saveProject(duplicate)

                val sourceDir = mediaProjectManager.getProjectDir(source.id)
                val targetDir = mediaProjectManager.getProjectDir(duplicate.id)
                targetDir.deleteRecursively()
                if (sourceDir.exists()) {
                    sourceDir.copyRecursively(targetDir, overwrite = true)
                } else {
                    targetDir.mkdirs()
                }
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            runCatching {
                deleteProject.invoke(projectId)
                mediaProjectManager.clearProject(projectId)
            }
        }
    }

    private fun Project.toProjectItem(): ProjectItem = ProjectItem(
        id = id,
        name = name,
        duration = formatDuration(durationMs),
        lastEdited = formatDate(updatedAt),
        thumbnailPath = thumbnailPath
    )

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
}
