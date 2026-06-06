package com.changecut.core.gpu.effects

import com.changecut.core.editor.BlendMode
import com.changecut.core.gpu.GlShaderProgram

object BlendShader {
    fun create(mode: BlendMode): GlShaderProgram {
        val blendCode = when (mode) {
            BlendMode.NORMAL -> "result = overlay;"
            BlendMode.SCREEN -> "result = 1.0 - (1.0 - base) * (1.0 - overlay);"
            BlendMode.MULTIPLY -> "result = base * overlay;"
            BlendMode.OVERLAY -> """
                result = base < 0.5 ? 2.0 * base * overlay : 1.0 - 2.0 * (1.0 - base) * (1.0 - overlay);
            """.trimIndent()
            BlendMode.ADD -> "result = min(base + overlay, 1.0);"
            BlendMode.DARKEN -> "result = min(base, overlay);"
            BlendMode.LIGHTEN -> "result = max(base, overlay);"
            BlendMode.DIFFERENCE -> "result = abs(base - overlay);"
            BlendMode.HARD_LIGHT -> """
                result = overlay < 0.5 ? 2.0 * base * overlay : 1.0 - 2.0 * (1.0 - base) * (1.0 - overlay);
            """.trimIndent()
            BlendMode.SOFT_LIGHT -> """
                result = 2.0 * base * overlay + base * base * (1.0 - 2.0 * overlay);
            """.trimIndent()
            BlendMode.EXCLUSION -> "result = base + overlay - 2.0 * base * overlay;"
            BlendMode.DIVIDE -> "result = min(base / max(overlay, 0.001), 1.0);"
        }

        val frag = """
            #version 300 es
            precision highp float;
            in vec2 vTexCoord;
            out vec4 fragColor;
            uniform sampler2D uTexture;
            uniform sampler2D uOverlay;

            void main() {
                vec4 base = texture(uTexture, vTexCoord);
                vec4 overlay = texture(uOverlay, vTexCoord);
                vec4 result;
                $blendCode
                fragColor = vec4(mix(base.rgb, result.rgb, overlay.a), base.a);
            }
        """.trimIndent()
        return GlShaderProgram(GlShaderProgram.FULLSCREEN_QUAD_VERTEX, frag)
    }
}
