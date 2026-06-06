package com.changecut.feature.editor.effects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.changecut.core.editor.EffectCategory
import com.changecut.core.editor.EffectDef
import com.changecut.core.editor.SnapshotCommand
import com.changecut.core.editor.TrackManager
import com.changecut.core.editor.UndoRedoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EffectUiState(
    val currentClipId: String? = null,
    val appliedEffectId: String? = null,
    val appliedFilterId: String? = null,
    val selectedCategory: EffectCategory = EffectCategory.COLOR,
    val allEffects: List<EffectDef> = EffectCatalog.getAllEffects(),
    val categoryEffects: List<EffectDef> = EffectCatalog.getEffectsByCategory(EffectCategory.COLOR)
)

@HiltViewModel
class EffectViewModel @Inject constructor(
    private val trackManager: TrackManager,
    private val undoRedoManager: UndoRedoManager
) : ViewModel() {

    private val _state = MutableStateFlow(EffectUiState())
    val state: StateFlow<EffectUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            trackManager.tracks.collect {
                refreshAppliedState()
            }
        }
    }

    fun setCurrentClip(clipId: String?) {
        _state.update { state ->
            val clip = clipId?.let { trackManager.getClip(it) }
            state.copy(
                currentClipId = clip?.id,
                appliedEffectId = clip?.effectId,
                appliedFilterId = clip?.colorFilter
            )
        }
    }

    fun selectCategory(category: EffectCategory) {
        _state.update {
            it.copy(
                selectedCategory = category,
                categoryEffects = EffectCatalog.getEffectsByCategory(category)
            )
        }
    }

    fun applyEffect(clipId: String, effectId: String) {
        viewModelScope.launch {
            undoRedoManager.execute(
                SnapshotCommand(trackManager, "Apply effect") {
                    trackManager.applyEffect(clipId, effectId)
                }
            )
            refreshAppliedState()
        }
    }

    fun removeEffect(clipId: String) {
        viewModelScope.launch {
            undoRedoManager.execute(
                SnapshotCommand(trackManager, "Remove effect") {
                    trackManager.removeEffect(clipId)
                }
            )
            refreshAppliedState()
        }
    }

    fun applyFilter(clipId: String, filterId: String) {
        viewModelScope.launch {
            undoRedoManager.execute(
                SnapshotCommand(trackManager, "Apply filter") {
                    trackManager.applyColorFilter(clipId, filterId)
                }
            )
            refreshAppliedState()
        }
    }

    fun removeFilter(clipId: String) {
        viewModelScope.launch {
            undoRedoManager.execute(
                SnapshotCommand(trackManager, "Remove filter") {
                    trackManager.applyColorFilter(clipId, "")
                }
            )
            refreshAppliedState()
        }
    }

    private fun refreshAppliedState() {
        val clipId = _state.value.currentClipId ?: run {
            _state.update {
                it.copy(
                    appliedEffectId = null,
                    appliedFilterId = null
                )
            }
            return
        }
        val clip = trackManager.getClip(clipId)
        _state.update {
            it.copy(
                currentClipId = clip?.id,
                appliedEffectId = clip?.effectId,
                appliedFilterId = clip?.colorFilter
            )
        }
    }
}
