package com.changecut.core.gpu.effects

import com.changecut.core.gpu.GlShaderProgram

object StabilizeShader {
    fun create(): GlShaderProgram {
        val frag = """
            #version 300 es
            precision highp float;
            in vec2 vTexCoord;
            out vec4 fragColor;
            uniform sampler2D uTexture;
            uniform vec2 uResolution;
            uniform float uTime;

            // Placeholder: uniform mat3 uStabMatrix would come from gyro data

            void main() {
                // Apply stabilization matrix (identity = no warp)
                vec2 texCoord = vTexCoord;

                // Simulated stabilization: slight crop + counter-shake
                float cropScale = 0.9;
                vec2 center = vec2(0.5, 0.5);
                texCoord = center + (texCoord - center) / cropScale;

                // Clamp to prevent edge artifacts
                if (texCoord.x < 0.0 || texCoord.x > 1.0 || texCoord.y < 0.0 || texCoord.y > 1.0) {
                    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
                    return;
                }

                fragColor = texture(uTexture, texCoord);
            }
        """.trimIndent()
        return GlShaderProgram(GlShaderProgram.FULLSCREEN_QUAD_VERTEX, frag)
    }
}
