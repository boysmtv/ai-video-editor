package com.changecut.core.ai

import org.junit.Test
import org.junit.Assert.*

class AiBeatSyncTest {

    @Test
    fun `test beat info data class`() {
        val beat = AiBeatSync.BeatInfo(timeUs = 1000000L, intensity = 0.8f, frequency = 440f)
        assertEquals(1000000L, beat.timeUs)
        assertEquals(0.8f, beat.intensity)
    }

    @Test
    fun `test beat sync empty result on invalid path`() {
        val beatSync = AiBeatSync()
        val result = beatSync.detectBeats("/nonexistent/path.mp4")
        assertTrue(result.isEmpty())
    }
}

class AiSmartTrimTest {

    @Test
    fun `test trim segment data class`() {
        val segment = AiSmartTrim.TrimSegment(startUs = 1000L, endUs = 2000L)
        assertEquals(1000L, segment.startUs)
        assertEquals(2000L, segment.endUs)
    }
}

class AiVoiceIsolationTest {

    @Test
    fun `test voice isolation filter build`() {
        val isolator = AiVoiceIsolation()
        val filter = isolator.buildFullVoiceIsolation()
        assertTrue(filter.contains("highpass"))
        assertTrue(filter.contains("lowpass"))
    }
}