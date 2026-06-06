package com.changecut.domain.usecase.project

import com.changecut.domain.model.Project
import com.changecut.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProjectsUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    operator fun invoke(): Flow<List<Project>> {
        return repository.getProjects()
    }
}
