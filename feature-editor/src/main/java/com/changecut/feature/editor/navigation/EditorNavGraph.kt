package com.changecut.feature.editor.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.changecut.feature.editor.EditorScreen

const val editorRoute = "editor/{projectId}"

fun NavGraphBuilder.editorNavGraph(navController: NavHostController) {
    composable(
        route = editorRoute,
        arguments = listOf(navArgument("projectId") { type = NavType.StringType })
    ) { backStackEntry ->
        val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
        EditorScreen(
            projectId = projectId,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
