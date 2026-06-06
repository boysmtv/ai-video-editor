package com.changecut.core.export

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES31
import android.view.Surface
import com.changecut.core.editor.TrackManager
import com.changecut.core.gpu.GlVideoRenderer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HardwareExporter @Inject constructor(
    private val renderer: GlVideoRenderer
) {
    fun export(
        trackManager: TrackManager,
        outputPath: String,
        width: Int = 1080,
        height: Int = 1920,
        bitRate: Int = 15_000_000,
        frameRate: Int = 30,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        var muxer: MediaMuxer? = null
        var encoder: MediaCodec? = null
        var inputSurface: Surface? = null
        var eglDisplay: EGLDisplay? = null
        var eglContext: EGLContext? = null
        var eglSurface: EGLSurface? = null
        var muxerStarted = false
        return try {
            val durationUs = trackManager.totalDurationUs.value
            if (durationUs <= 0) return Result.failure(Exception("No timeline content"))

            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Configure video encoder
            val format = MediaFormat.createVideoFormat("video/avc", width, height).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }

            encoder = MediaCodec.createEncoderByType("video/avc")
            val codec = encoder ?: throw IllegalStateException("Failed to create H.264 encoder")
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = codec.createInputSurface()
            codec.start()

            var videoTrackIndex = -1
            var frameCount = 0L
            var totalFrames = (durationUs * frameRate / 1_000_000L).coerceAtLeast(1L)

            // Setup EGL for encoder input
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

            val configAttribs = intArrayOf(
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
            val config = configs[0] ?: throw RuntimeException("No EGL config")

            val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
            eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, inputSurface, intArrayOf(EGL14.EGL_NONE), 0)

            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
            GLES31.glViewport(0, 0, width, height)
            GLES31.glClearColor(0f, 0f, 0f, 1f)

            val bufferInfo = MediaCodec.BufferInfo()
            var frameUs = 0L
            val frameDurationUs = 1_000_000L / frameRate

            while (frameUs < durationUs) {
                GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)

                renderer.updateTimeline(trackManager.tracks.value, frameUs, false)
                renderer.onDrawFrame(null)

                EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, frameUs * 1000)
                EGL14.eglSwapBuffers(eglDisplay, eglSurface)

                // Drain encoder
                var outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                while (outputIndex >= 0 || outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            videoTrackIndex = muxer!!.addTrack(codec.outputFormat)
                            muxer!!.start()
                            muxerStarted = true
                        }
                    } else if (outputIndex >= 0) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size > 0) {
                            if (!muxerStarted) {
                                videoTrackIndex = muxer!!.addTrack(codec.outputFormat)
                                muxer!!.start()
                                muxerStarted = true
                            }
                            val outputBuffer = codec.getOutputBuffer(outputIndex)
                            if (outputBuffer != null) {
                                muxer!!.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                            }
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                    outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                }

                frameUs += frameDurationUs
                frameCount++
                if (frameCount % 30 == 0L) {
                    onProgress(frameCount.toFloat() / totalFrames.toFloat())
                }
            }

            // Signal end of stream
            codec.signalEndOfInputStream()
            var eos = false
            while (!eos) {
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                when {
                    outputIndex >= 0 -> {
                        if (bufferInfo.size > 0) {
                            if (!muxerStarted) {
                                videoTrackIndex = muxer!!.addTrack(codec.outputFormat)
                                muxer!!.start()
                                muxerStarted = true
                            }
                            val outputBuffer = codec.getOutputBuffer(outputIndex)
                            if (outputBuffer != null) {
                                muxer!!.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                            }
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) eos = true
                    }
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            videoTrackIndex = muxer!!.addTrack(codec.outputFormat)
                            muxer!!.start()
                            muxerStarted = true
                        }
                    }
                }
            }

            onProgress(1f)
            Result.success(outputPath)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { encoder?.stop() }
            runCatching { encoder?.release() }
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
            runCatching { inputSurface?.release() }
            if (eglDisplay != null && eglSurface != null) {
                runCatching { EGL14.eglDestroySurface(eglDisplay, eglSurface) }
            }
            if (eglDisplay != null && eglContext != null) {
                runCatching { EGL14.eglDestroyContext(eglDisplay, eglContext) }
            }
            if (eglDisplay != null) {
                runCatching { EGL14.eglTerminate(eglDisplay) }
            }
        }
    }
}
