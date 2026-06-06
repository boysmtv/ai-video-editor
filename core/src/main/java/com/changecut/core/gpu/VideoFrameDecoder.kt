package com.changecut.core.gpu

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES31
import android.os.Build
import android.view.Surface
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class VideoFrameDecoder(
    private val videoPath: String
) {
    private var extractor: MediaExtractor? = null
    private var codec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var eglDisplay: EGLDisplay? = null
    private var eglContext: EGLContext? = null
    private var eglSurface: EGLSurface? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var frameAvailableLatch: CountDownLatch? = null

    var textureId: Int = -1
        private set
    var width: Int = 0
        private set
    var height: Int = 0
        private set
    var durationUs: Long = 0L
        private set
    var isEos: Boolean = false
        private set

    private val shouldContinue = AtomicBoolean(true)

    fun prepare() {
        extractor = MediaExtractor().also { ext ->
            ext.setDataSource(videoPath)
            val trackIndex = selectVideoTrack(ext)
            if (trackIndex < 0) throw RuntimeException("No video track found")
            ext.selectTrack(trackIndex)
            val format = ext.getTrackFormat(trackIndex)
            width = format.getInteger(MediaFormat.KEY_WIDTH)
            height = format.getInteger(MediaFormat.KEY_HEIGHT)
            durationUs = format.getLong(MediaFormat.KEY_DURATION)

            initEGL()
            val textures = IntArray(1)
            GLES31.glGenTextures(1, textures, 0)
            textureId = textures[0]
            GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

            surfaceTexture = SurfaceTexture(textureId)
            surfaceTexture?.setOnFrameAvailableListener {
                frameAvailableLatch?.countDown()
            }
            inputSurface = Surface(surfaceTexture)

            val mime = format.getString(MediaFormat.KEY_MIME) ?: "video/avc"
            codec = MediaCodec.createDecoderByType(mime).also { dec ->
                dec.configure(format, inputSurface, null, 0)
                dec.start()
            }
        }
    }

    fun release() {
        shouldContinue.set(false)
        codec?.stop()
        codec?.release()
        codec = null
        extractor?.release()
        extractor = null
        inputSurface?.release()
        inputSurface = null
        surfaceTexture?.release()
        surfaceTexture = null
        destroyEGL()
    }

    fun seekTo(timeUs: Long) {
        isEos = false
        codec?.flush()
        extractor?.seekTo(timeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
    }

    fun renderFrame(timeUs: Long) {
        if (isEos) return
        val ext = extractor ?: return
        val dec = codec ?: return
        val timeoutUs: Long = 10000

        var inputBufferIndex = dec.dequeueInputBuffer(timeoutUs)
        while (inputBufferIndex >= 0 && shouldContinue.get()) {
            val inputBuffer = dec.getInputBuffer(inputBufferIndex) ?: break
            val sampleSize = ext.readSampleData(inputBuffer, 0)
            if (sampleSize < 0) {
                dec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                isEos = true
                break
            }
            val sampleTime = ext.sampleTime
            dec.queueInputBuffer(inputBufferIndex, 0, sampleSize, sampleTime, 0)
            ext.advance()
            if (sampleTime >= timeUs) break
            inputBufferIndex = dec.dequeueInputBuffer(timeoutUs)
        }

        val bufferInfo = MediaCodec.BufferInfo()
        val outputIndex = dec.dequeueOutputBuffer(bufferInfo, timeoutUs)
        if (outputIndex >= 0) {
            frameAvailableLatch = CountDownLatch(1)
            dec.releaseOutputBuffer(outputIndex, true)
            frameAvailableLatch?.await(15, TimeUnit.MILLISECONDS)
            surfaceTexture?.updateTexImage()
            frameAvailableLatch = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, bufferInfo.presentationTimeUs * 1000)
            }
            eglSwap()
        }
    }

    private fun selectVideoTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/")) return i
        }
        return -1
    }

    private fun initEGL() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
        val configAttribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
        val config = configs[0] ?: throw RuntimeException("No EGL config")

        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
            ?: throw RuntimeException("EGL context failed")

        val surfAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, config, surfAttribs, 0)
            ?: throw RuntimeException("EGL surface failed")

        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
    }

    private fun destroyEGL() {
        eglSurface?.let { EGL14.eglDestroySurface(eglDisplay, it) }
        eglContext?.let { EGL14.eglDestroyContext(eglDisplay, it) }
        eglDisplay?.let { EGL14.eglTerminate(it) }
        eglSurface = null; eglContext = null; eglDisplay = null
    }

    private fun eglSwap() {
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }
}
