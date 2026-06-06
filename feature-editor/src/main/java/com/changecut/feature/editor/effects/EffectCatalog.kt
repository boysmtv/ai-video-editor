package com.changecut.feature.editor.effects

import com.changecut.core.editor.EffectCategory
import com.changecut.core.editor.EffectDef

object EffectCatalog {
    val visualEffects: List<EffectDef> = listOf(
        EffectDef("glitch_1", "Glitch", EffectCategory.GLITCH, "glitch=1"),
        EffectDef("glitch_2", "Glitch Heavy", EffectCategory.GLITCH, "glitch=3"),
        EffectDef("shake_1", "Shake", EffectCategory.SHAKE, "shake=5:5"),
        EffectDef("shake_2", "Shake Strong", EffectCategory.SHAKE, "shake=10:10"),
        EffectDef("zoom_1", "Zoom In", EffectCategory.ZOOM, "zoompan=z=zoom+0.002:d=25*s"),
        EffectDef("zoom_2", "Zoom Out", EffectCategory.ZOOM, "zoompan=z=zoom-0.002:d=25*s"),
        EffectDef("pulse_1", "Pulse", EffectCategory.ZOOM, "zoompan=z=1+0.5*sin(2*PI*t/25):d=1"),
        EffectDef("blur_1", "Blur Light", EffectCategory.BLUR, "boxblur=2:1"),
        EffectDef("blur_2", "Blur Medium", EffectCategory.BLUR, "boxblur=5:3"),
        EffectDef("blur_3", "Blur Heavy", EffectCategory.BLUR, "boxblur=10:5"),
        EffectDef("blur_4", "Gaussian Blur", EffectCategory.BLUR, "gblur=sigma=3"),
        EffectDef("shatter_1", "Shatter", EffectCategory.SHATTER, "shatter"),
        EffectDef("blend_overlay", "Overlay", EffectCategory.BLEND, "blend=all_mode=overlay"),
        EffectDef("blend_screen", "Screen", EffectCategory.BLEND, "blend=all_mode=screen"),
        EffectDef("blend_multiply", "Multiply", EffectCategory.BLEND, "blend=all_mode=multiply")
    )

    val colorEffects: List<EffectDef> = listOf(
        EffectDef("filter_vintage", "Vintage", EffectCategory.COLOR, "colorchannelmixer=.393:.769:.189:.349:.686:.168:.272:.534:.131"),
        EffectDef("filter_grayscale", "Grayscale", EffectCategory.COLOR, "colorchannelmixer=.3:.4:.3:0:.3:.4:.3:0:.3:.4:.3"),
        EffectDef("filter_sepia", "Sepia", EffectCategory.COLOR, "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131"),
        EffectDef("filter_vivid", "Vivid", EffectCategory.COLOR, "eq=saturation=2.0:contrast=1.2"),
        EffectDef("filter_cool", "Cool", EffectCategory.COLOR, "colorbalance=rs=-0.2:gs=-0.1:bs=0.3"),
        EffectDef("filter_warm", "Warm", EffectCategory.COLOR, "colorbalance=rs=0.3:gs=0.1:bs=-0.2"),
        EffectDef("filter_dramatic", "Dramatic", EffectCategory.COLOR, "eq=contrast=1.5:brightness=-0.1"),
        EffectDef("filter_noir", "Noir", EffectCategory.COLOR, "hue=s=0:eq=contrast=1.3:brightness=-0.05"),
        EffectDef("filter_fade", "Fade", EffectCategory.COLOR, "curves=master='0/0 0.5/0.4 1/1'")
    )

    private val allEffectsList: List<EffectDef> by lazy { visualEffects + colorEffects }

    private val effectsByCategory: Map<EffectCategory, List<EffectDef>> by lazy {
        allEffectsList.groupBy { it.category }
    }

    fun getAllEffects(): List<EffectDef> = allEffectsList

    fun getEffectsByCategory(category: EffectCategory): List<EffectDef> =
        effectsByCategory[category] ?: emptyList()
}
