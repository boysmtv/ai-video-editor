package com.changecut.data.repository

import com.changecut.core.database.dao.ProjectDao
import com.changecut.core.database.entity.ProjectEntity
import com.changecut.domain.model.Project
import com.changecut.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao
) : ProjectRepository {

    override fun getProjects(): Flow<List<Project>> {
        return projectDao.getAllProjects().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getProjectById(id: String): Project? {
        return projectDao.getProjectById(id)?.toDomain()
    }

    override suspend fun saveProject(project: Project) {
        projectDao.insertProject(project.toEntity())
    }

    override suspend fun deleteProject(id: String) {
        projectDao.deleteProjectById(id)
    }

    private fun ProjectEntity.toDomain() = Project(
        id = id,
        name = name,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        durationMs = durationMs,
        thumbnailPath = thumbnailPath,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Project.toEntity() = ProjectEntity(
        id = id,
        name = name,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        durationMs = durationMs,
        thumbnailPath = thumbnailPath,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
