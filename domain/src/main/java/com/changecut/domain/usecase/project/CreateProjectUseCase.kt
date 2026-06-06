package com.changecut.domain.usecase.project

import com.changecut.domain.model.Project
import com.changecut.domain.repository.ProjectRepository
import javax.inject.Inject

class CreateProjectUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(name: String, canvasWidth: Int = 1080, canvasHeight: Int = 1920): Project {
        val project = Project(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight
        )
        repository.saveProject(project)
        return project
    }
}
