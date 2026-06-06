package com.changecut.core.ffmpeg

import android.content.Context
import com.changecut.core.editor.FilterDef
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ffmpegExecutor: FfmpegExecutor
) {
    suspend fun applyFilter(
        videoPath: String,
        filterId: String,
        outputPath: String
    ): Result<String> {
        val filterDef = filters.find { it.id == filterId } ?: return Result.success(outputPath)
        if (filterDef.ffmpegFilter.isBlank()) return Result.success(outputPath)
        return execute(
            "-i", videoPath,
            "-vf", filterDef.ffmpegFilter,
            "-c:a", "copy",
            "-y", outputPath
        ).map { outputPath }
    }

    suspend fun applyFilters(
        videoPath: String,
        filterIds: List<String>,
        outputPath: String
    ): Result<String> {
        val activeFilters = filterIds.mapNotNull { id -> filters.find { it.id == id } }
            .filter { it.ffmpegFilter.isNotBlank() }
        if (activeFilters.isEmpty()) return Result.success(outputPath)
        val filterChain = activeFilters.joinToString(",") { it.ffmpegFilter }
        return execute(
            "-i", videoPath,
            "-vf", filterChain,
            "-c:a", "copy",
            "-y", outputPath
        ).map { outputPath }
    }

    fun getAllFilters(): List<FilterDef> = filters

    private suspend fun execute(vararg args: String): Result<Unit> {
        val command = ffmpegExecutor.buildCommand(*args)
        return ffmpegExecutor.execute(command).map { }
    }

    companion object {
        val filters: List<FilterDef> = listOf(
            FilterDef("normal", "Normal", ""),
            FilterDef(
                "vintage", "Vintage",
                "colorchannelmixer=.393:.769:.189:.349:.686:.168:.272:.534:.131"
            ),
            FilterDef(
                "grayscale", "Grayscale",
                "colorchannelmixer=.3:.4:.3:0:.3:.4:.3:0:.3:.4:.3"
            ),
            FilterDef(
                "sepia", "Sepia",
                "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131"
            ),
            FilterDef(
                "vivid", "Vivid",
                "eq=saturation=2.0:contrast=1.2"
            ),
            FilterDef(
                "cool", "Cool",
                "colorbalance=rs=-0.2:gs=-0.1:bs=0.3"
            ),
            FilterDef(
                "warm", "Warm",
                "colorbalance=rs=0.3:gs=0.1:bs=-0.2"
            ),
            FilterDef(
                "dramatic", "Dramatic",
                "eq=contrast=1.5:brightness=-0.1"
            ),
            FilterDef(
                "noir", "Noir",
                "hue=s=0:eq=contrast=1.3:brightness=-0.05"
            ),
            FilterDef(
                "fade", "Fade",
                "curves=master='0/0 0.5/0.4 1/1'"
            )
        )
    }
}
