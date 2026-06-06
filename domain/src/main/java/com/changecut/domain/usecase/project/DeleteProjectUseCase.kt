package com.changecut.domain.usecase.project

import com.changecut.domain.repository.ProjectRepository
import javax.inject.Inject

class DeleteProjectUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(projectId: String) {
        repository.deleteProject(projectId)
    }
}
