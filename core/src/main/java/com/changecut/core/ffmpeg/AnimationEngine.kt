package com.changecut.core.ffmpeg

import com.changecut.core.editor.AnimationDef
import com.changecut.core.editor.AnimationDirection
import com.changecut.core.editor.AnimationType
import com.changecut.core.editor.EasingType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimationEngine @Inject constructor() {

    fun buildInAnimation(animation: AnimationDef, durationMs: Int, width: Int = 1920, height: Int = 1080): String {
        val durSec = durationMs / 1000f
        return when (animation.type) {
            AnimationType.FADE -> "fade=in:st=0:d=$durSec"
            AnimationType.SLIDE -> buildSlideIn(animation.direction, durSec, width, height)
            AnimationType.ZOOM -> "zoompan=z='if(lte(in,1),1+($durSec-in)*0.1,1)':d=1:x=iw/2:y=ih/2:s=${width}x$height"
            AnimationType.BOUNCE -> buildBounceIn(durSec, width, height)
            AnimationType.ROTATE -> "rotate='if(lte(t,$durSec),PI*2*t/$durSec,0)':ow=rotw(PI*2):oh=roth(PI*2)"
            AnimationType.BLUR -> "boxblur=enable='lt(t,$durSec)':luma_radius='if(lt(t,$durSec),20*(1-t/$durSec),0)'"
            AnimationType.WOBBLE -> buildWobble(durSec, width, height)
            AnimationType.FLIP -> "hflip=enable='lt(t,$durSec)'"
            AnimationType.NONE -> ""
        }
    }

    fun buildOutAnimation(animation: AnimationDef, durationMs: Int, totalDurationMs: Int, width: Int = 1920, height: Int = 1080): String {
        val durSec = durationMs / 1000f
        val startSec = (totalDurationMs - durationMs) / 1000f
        val enable = "enable='gte(t,$startSec)'"
        return when (animation.type) {
            AnimationType.FADE -> "fade=out:st=$startSec:d=$durSec"
            AnimationType.SLIDE -> buildSlideOut(animation.direction, durSec, startSec, width, height)
            AnimationType.ZOOM -> "$enable,zoompan=z='if(gte(t,$startSec),1-($startSec+durSec-t)*0.1,1)':d=1:x=iw/2:y=ih/2:s=${width}x$height"
            AnimationType.BOUNCE -> "$enable,${buildBounceOut(durSec, width, height)}"
            AnimationType.ROTATE -> "$enable,rotate='if(gte(t,$startSec),PI*2*($startSec+durSec-t)/$durSec,0)':ow=rotw(PI*2):oh=roth(PI*2)"
            AnimationType.BLUR -> "$enable,boxblur=luma_radius='if(gte(t,$startSec),20*(t-$startSec)/$durSec,0)'"
            AnimationType.WOBBLE -> "$enable,${buildWobble(durSec, width, height)}"
            AnimationType.FLIP -> "$enable,hflip='mod(floor((t-$startSec)*10),2)'"
            AnimationType.NONE -> ""
        }
    }

    private fun buildSlideIn(direction: AnimationDirection, durSec: Float, w: Int, h: Int): String {
        return when (direction) {
            AnimationDirection.LEFT -> "crop=iw-${((w * 0.3).toInt())}*if(lt(t,$durSec),1-t/$durSec,0):ih:0:0"
            AnimationDirection.RIGHT -> "crop=iw-${((w * 0.3).toInt())}*if(lt(t,$durSec),1-t/$durSec,0):ih:${((w * 0.3).toInt())}*if(lt(t,$durSec),1-t/$durSec,0):0"
            AnimationDirection.TOP -> "crop=iw:ih-${((h * 0.3).toInt())}*if(lt(t,$durSec),1-t/$durSec,0):0:0"
            AnimationDirection.BOTTOM -> "crop=iw:ih-${((h * 0.3).toInt())}*if(lt(t,$durSec),1-t/$durSec,0):0:${((h * 0.3).toInt())}*if(lt(t,$durSec),1-t/$durSec,0)"
            else -> "fade=in:st=0:d=$durSec"
        }
    }

    private fun buildSlideOut(direction: AnimationDirection, durSec: Float, startSec: Float, w: Int, h: Int): String {
        return when (direction) {
            AnimationDirection.LEFT -> "crop=iw-${((w * 0.3).toInt())}*if(gte(t,$startSec),(t-$startSec)/$durSec,0):ih:${((w * 0.3).toInt())}*if(gte(t,$startSec),(t-$startSec)/$durSec,0):0"
            AnimationDirection.RIGHT -> "crop=iw-${((w * 0.3).toInt())}*if(gte(t,$startSec),(t-$startSec)/$durSec,0):ih:0:0"
            AnimationDirection.TOP -> "crop=iw:ih-${((h * 0.3).toInt())}*if(gte(t,$startSec),(t-$startSec)/$durSec,0):0:${((h * 0.3).toInt())}*if(gte(t,$startSec),(t-$startSec)/$durSec,0)"
            AnimationDirection.BOTTOM -> "crop=iw:ih-${((h * 0.3).toInt())}*if(gte(t,$startSec),(t-$startSec)/$durSec,0):0:0"
            else -> "fade=out:st=$startSec:d=$durSec"
        }
    }

    private fun buildBounceIn(durSec: Float, w: Int, h: Int): String {
        return "zoompan=z='if(lte(t,$durSec),1+0.3*${easeOutBounceExpr("t/$durSec")},1)':d=1:x=iw/2:y=ih/2:s=${w}x$h"
    }

    private fun buildBounceOut(durSec: Float, w: Int, h: Int): String {
        return "zoompan=z='if(lt(t,$durSec),1+0.3*${easeOutBounceExpr("($durSec-t)/$durSec")},1)':d=1:x=iw/2:y=ih/2:s=${w}x$h"
    }

    private fun buildWobble(durSec: Float, w: Int, h: Int): String {
        return "rotate='if(lt(t,$durSec),sin(10*t*PI)*5*(1-t/$durSec),0)':ow=rotw(5):oh=roth(5)"
    }

    private fun easeOutBounceExpr(x: String): String {
        return "if(lt($x,1/2.75),7.5625*$x*$x,if(lt($x,2/2.75),7.5625*($x-1.5/2.75)*($x-1.5/2.75)+0.75,if(lt($x,2.5/2.75),7.5625*($x-2.25/2.75)*($x-2.25/2.75)+0.9375,7.5625*($x-2.625/2.75)*($x-2.625/2.75)+0.984375)))"
    }
}
