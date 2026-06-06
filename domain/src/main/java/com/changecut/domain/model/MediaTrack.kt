package com.changecut.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaTrack(
    val id: String,
    val projectId: String,
    val name: String,
    val type: TrackType,
    val clips: List<Clip> = emptyList(),
    val order: Int = 0,
    val isVisible: Boolean = true,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false
)

@Serializable
enum class TrackType {
    VIDEO,
    AUDIO,
    TEXT,
    SUBTITLE,
    STICKER,
    EFFECT,
    OVERLAY
}

@Serializable
data class Clip(
    val id: String,
    val sourcePath: String,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val isReversed: Boolean = false,
    val isFrozen: Boolean = false
)
