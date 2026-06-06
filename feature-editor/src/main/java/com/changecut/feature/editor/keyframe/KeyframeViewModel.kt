package com.changecut.feature.editor.keyframe

import androidx.lifecycle.ViewModel
import com.changecut.core.editor.EasingType
import com.changecut.core.editor.KeyframePoint
import com.changecut.core.editor.KeyframeProperty
import com.changecut.core.editor.KeyframeSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

data class KeyframeItem(
    val id: String,
    val property: String,
    val timeUs: Long,
    val value: Float,
    val easing: EasingType
)

data class KeyframeUiState(
    val clipId: String = "",
    val allKeyframes: List<KeyframeItem> = emptyList(),
    val keyframes: List<KeyframeItem> = emptyList(),
    val selectedProperty: KeyframeProperty = KeyframeProperty.POSITION_X,
    val selectedKeyframeId: String? = null,
    val currentTimeUs: Long = 0L,
    val interpolatedValue: Float = 0f
)

@HiltViewModel
class KeyframeViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(KeyframeUiState())
    val state: StateFlow<KeyframeUiState> = _state.asStateFlow()

    fun initialize(clipId: String, existingKeyframes: List<KeyframeItem>) {
        _state.update {
            it.copy(
                clipId = clipId,
                allKeyframes = existingKeyframes,
                keyframes = existingKeyframes.filter { kf ->
                    kf.property == it.selectedProperty.name
                }
            )
        }
        recalculateInterpolation()
    }

    fun selectProperty(property: KeyframeProperty) {
        _state.update {
            it.copy(
                selectedProperty = property,
                selectedKeyframeId = null
            )
        }
        refreshKeyframesForProperty()
        recalculateInterpolation()
    }

    fun setCurrentTime(timeUs: Long) {
        _state.update { it.copy(currentTimeUs = timeUs) }
        recalculateInterpolation()
    }

    fun addKeyframe(timeUs: Long, value: Float) {
        val property = _state.value.selectedProperty
        val id = "${property.name}_${UUID.randomUUID()}"
        val keyframe = KeyframeItem(
            id = id,
            property = property.name,
            timeUs = timeUs,
            value = value,
            easing = EasingType.LINEAR
        )
        _state.update { current ->
            val updatedAll = (current.allKeyframes + keyframe).sortedBy { kf -> kf.timeUs }
            current.copy(
                allKeyframes = updatedAll,
                keyframes = updatedAll.filter { it.property == current.selectedProperty.name },
                selectedKeyframeId = keyframe.id
            )
        }
        recalculateInterpolation()
    }

    fun removeKeyframe(id: String) {
        _state.update { current ->
            val updatedAll = current.allKeyframes.filter { kf -> kf.id != id }
            current.copy(
                allKeyframes = updatedAll,
                keyframes = updatedAll.filter { kf -> kf.property == current.selectedProperty.name },
                selectedKeyframeId = if (current.selectedKeyframeId == id) null else current.selectedKeyframeId
            )
        }
        recalculateInterpolation()
    }

    fun updateKeyframeValue(id: String, value: Float) {
        _state.update { state ->
            val updatedAll = state.allKeyframes.map { kf ->
                if (kf.id == id) kf.copy(value = value) else kf
            }
            state.copy(
                allKeyframes = updatedAll,
                keyframes = updatedAll.filter { it.property == state.selectedProperty.name }
            )
        }
        recalculateInterpolation()
    }

    fun updateKeyframeEasing(id: String, easing: EasingType) {
        _state.update { state ->
            val updatedAll = state.allKeyframes.map { kf ->
                if (kf.id == id) kf.copy(easing = easing) else kf
            }
            state.copy(
                allKeyframes = updatedAll,
                keyframes = updatedAll.filter { it.property == state.selectedProperty.name }
            )
        }
        recalculateInterpolation()
    }

    fun selectKeyframe(id: String?) {
        _state.update { it.copy(selectedKeyframeId = id) }
    }

    private fun recalculateInterpolation() {
        val state = _state.value
        val points = state.keyframes.map { kf ->
            KeyframePoint(timeUs = kf.timeUs, value = kf.value, easing = kf.easing)
        }
        val interpolated = KeyframeSystem.interpolate(points, state.currentTimeUs)
        _state.update { it.copy(interpolatedValue = interpolated) }
    }

    private fun refreshKeyframesForProperty() {
        val selectedPropertyName = _state.value.selectedProperty.name
        val allKeyframes = _state.value.allKeyframes
        _state.update { current ->
            current.copy(
                keyframes = allKeyframes.filter { kf -> kf.property == selectedPropertyName }
            )
        }
    }
}
