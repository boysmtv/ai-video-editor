package com.changecut.core.gpu.effects

import com.changecut.core.editor.AnimationDef
import com.changecut.core.editor.AnimationDirection
import com.changecut.core.editor.AnimationType
import com.changecut.core.gpu.GlShaderProgram

object AnimationShader {
    fun create(anim: AnimationDef): GlShaderProgram {
        val durMs = anim.durationMs
        val dir = when (anim.direction) {
            AnimationDirection.NONE -> "vec2(0.0, 0.0)"
            AnimationDirection.LEFT -> "vec2(-1.0, 0.0)"
            AnimationDirection.RIGHT -> "vec2(1.0, 0.0)"
            AnimationDirection.TOP -> "vec2(0.0, -1.0)"
            AnimationDirection.BOTTOM -> "vec2(0.0, 1.0)"
            AnimationDirection.TOP_LEFT -> "vec2(-1.0, -1.0)"
            AnimationDirection.TOP_RIGHT -> "vec2(1.0, -1.0)"
            AnimationDirection.BOTTOM_LEFT -> "vec2(-1.0, 1.0)"
            AnimationDirection.BOTTOM_RIGHT -> "vec2(1.0, 1.0)"
        }

        val animLogic = when (anim.type) {
            AnimationType.NONE -> "// no animation"
            AnimationType.FADE -> """
                float alpha = clamp(uTime * 1000.0 / ${durMs}.0, 0.0, 1.0);
                color *= alpha;
            """.trimIndent()

            AnimationType.SLIDE -> """
                float t = clamp(uTime * 1000.0 / ${durMs}.0, 0.0, 1.0);
                vec2 offset = $dir * (1.0 - t);
                texCoord = clamp(texCoord + offset * 0.5, 0.0, 1.0);
            """.trimIndent()

            AnimationType.ZOOM -> """
                float t = clamp(uTime * 1000.0 / ${durMs}.0, 0.0, 1.0);
                float scale = 0.3 + 0.7 * t;
                vec2 center = vec2(0.5, 0.5);
                texCoord = center + (texCoord - center) / scale;
            """.trimIndent()

            AnimationType.BOUNCE -> """
                float t = clamp(uTime * 1000.0 / ${durMs}.0, 0.0, 1.0);
                float bounce = sin(t * 3.14159 * 3.0) * (1.0 - t) * 0.3;
                vec2 offset = $dir * bounce;
                texCoord = clamp(texCoord + offset * 0.3, 0.0, 1.0);
            """.trimIndent()

            AnimationType.ROTATE -> """
                float t = clamp(uTime * 1000.0 / ${durMs}.0, 0.0, 1.0);
                float angle = (1.0 - t) * 6.28319 * 0.25;
                vec2 center = vec2(0.5, 0.5);
                vec2 rel = texCoord - center;
                float c = cos(angle * t);
                float s_ = sin(angle * t);
                texCoord = center + vec2(rel.x * c - rel.y * s_, rel.x * s_ + rel.y * c);
            """.trimIndent()

            AnimationType.BLUR -> """
                float t = 1.0 - clamp(uTime * 1000.0 / ${durMs}.0, 0.0, 1.0);
                vec2 texel = 1.0 / uResolution;
                vec4 blur = vec4(0.0);
                for (int i = -4; i <= 4; i++) {
                    blur += texture(uTexture, texCoord + vec2(0.0, float(i)) * texel * t * 10.0);
                }
                color = blur / 9.0;
            """.trimIndent()

            AnimationType.WOBBLE -> """
                float t = clamp(uTime * 1000.0 / ${durMs}.0, 0.0, 1.0);
                float wobble = sin(texCoord.y * 50.0 + uTime * 10.0) * (1.0 - t) * 0.02;
                texCoord.x += wobble;
            """.trimIndent()

            AnimationType.FLIP -> """
                float t = clamp(uTime * 1000.0 / ${durMs}.0, 0.0, 1.0);
                float flip = cos(t * 3.14159);
                texCoord.x = mix(texCoord.x, 1.0 - texCoord.x, flip * 0.5 + 0.5);
                float scale = abs(flip);
                texCoord = vec2(0.5, 0.5) + (texCoord - vec2(0.5, 0.5)) / max(scale, 0.1);
            """.trimIndent()
        }

        val frag = """
            #version 300 es
            precision highp float;
            in vec2 vTexCoord;
            out vec4 fragColor;
            uniform sampler2D uTexture;
            uniform vec2 uResolution;
            uniform float uTime;

            void main() {
                vec2 texCoord = vTexCoord;
                vec4 color = texture(uTexture, texCoord);
                $animLogic
                fragColor = color;
            }
        """.trimIndent()
        return GlShaderProgram(GlShaderProgram.FULLSCREEN_QUAD_VERTEX, frag)
    }
}
