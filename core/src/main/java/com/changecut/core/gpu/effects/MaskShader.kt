package com.changecut.core.gpu.effects

import com.changecut.core.editor.MaskDef
import com.changecut.core.editor.MaskType
import com.changecut.core.gpu.GlShaderProgram

object MaskShader {
    fun create(mask: MaskDef): GlShaderProgram {
        val cx = "(${mask.centerX}f)"
        val cy = "(${mask.centerY}f)"
        val w = "(${mask.width}f)"
        val h = "(${mask.height}f)"
        val feather = "(${mask.featherPx}f)"
        val inv = if (mask.invert) "1.0" else "0.0"
        val rotation = "(${mask.rotation}f)"

        val maskLogic: String = when (mask.type) {
            MaskType.RECTANGLE -> """
                vec2 pos = vTexCoord - vec2($cx, $cy);
                vec2 halfSize = vec2($w * 0.5, $h * 0.5);
                vec2 dist = abs(pos) - halfSize;
                float maskVal = 1.0 - smoothstep(-$feather * 0.002, $feather * 0.002, max(dist.x, dist.y));
            """.trimIndent()

            MaskType.LINEAR -> """
                vec2 dir = vec2(cos($rotation), sin($rotation));
                float proj = dot(vTexCoord - vec2($cx, $cy), dir);
                float maskVal = 1.0 - smoothstep(-$feather * 0.002, $feather * 0.002, proj);
            """.trimIndent()

            MaskType.RADIAL -> """
                float dist = distance(vTexCoord, vec2($cx, $cy));
                float radius = $w * 0.5;
                float maskVal = 1.0 - smoothstep(radius - $feather * 0.002, radius + $feather * 0.002, dist);
            """.trimIndent()

            MaskType.HEART -> """
                float hx = (vTexCoord.x - $cx) / $w * 2.0;
                float hy = (vTexCoord.y - $cy) / $h * 2.0;
                float h_val = hx * hx + hy * hy - 1.0;
                float heart = h_val * h_val * h_val - hx * hx * hy * hy * hy;
                float maskVal = 1.0 - smoothstep(-$feather * 0.01, $feather * 0.01, heart);
            """.trimIndent()

            MaskType.STAR -> """
                float sx = (vTexCoord.x - $cx) / ($w * 0.5);
                float sy = (vTexCoord.y - $cy) / ($h * 0.5);
                float a = atan(sy, sx);
                float r = length(vec2(sx, sy));
                float n = 5.0;
                float starVal = cos(a * n) * 0.5 + 0.5;
                float maskVal = 1.0 - smoothstep(starVal * 0.8 - $feather * 0.01, starVal * 0.8 + $feather * 0.01, r);
            """.trimIndent()

            MaskType.CUSTOM -> "float maskVal = 1.0;"
        }

        val frag = """
            #version 300 es
            precision highp float;
            in vec2 vTexCoord;
            out vec4 fragColor;
            uniform sampler2D uTexture;

            void main() {
                vec4 color = texture(uTexture, vTexCoord);
                $maskLogic
                maskVal = mix(maskVal, 1.0 - maskVal, $inv);
                fragColor = vec4(color.rgb, color.a * maskVal);
            }
        """.trimIndent()
        return GlShaderProgram(GlShaderProgram.FULLSCREEN_QUAD_VERTEX, frag)
    }
}
