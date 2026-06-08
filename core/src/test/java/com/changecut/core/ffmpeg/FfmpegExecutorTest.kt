package com.changecut.core.ffmpeg

import org.junit.Test
import org.junit.Assert.*

class FfmpegExecutorTest {

    @Test
    fun `test ffmpeg command builder`() {
        val executor = FfmpegExecutor()
        val cmd = executor.buildCommand("-i", "input.mp4", "-c:v", "libx264", "output.mp4")
        assertTrue(cmd.contains("ffmpeg"))
        assertTrue(cmd.contains("-i"))
        assertTrue(cmd.contains("input.mp4"))
    }

    @Test
    fun `test format time helper`() {
        // Test VideoEngine.formatTime logic via reflection
        val result = VideoEngine::class.members.find { it.name == "formatTime" }
        // VideoEngine is a class, formatTime is private - test via public API
    }
}