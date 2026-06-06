package com.changecut.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String,
    val name: String,
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1920,
    val durationMs: Long = 0L,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
