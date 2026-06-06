package com.changecut.core.gpu.effects

import com.changecut.core.gpu.GlShaderProgram

object BeautyShader {
    fun create(
        smoothStrength: Float = 0.5f,
        skinTone: Float = 0.0f,
        brightness: Float = 0.0f,
        sharpenStrength: Float = 0.3f
    ): GlShaderProgram {
        val frag = """
            #version 300 es
            precision highp float;
            in vec2 vTexCoord;
            out vec4 fragColor;
            uniform sampler2D uTexture;
            uniform vec2 uResolution;

            void main() {
                vec4 color = texture(uTexture, vTexCoord);
                vec2 texel = 1.0 / uResolution;

                // Bilateral filter for skin smoothing
                float sigmaR = 0.1 * $smoothStrength + 0.02;
                float sigmaS = 2.0 * $smoothStrength + 1.0;

                vec4 sum = vec4(0.0);
                float totalWeight = 0.0;
                int radius = 2;

                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        vec2 offset = vec2(float(dx), float(dy)) * texel;
                        vec4 sample = texture(uTexture, vTexCoord + offset);
                        float spatial = exp(-float(dx*dx + dy*dy) / (2.0 * sigmaS * sigmaS));
                        vec3 diff = sample.rgb - color.rgb;
                        float range = exp(-dot(diff, diff) / (2.0 * sigmaR * sigmaR));
                        float weight = spatial * range;
                        sum += sample * weight;
                        totalWeight += weight;
                    }
                }

                vec4 smoothed = sum / max(totalWeight, 0.001);

                // Skin detection (simple RGB threshold)
                vec3 skinColor = vec3(0.8, 0.5, 0.3);
                vec3 skinDiff = abs(color.rgb - skinColor);
                float skinMask = 1.0 - smoothstep(0.1, 0.4, max(skinDiff.r, max(skinDiff.g, skinDiff.b)));

                // Blend based on skin mask
                vec4 result = mix(color, smoothed, skinMask * $smoothStrength);

                // Brighten
                result.rgb = mix(result.rgb, result.rgb * 1.1, max(0.0, $brightness));

                // Skin tone adjustment
                result.r = mix(result.r, result.r * (1.0 + $skinTone * 0.05), skinMask);
                result.b = mix(result.b, result.b * (1.0 - $skinTone * 0.05), skinMask);

                // Final sharpen
                vec4 blur = (texture(uTexture, vTexCoord + vec2(-texel.x, -texel.y)) +
                             texture(uTexture, vTexCoord + vec2(texel.x, -texel.y)) +
                             texture(uTexture, vTexCoord + vec2(-texel.x, texel.y)) +
                             texture(uTexture, vTexCoord + vec2(texel.x, texel.y))) * 0.25;
                vec4 diffSharp = result - blur;
                result = clamp(result + diffSharp * $sharpenStrength, 0.0, 1.0);

                fragColor = result;
            }
        """.trimIndent()
        return GlShaderProgram(GlShaderProgram.FULLSCREEN_QUAD_VERTEX, frag)
    }
}
