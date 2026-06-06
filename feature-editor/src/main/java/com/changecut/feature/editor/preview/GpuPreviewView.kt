package com.changecut.feature.editor.preview

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.changecut.core.editor.Track
import com.changecut.core.gpu.FrameScheduler
import com.changecut.core.gpu.GlVideoRenderer

@Composable
fun GpuPreviewView(
    renderer: GlVideoRenderer,
    scheduler: FrameScheduler,
    tracks: List<Track>,
    currentTimeUs: Long,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val glSurfaceView = remember { mutableStateOf<GLSurfaceView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            renderer.releaseAll()
        }
    }

    AndroidView(
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(3)
                setRenderer(renderer)
                renderMode = if (isPlaying) GLSurfaceView.RENDERMODE_CONTINUOUSLY
                             else GLSurfaceView.RENDERMODE_WHEN_DIRTY
                glSurfaceView.value = this
            }
        },
        modifier = modifier
    )

    DisposableEffect(tracks, currentTimeUs, isPlaying) {
        renderer.updateTimeline(tracks, currentTimeUs, isPlaying)
        glSurfaceView.value?.let { view ->
            view.renderMode = if (isPlaying) GLSurfaceView.RENDERMODE_CONTINUOUSLY
                              else GLSurfaceView.RENDERMODE_WHEN_DIRTY
            view.requestRender()
        }
        onDispose { }
    }
}
