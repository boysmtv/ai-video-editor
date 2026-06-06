package com.changecut.core.gpu.effects

import com.changecut.core.editor.TransitionDef
import com.changecut.core.gpu.GlShaderProgram

object TransitionShader {
    fun create(transition: TransitionDef): GlShaderProgram {
        val durMs = transition.durationMs
        val type = transition.type

        val transitionCode = when (type) {
            "fade" -> "mix(fromColor, toColor, progress);"
            "dissolve" -> "mix(fromColor, toColor, step(progress, rand(texCoord)));"
            "slideleft" -> """
                vec2 offset = texCoord + vec2(1.0 - progress, 0.0);
                mix(texture(uTexture, clamp(offset, 0.0, 1.0)), 
                    texture(uToTexture, texCoord - vec2(progress, 0.0)), 
                    step(progress, texCoord.x));
            """.trimIndent()
            "slideright" -> """
                vec2 offset = texCoord - vec2(1.0 - progress, 0.0);
                mix(texture(uTexture, clamp(offset, 0.0, 1.0)), 
                    texture(uToTexture, texCoord + vec2(progress, 0.0)), 
                    step(progress, 1.0 - texCoord.x));
            """.trimIndent()
            "slideup" -> """
                vec2 offset = texCoord + vec2(0.0, 1.0 - progress);
                mix(texture(uTexture, clamp(offset, 0.0, 1.0)), 
                    texture(uToTexture, texCoord - vec2(0.0, progress)), 
                    step(progress, texCoord.y));
            """.trimIndent()
            "slidedown" -> """
                vec2 offset = texCoord - vec2(0.0, 1.0 - progress);
                mix(texture(uTexture, clamp(offset, 0.0, 1.0)), 
                    texture(uToTexture, texCoord + vec2(0.0, progress)), 
                    step(progress, 1.0 - texCoord.y));
            """.trimIndent()
            "zoomin" -> """
                vec2 center = vec2(0.5, 0.5);
                vec2 fromCoord = center + (texCoord - center) / (1.0 - progress * 0.5);
                vec2 toCoord = center + (texCoord - center) * (1.0 + progress * 0.5);
                mix(texture(uTexture, fromCoord), 
                    texture(uToTexture, toCoord), 
                    progress);
            """.trimIndent()
            "circlewipe" -> """
                float dist = distance(texCoord, vec2(0.5, 0.5));
                float maxDist = 0.7071;
                float circleProgress = dist / maxDist;
                mix(fromColor, toColor, step(progress, circleProgress));
            """.trimIndent()
            "pixelate" -> """
                vec2 pixels = vec2(20.0);
                vec2 pTex = floor(texCoord * pixels) / pixels;
                mix(texture(uTexture, texCoord), 
                    texture(uToTexture, pTex), 
                    progress);
            """.trimIndent()
            "ripple" -> """
                vec2 center = vec2(0.5, 0.5);
                float dist = distance(texCoord, center);
                float ripple = sin(dist * 50.0 - progress * 6.283) * 0.5 + 0.5;
                mix(fromColor, toColor, ripple * progress);
            """.trimIndent()
            "doorwarp" -> """
                vec2 center = texCoord - 0.5;
                float angle = atan(center.y, center.x);
                float door = abs(angle) / 3.14159;
                mix(fromColor, toColor, step(progress, door));
            """.trimIndent()
            "swirl" -> """
                vec2 center = texCoord - 0.5;
                float dist = length(center);
                float angle = sin(dist * 10.0 - progress * 3.14159) * 0.3 * progress;
                float c = cos(angle); float s_ = sin(angle);
                vec2 swirlCoord = vec2(center.x * c - center.y * s_, center.x * s_ + center.y * c) + 0.5;
                mix(texture(uTexture, texCoord), 
                    texture(uToTexture, swirlCoord), 
                    progress);
            """.trimIndent()
            "cube" -> """
                vec2 dir = vec2(1.0, 0.0);
                float skew = progress * 0.3;
                vec2 fromCoord = texCoord + dir * skew;
                vec2 toCoord = texCoord - dir * (1.0 - progress) * 0.3;
                float edge = texCoord.x < progress ? 1.0 : 0.0;
                mix(texture(uTexture, fromCoord), 
                    texture(uToTexture, toCoord), 
                    edge);
            """.trimIndent()
            "glitch" -> """
                float block = floor(texCoord.y * 20.0 + progress * 10.0);
                float offset = sin(block * 3.14159 * 2.0 + progress * 10.0) * 0.1 * progress;
                vec2 glitchCoord = texCoord + vec2(offset, 0.0);
                mix(texture(uTexture, texCoord), 
                    texture(uToTexture, glitchCoord), 
                    progress);
            """.trimIndent()
            "burn" -> """
                float noise = fract(sin(dot(texCoord * 100.0, vec2(12.9898, 78.233))) * 43758.5453);
                float burnEdge = progress + noise * 0.1;
                float mask = smoothstep(burnEdge - 0.05, burnEdge + 0.05, 0.5);
                vec3 burnColor = vec3(1.0, 0.3, 0.0) * (1.0 - mask) * progress;
                vec3 mixed = mix(fromColor.rgb, toColor.rgb, mask);
                fragColor = vec4(mixed + burnColor, 1.0);
                return;
            """.trimIndent()
            "radialblur" -> """
                vec2 center = texCoord - 0.5;
                float dist = length(center);
                float blur = progress * (1.0 - dist) * 0.05;
                vec2 blurCoord = texCoord + center * blur;
                vec4 blurred = texture(uTexture, blurCoord);
                mix(blurred, texture(uToTexture, texCoord), progress * dist);
            """.trimIndent()
            else -> "mix(fromColor, toColor, progress);" // default crossfade
        }

        val frag = """
            #version 300 es
            precision highp float;
            in vec2 vTexCoord;
            out vec4 fragColor;
            uniform sampler2D uTexture;
            uniform sampler2D uToTexture;
            uniform float uTime;
            uniform vec2 uResolution;

            float rand(vec2 co) {
                return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
            }

            void main() {
                vec2 texCoord = vTexCoord;
                float progress = clamp(uTime / ${durMs}ms, 0.0, 1.0);
                vec4 fromColor = texture(uTexture, texCoord);
                vec4 toColor = texture(uToTexture, texCoord);

                $transitionCode

                // Default fallback
                fragColor = mix(fromColor, toColor, progress);
            }
        """.trimIndent()
        return GlShaderProgram(GlShaderProgram.FULLSCREEN_QUAD_VERTEX, frag)
    }
}
