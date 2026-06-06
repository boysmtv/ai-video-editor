package com.changecut.core.gpu

import android.opengl.GLES31
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class GlShaderProgram(
    vertexSource: String,
    fragmentSource: String
) {
    val programId: Int
    private val attributeLocations = mutableMapOf<String, Int>()
    private val uniformLocations = mutableMapOf<String, Int>()

    init {
        val vertex = compileShader(GLES31.GL_VERTEX_SHADER, vertexSource)
        val fragment = compileShader(GLES31.GL_FRAGMENT_SHADER, fragmentSource)
        programId = GLES31.glCreateProgram().also { prog ->
            GLES31.glAttachShader(prog, vertex)
            GLES31.glAttachShader(prog, fragment)
            GLES31.glLinkProgram(prog)
            val status = IntArray(1)
            GLES31.glGetProgramiv(prog, GLES31.GL_LINK_STATUS, status, 0)
            if (status[0] == 0) {
                val log = GLES31.glGetProgramInfoLog(prog)
                GLES31.glDeleteProgram(prog)
                throw RuntimeException("Program link failed: $log")
            }
            GLES31.glDeleteShader(vertex)
            GLES31.glDeleteShader(fragment)
        }
    }

    fun use() = GLES31.glUseProgram(programId)

    fun getAttrib(name: String): Int = attributeLocations.getOrPut(name) {
        GLES31.glGetAttribLocation(programId, name).also { loc ->
            if (loc == -1) throw RuntimeException("Attribute $name not found")
        }
    }

    fun getUniform(name: String): Int = uniformLocations.getOrPut(name) {
        GLES31.glGetUniformLocation(programId, name).also { loc ->
            if (loc == -1) throw RuntimeException("Uniform $name not found")
        }
    }

    fun setInt(name: String, value: Int) = GLES31.glUniform1i(getUniform(name), value)
    fun setFloat(name: String, value: Float) = GLES31.glUniform1f(getUniform(name), value)
    fun setVec2(name: String, x: Float, y: Float) = GLES31.glUniform2f(getUniform(name), x, y)
    fun setVec3(name: String, x: Float, y: Float, z: Float) = GLES31.glUniform3f(getUniform(name), x, y, z)
    fun setVec4(name: String, x: Float, y: Float, z: Float, w: Float) = GLES31.glUniform4f(getUniform(name), x, y, z, w)
    fun setMat4(name: String, matrix: FloatArray) = GLES31.glUniformMatrix4fv(getUniform(name), 1, false, matrix, 0)
    fun setFloatArray(name: String, values: FloatArray) = GLES31.glUniform1fv(getUniform(name), values.size, values, 0)

    fun delete() = GLES31.glDeleteProgram(programId)

    companion object {
        private fun compileShader(type: Int, source: String): Int {
            return GLES31.glCreateShader(type).also { shader ->
                GLES31.glShaderSource(shader, source)
                GLES31.glCompileShader(shader)
                val status = IntArray(1)
                GLES31.glGetShaderiv(shader, GLES31.GL_COMPILE_STATUS, status, 0)
                if (status[0] == 0) {
                    val log = GLES31.glGetShaderInfoLog(shader)
                    GLES31.glDeleteShader(shader)
                    val typeName = if (type == GLES31.GL_VERTEX_SHADER) "vertex" else "fragment"
                    throw RuntimeException("$typeName shader compile failed: $log")
                }
            }
        }

        val FULLSCREEN_QUAD_VERTEX = """
            #version 300 es
            in vec4 aPosition;
            in vec2 aTexCoord;
            out vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        fun createFullscreenQuad(): FloatBuffer {
            val vertices = floatArrayOf(
                -1f, -1f, 0f, 0f,
                 1f, -1f, 1f, 0f,
                -1f,  1f, 0f, 1f,
                 1f,  1f, 1f, 1f
            )
            return ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertices)
                .apply { position(0) }
        }
    }
}
