package com.changecut.feature.export.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.changecut.feature.export.export.ExportScreen

const val exportRoute = "export/{projectId}"

fun NavGraphBuilder.exportNavGraph(navController: NavHostController) {
    composable(
        route = exportRoute,
        arguments = listOf(navArgument("projectId") { type = NavType.StringType })
    ) { backStackEntry ->
        val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
        ExportScreen(
            projectId = projectId,
            outputPath = null,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
