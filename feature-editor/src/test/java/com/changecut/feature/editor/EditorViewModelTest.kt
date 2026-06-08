package com.changecut.feature.editor

import org.junit.Test
import org.junit.Assert.*

class EditorViewModelLogicTest {

    @Test
    fun `test track types are valid`() {
        assertTrue(com.changecut.core.editor.TrackType.VIDEO.name.isNotEmpty())
        assertTrue(com.changecut.core.editor.TrackType.AUDIO.name.isNotEmpty())
    }

    @Test
    fun `test keyframe property enum values`() {
        val properties = com.changecut.core.editor.KeyframeProperty.values()
        assertTrue(properties.size >= 7) // Position, Scale, Rotation, Opacity, Volume + mask props
    }

    @Test
    fun `test blend mode enum values`() {
        val modes = com.changecut.core.editor.BlendMode.values()
        assertTrue(modes.contains(com.changecut.core.editor.BlendMode.NORMAL))
        assertTrue(modes.contains(com.changecut.core.editor.BlendMode.SCREEN))
    }

    @Test
    fun `test animation types exist`() {
        val types = com.changecut.core.editor.AnimationType.values()
        assertTrue(types.contains(com.changecut.core.editor.AnimationType.FADE))
        assertTrue(types.contains(com.changecut.core.editor.AnimationType.SLIDE))
        assertTrue(types.contains(com.changecut.core.editor.AnimationType.ZOOM))
    }

    @Test
    fun `test video effect types exist`() {
        val types = com.changecut.core.editor.VideoEffectType.values()
        assertTrue(types.contains(com.changecut.core.editor.VideoEffectType.VHS))
        assertTrue(types.contains(com.changecut.core.editor.VideoEffectType.GLITCH))
        assertTrue(types.contains(com.changecut.core.editor.VideoEffectType.NEON))
    }
}