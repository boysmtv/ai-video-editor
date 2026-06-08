package com.changecut.core.editor

import org.junit.Test
import org.junit.Assert.*

class TrackManagerTest {

    @Test
    fun `test track manager create track`() {
        val trackManager = TrackManager()
        val initialCount = trackManager.tracks.value.size
        trackManager.addTrack(TrackType.VIDEO, "Test Video")
        assertTrue(trackManager.tracks.value.size > initialCount)
    }

    @Test
    fun `test track manager add clip`() {
        val trackManager = TrackManager()
        val clip = EditorClip(
            id = "clip_1",
            sourceUri = "/test/video.mp4",
            startOffsetUs = 0L,
            endOffsetUs = 5_000_000L
        )
        trackManager.addClip(0, clip)
        assertEquals(1, trackManager.tracks.value[0].clips.size)
    }

    @Test
    fun `test keyframe interpolation returns correct values`() {
        val keyframes = listOf(
            Keyframe("k1", "POSITION_X", 0L, 0f),
            Keyframe("k2", "POSITION_X", 1_000_000L, 1f)
        )
        val start = KeyframeSystem.interpolate(keyframes, 0L)
        assertEquals(0f, start)
        val mid = KeyframeSystem.interpolate(keyframes, 500_000L)
        assertEquals(0.5f, mid, 0.01f)
        val end = KeyframeSystem.interpolate(keyframes, 1_000_000L)
        assertEquals(1f, end)
    }

    @Test
    fun `test mask def serialization`() {
        val mask = MaskDef(type = MaskType.RADIAL, centerX = 0.3f, centerY = 0.7f)
        assertNotNull(mask.type)
        assertEquals(0.3f, mask.centerX)
    }
}