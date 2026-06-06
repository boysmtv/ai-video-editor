package com.changecut.core.export

import com.changecut.core.editor.ExportJob
import com.changecut.core.editor.ExportStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportQueueManager @Inject constructor() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _jobs = MutableStateFlow<List<ExportJob>>(emptyList())
    val jobs: StateFlow<List<ExportJob>> = _jobs.asStateFlow()
    private var currentJob: Job? = null

    fun enqueueExport(
        projectName: String,
        outputPath: String,
        presetName: String,
        exportBlock: suspend (onProgress: (Float) -> Unit) -> Unit
    ): String {
        val id = UUID.randomUUID().toString()
        val job = ExportJob(id, projectName, outputPath, presetName)
        _jobs.value = _jobs.value + job
        processNext(exportBlock)
        return id
    }

    private fun processNext(exportBlock: suspend (onProgress: (Float) -> Unit) -> Unit) {
        if (currentJob?.isActive == true) return
        val pending = _jobs.value.find { it.status == ExportStatus.QUEUED } ?: return
        currentJob = scope.launch {
            _jobs.value = _jobs.value.map { if (it.id == pending.id) it.copy(status = ExportStatus.EXPORTING) else it }
            try {
                exportBlock { progress ->
                    _jobs.value = _jobs.value.map { if (it.id == pending.id) it.copy(progress = progress) else it }
                }
                _jobs.value = _jobs.value.map { if (it.id == pending.id) it.copy(status = ExportStatus.COMPLETED, progress = 1f) else it }
            } catch (e: Exception) {
                _jobs.value = _jobs.value.map { if (it.id == pending.id) it.copy(status = ExportStatus.FAILED) else it }
            }
            currentJob = null
            processNext(exportBlock)
        }
    }

    fun cancelAll() {
        currentJob?.cancel()
        _jobs.value = emptyList()
    }
}
