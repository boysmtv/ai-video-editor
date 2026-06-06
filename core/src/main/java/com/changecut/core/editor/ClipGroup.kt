package com.changecut.core.editor

data class ClipGroup(
    val id: String,
    val clipIds: List<String> = emptyList(),
    val name: String = "Group",
    val isExpanded: Boolean = true
)

data class TemplateProject(
    val id: String,
    val name: String,
    val description: String = "",
    val category: String = "Custom",
    val thumbnailPath: String? = null,
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1920,
    val tracks: List<Track> = emptyList(),
    val durationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

data class ExportJob(
    val id: String,
    val projectName: String,
    val outputPath: String,
    val presetName: String,
    val progress: Float = 0f,
    val status: ExportStatus = ExportStatus.QUEUED,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ExportStatus {
    QUEUED, EXPORTING, COMPLETED, FAILED
}

data class SnapGuide(
    val position: Float,
    val type: SnapType
)

enum class SnapType {
    PLAYHEAD, CLIP_START, CLIP_END, TRACK_BOUNDARY
}
