package com.changecut.core.gpu.effects

import com.changecut.core.editor.ColorGradeDef
import com.changecut.core.gpu.GlShaderProgram

object ColorGradeShader {
    fun create(grade: ColorGradeDef): GlShaderProgram {
        val h = grade.hslHue; val s = grade.hslSaturation; val l = grade.hslLightness
        val vi = grade.vignetteIntensity

        val frag = """
            #version 300 es
            precision highp float;
            in vec2 vTexCoord;
            out vec4 fragColor;
            uniform sampler2D uTexture;
            uniform vec2 uResolution;

            void main() {
                vec4 color = texture(uTexture, vTexCoord);

                // HSL adjustment
                float hue = $h;
                float c = cos(hue * 3.14159);
                float s_ = sin(hue * 3.14159);
                mat3 hueRot = mat3(
                    0.299 + 0.701*c + 0.168*s_, 0.587 - 0.587*c + 0.330*s_, 0.114 - 0.114*c - 0.497*s_,
                    0.299 - 0.299*c - 0.328*s_, 0.587 + 0.413*c + 0.035*s_, 0.114 - 0.114*c + 0.292*s_,
                    0.299 - 0.299*c + 1.25*s_, 0.587 - 0.587*c - 1.05*s_, 0.114 + 0.886*c - 0.203*s_
                );
                color.rgb = hueRot * color.rgb;

                // Saturation
                float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                color.rgb = mix(vec3(gray), color.rgb, clamp(1.0 + $s, 0.0, 2.0));

                // Brightness/Lightness
                color.rgb = clamp(color.rgb * (1.0 + $l * 0.5), 0.0, 1.0);

                // Vignette
                float dist = distance(vTexCoord, vec2(0.5, 0.5)) * 2.0;
                float vignette = 1.0 - clamp($vi, 0.0, 1.0) * dist * dist;
                color.rgb *= vignette;

                fragColor = color;
            }
        """.trimIndent()
        return GlShaderProgram(GlShaderProgram.FULLSCREEN_QUAD_VERTEX, frag)
    }
}
