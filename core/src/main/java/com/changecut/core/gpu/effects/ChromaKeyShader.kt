package com.changecut.core.gpu.effects

import com.changecut.core.gpu.GlShaderProgram

object ChromaKeyShader {
    fun create(
        keyColorR: Float = 0f,
        keyColorG: Float = 1f,
        keyColorB: Float = 0f,
        similarity: Float = 0.4f,
        smoothness: Float = 0.1f,
        spillReduce: Float = 0.1f
    ): GlShaderProgram {
        val frag = """
            #version 300 es
            precision highp float;
            in vec2 vTexCoord;
            out vec4 fragColor;
            uniform sampler2D uTexture;

            void main() {
                vec4 color = texture(uTexture, vTexCoord);
                vec3 keyColor = vec3($keyColorR, $keyColorG, $keyColorB);
                float diff = length(color.rgb - keyColor);
                float mask = smoothstep($similarity, $similarity + $smoothness, diff);
                color.rgb = mix(color.rgb, vec3(0.0), (1.0 - mask) * $spillReduce);
                fragColor = vec4(color.rgb * mask, color.a * mask);
            }
        """.trimIndent()
        return GlShaderProgram(GlShaderProgram.FULLSCREEN_QUAD_VERTEX, frag)
    }
}
