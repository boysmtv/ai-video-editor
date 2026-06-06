package com.changecut.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.changecut.feature.home.HomeScreen

const val homeRoute = "home"

fun NavGraphBuilder.homeNavGraph(navController: NavHostController) {
    composable(homeRoute) {
        HomeScreen(
            onNewProject = {
                navController.navigate("new_project")
            },
            onOpenProject = { projectId ->
                navController.navigate("editor/$projectId")
            },
            onSettings = {
                navController.navigate("settings")
            }
        )
    }
}
