package com.changecut.core.ffmpeg

import com.changecut.core.editor.ColorGradeDef
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ColorGradingEngine @Inject constructor() {

    fun buildHSLFilter(hue: Float, saturation: Float, lightness: Float): String {
        val parts = mutableListOf<String>()
        if (hue != 0f) parts.add("hue=h=${hue}:s=0")
        if (saturation != 0f) parts.add("eq=saturation=${1 + saturation / 100}")
        if (lightness != 0f) parts.add("eq=brightness=${lightness / 100}")
        return parts.joinToString(",")
    }

    fun buildCurvesFilter(curves: ColorGradeDef): String {
        val rgb = curves.rgbCurve
        val r = curves.redCurve
        val g = curves.greenCurve
        val b = curves.blueCurve
        if (r.size < 2 || g.size < 2 || b.size < 2) return ""
        val rgbStr = rgb.joinToString(" ") { (it * 255).toInt().toString() }
        val rStr = r.joinToString(" ") { (it * 255).toInt().toString() }
        val gStr = g.joinToString(" ") { (it * 255).toInt().toString() }
        val bStr = b.joinToString(" ") { (it * 255).toInt().toString() }
        return "curves=master='$rgbStr':red='$rStr':green='$gStr':blue='$bStr'"
    }

    fun buildVignetteFilter(intensity: Float, width: Int = 1920, height: Int = 1080): String {
        if (intensity <= 0f) return ""
        val cx = width / 2f
        val cy = height / 2f
        val maxDist = kotlin.math.sqrt((cx * cx + cy * cy).toDouble()).toFloat()
        return "geq=lum='lum(X,Y)':a='if(lt(hypot(X-$cx,Y-$cy),$maxDist),255*(1-$intensity*((hypot(X-$cx,Y-$cy))/$maxDist)),255)'"
    }

    fun buildLUTFilter(lutPath: String): String {
        return "lut3d=file='$lutPath'"
    }

    fun buildFullColorGrade(grade: ColorGradeDef, width: Int = 1920, height: Int = 1080): String {
        val parts = mutableListOf<String>()
        if (grade.hslHue != 0f || grade.hslSaturation != 0f || grade.hslLightness != 0f) {
            parts.add(buildHSLFilter(grade.hslHue, grade.hslSaturation, grade.hslLightness))
        }
        parts.add(buildCurvesFilter(grade))
        if (grade.vignetteIntensity > 0f) {
            parts.add(buildVignetteFilter(grade.vignetteIntensity, width, height))
        }
        if (grade.lutPath != null) {
            parts.add(buildLUTFilter(grade.lutPath))
        }
        return parts.joinToString(",")
    }
}
