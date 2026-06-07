package com.changecut.feature.home.newproject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.changecut.domain.usecase.project.CreateProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewProjectUiState(
    val isCreating: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class NewProjectViewModel @Inject constructor(
    private val createProject: CreateProjectUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(NewProjectUiState())
    val state: StateFlow<NewProjectUiState> = _state.asStateFlow()

    fun create(option: CanvasOption, onCreated: (String) -> Unit) {
        if (_state.value.isCreating) return

        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, errorMessage = null) }
            runCatching {
                createProject(
                    name = "Untitled Project",
                    canvasWidth = option.width.takeIf { it > 0 } ?: 1080,
                    canvasHeight = option.height.takeIf { it > 0 } ?: 1920
                )
            }.onSuccess { project ->
                _state.update { it.copy(isCreating = false) }
                onCreated(project.id)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isCreating = false,
                        errorMessage = error.message ?: "Failed to create project"
                    )
                }
            }
        }
    }
}
