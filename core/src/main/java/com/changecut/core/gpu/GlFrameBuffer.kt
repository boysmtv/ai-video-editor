package com.changecut.core.gpu

import android.opengl.GLES31

class GlFrameBuffer(val width: Int, val height: Int) {
    val fboId: Int
    val textureId: Int

    init {
        val fbos = IntArray(1)
        GLES31.glGenFramebuffers(1, fbos, 0)
        fboId = fbos[0]

        val textures = IntArray(1)
        GLES31.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureId)
        GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGBA, width, height, 0, GLES31.GL_RGBA, GLES31.GL_UNSIGNED_BYTE, null)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)

        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fboId)
        GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, textureId, 0)
        val status = GLES31.glCheckFramebufferStatus(GLES31.GL_FRAMEBUFFER)
        if (status != GLES31.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer incomplete: $status")
        }
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)
    }

    fun bind() = GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fboId)
    fun unbind() = GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)

    fun delete() {
        GLES31.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
        GLES31.glDeleteTextures(1, intArrayOf(textureId), 0)
    }
}
