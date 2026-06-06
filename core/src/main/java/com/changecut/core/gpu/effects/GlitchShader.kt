package com.changecut.core.gpu.effects

import com.changecut.core.gpu.GlShaderProgram

object GlitchShader {
    fun create(
        intensity: Float = 0.3f,
        speed: Float = 1.0f
    ): GlShaderProgram {
        val frag = """
            #version 300 es
            precision highp float;
            in vec2 vTexCoord;
            out vec4 fragColor;
            uniform sampler2D uTexture;
            uniform vec2 uResolution;
            uniform float uTime;

            float rand(vec2 co) {
                return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
            }

            void main() {
                vec2 texCoord = vTexCoord;
                vec4 color = texture(uTexture, texCoord);
                float t = uTime * $speed;

                // Horizontal displacement glitch
                float glitchProb = $intensity * 0.3;
                if (rand(vec2(floor(texCoord.y * 100.0 + t * 5.0), t)) < glitchProb) {
                    float shift = (rand(vec2(t, texCoord.y * 50.0)) - 0.5) * 0.1 * $intensity;
                    texCoord.x = clamp(texCoord.x + shift, 0.0, 1.0);
                    color.r = texture(uTexture, texCoord).r;
                    texCoord.x = clamp(texCoord.x - shift * 2.0, 0.0, 1.0);
                    color.b = texture(uTexture, texCoord).b;
                }

                // RGB split
                if (rand(vec2(floor(texCoord.y * 50.0), t + 1.0)) < $intensity * 0.2) {
                    float split = (rand(vec2(t, texCoord.y * 30.0 + 1.0)) - 0.5) * 0.02;
                    vec2 rCoord = vec2(clamp(texCoord.x + split, 0.0, 1.0), texCoord.y);
                    vec2 bCoord = vec2(clamp(texCoord.x - split, 0.0, 1.0), texCoord.y);
                    color.r = texture(uTexture, rCoord).r;
                    color.b = texture(uTexture, bCoord).b;
                }

                // Vertical bar glitch
                if (rand(vec2(floor(texCoord.x * 200.0), t * 2.0)) < $intensity * 0.15) {
                    float barY = floor(texCoord.y * 50.0 + t * 8.0) / 50.0;
                    if (abs(texCoord.y - barY) < 0.01) {
                        color = vec4(1.0, 1.0, 1.0, 1.0);
                    }
                }

                fragColor = color;
            }
        """.trimIndent()
        return GlShaderProgram(GlShaderProgram.FULLSCREEN_QUAD_VERTEX, frag)
    }
}
