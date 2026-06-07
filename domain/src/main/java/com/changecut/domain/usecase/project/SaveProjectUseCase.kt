package com.changecut.domain.usecase.project

import com.changecut.domain.model.Project
import com.changecut.domain.repository.ProjectRepository
import javax.inject.Inject

class SaveProjectUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(project: Project) {
        repository.saveProject(project)
    }
}
