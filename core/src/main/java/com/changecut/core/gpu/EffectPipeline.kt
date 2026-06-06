package com.changecut.core.gpu

import android.opengl.GLES31
import com.changecut.core.editor.EditorClip
import com.changecut.core.gpu.effects.AnimationShader
import com.changecut.core.gpu.effects.BlendShader
import com.changecut.core.gpu.effects.ChromaKeyShader
import com.changecut.core.gpu.effects.ColorGradeShader
import com.changecut.core.gpu.effects.MaskShader
import com.changecut.core.gpu.effects.TransitionShader

class EffectPipeline(private val maxWidth: Int, private val maxHeight: Int) {
    private val fboChain = mutableListOf<GlFrameBuffer>()
    private var currentIndex = 0

    fun begin(clip: EditorClip): Int {
        currentIndex = 0
        val shaders = buildShaders(clip)
        val requiredPasses = shaders.size
        while (fboChain.size < requiredPasses) {
            fboChain.add(GlFrameBuffer(maxWidth, maxHeight))
        }
        return 0
    }

    fun processFrame(inputTexId: Int, clip: EditorClip): Int {
        val shaders = buildShaders(clip)
        if (shaders.isEmpty()) return inputTexId

        var currentTex = inputTexId
        for (shader in shaders) {
            val fbo = fboChain[currentIndex]
            fbo.bind()
            GLES31.glViewport(0, 0, fbo.width, fbo.height)
            GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)

            shader.use()
            shader.setInt("uTexture", 0)
            GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, currentTex)

            drawQuad(shader)
            fbo.unbind()
            currentTex = fbo.textureId
            currentIndex++
        }
        return currentTex
    }

    private fun buildShaders(clip: EditorClip): List<GlShaderProgram> {
        val list = mutableListOf<GlShaderProgram>()
        clip.mask?.let { list.add(MaskShader.create(it)) }
        if (clip.blendMode.ordinal > 0) list.add(BlendShader.create(clip.blendMode))
        clip.animationIn?.let { list.add(AnimationShader.create(it)) }
        clip.colorGrade?.let { list.add(ColorGradeShader.create(it)) }
        if (clip.effectId == "chromakey") list.add(ChromaKeyShader.create())
        clip.transitionIn?.let { list.add(TransitionShader.create(it)) }
        return list
    }

    private fun drawQuad(shader: GlShaderProgram) { /* impl same as GlVideoRenderer */ }

    fun release() {
        fboChain.forEach { it.delete() }
        fboChain.clear()
    }
}
