package com.changecut.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.changecut.feature.auth.navigation.authNavGraph
import com.changecut.feature.auth.navigation.AuthNavGraph
import com.changecut.feature.editor.navigation.editorRoute
import com.changecut.feature.editor.navigation.editorNavGraph
import com.changecut.feature.export.navigation.exportRoute
import com.changecut.feature.export.navigation.exportNavGraph
import com.changecut.feature.home.navigation.homeNavGraph
import com.changecut.feature.home.navigation.homeRoute
import com.changecut.feature.home.newproject.NewProjectScreen

const val newProjectRoute = "new_project"

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = AuthNavGraph.Splash.route
    ) {
        authNavGraph(navController)
        homeNavGraph(navController)

        composable(newProjectRoute) {
            NewProjectScreen(
                onNavigateBack = { navController.popBackStack() },
                onProjectCreated = { projectId ->
                    navController.navigate("editor/$projectId") {
                        popUpTo(homeRoute)
                    }
                }
            )
        }

        editorNavGraph(navController)
        exportNavGraph(navController)
    }
}
