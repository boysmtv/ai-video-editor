package com.changecut.core.ffmpeg

import com.changecut.core.editor.BlendMode
import com.changecut.core.editor.MaskDef
import com.changecut.core.editor.MaskType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaskEngine @Inject constructor() {

    fun buildMaskFilter(mask: MaskDef, width: Int = 1920, height: Int = 1080): String {
        val (cx, cy) = (mask.centerX * width).toInt() to (mask.centerY * height).toInt()
        val (mw, mh) = (mask.width * width).toInt() to (mask.height * height).toInt()
        val halfW = mw / 2
        val halfH = mh / 2
        val x1 = (cx - halfW).coerceAtLeast(0)
        val y1 = (cy - halfH).coerceAtLeast(0)
        val x2 = (cx + halfW).coerceAtMost(width)
        val y2 = (cy + halfH).coerceAtMost(height)

        val baseMask = when (mask.type) {
            MaskType.RECTANGLE -> buildRectangleMask(x1, y1, x2, y2, width, height)
            MaskType.LINEAR -> buildLinearMask(cx, cy, mask.rotation, width, height, mask.featherPx)
            MaskType.RADIAL -> buildRadialMask(cx, cy, halfW, mask.featherPx, width, height)
            MaskType.HEART -> buildHeartMask(cx, cy, halfW, width, height)
            MaskType.STAR -> buildStarMask(cx, cy, halfW, width, height)
            MaskType.CUSTOM -> buildCustomMask(mask.points, width, height)
        }

        return if (mask.invert) "negate=${baseMask}" else baseMask
    }

    fun buildBlendFilter(blendMode: BlendMode): String {
        if (blendMode == BlendMode.NORMAL) return ""
        return "blend=all_mode=${blendMode.ffmpegName}"
    }

    fun buildBlendFilterWithMask(blendMode: BlendMode, maskFilter: String): String {
        if (blendMode == BlendMode.NORMAL && maskFilter.isEmpty()) return ""
        val parts = mutableListOf<String>()
        if (maskFilter.isNotEmpty()) parts.add(maskFilter)
        if (blendMode != BlendMode.NORMAL) parts.add("blend=all_mode=${blendMode.ffmpegName}")
        return parts.joinToString(",")
    }

    private fun buildRectangleMask(x1: Int, y1: Int, x2: Int, y2: Int, w: Int, h: Int): String {
        return "drawbox=x=$x1:y=$y1:w=${x2 - x1}:h=${y2 - y1}:color=white@1:t=fill"
    }

    private fun buildLinearMask(cx: Int, cy: Int, rotation: Float, w: Int, h: Int, feather: Float): String {
        val angle = java.lang.Math.toRadians(rotation.toDouble())
        val dx = (java.lang.Math.cos(angle) * w).toInt()
        val dy = (java.lang.Math.sin(-angle) * h).toInt()
        val x1 = (cx - dx / 2).coerceIn(0, w)
        val y1 = (cy - dy / 2).coerceIn(0, h)
        val x2 = (cx + dx / 2).coerceIn(0, w)
        val y2 = (cy + dy / 2).coerceIn(0, h)
        return "geq=lum='if(gt(X*($y2-$y1)-Y*($x2-$x1)+$x2*$y1-$y2*$x1,0),255,0)':a='255'"
    }

    private fun buildRadialMask(cx: Int, cy: Int, radius: Int, feather: Float, w: Int, h: Int): String {
        val r2 = radius * radius
        return "geq=lum='if(lt((X-$cx)*(X-$cx)+(Y-$cy)*(Y-$cy),$r2),255,0)':a='255'"
    }

    private fun buildHeartMask(cx: Int, cy: Int, size: Int, w: Int, h: Int): String {
        val s = size.toFloat()
        return "geq=lum='let(xx=(X-$cx)/$s,yy=(Y-$cy)/$s-0.4,if(lt(pow(xx*xx+yy*yy-1,3)-xx*xx*yy*y*yy*yy,0),255,0))':a='255'"
    }

    private fun buildStarMask(cx: Int, cy: Int, size: Int, w: Int, h: Int): String {
        val s = size.toFloat()
        val pts = 5
        return "geq=lum='let(r=hypot(X-$cx,Y-$cy)/$s,ang=atan2(Y-$cy,X-$cx)+PI/2," +
                "if(lt(r,cos(ang*$pts)/0.95+0.5),255,0))':a='255'"
    }

    private fun buildCustomMask(points: List<Pair<Float, Float>>, w: Int, h: Int): String {
        if (points.isEmpty()) return "drawbox=x=0:y=0:w=$w:h=$h:color=white@1:t=fill"
        val ptsStr = points.joinToString("|") { (x, y) ->
            "${(x * w).toInt()},${(y * h).toInt()}"
        }
        return "geq=lum='if(eq(pocket(X,Y,$ptsStr),1),255,0)':a='255'"
    }

    companion object {
        private const val TAG = "MaskEngine"
    }
}
