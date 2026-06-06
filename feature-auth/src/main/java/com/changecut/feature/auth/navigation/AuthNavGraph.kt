package com.changecut.feature.auth.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.changecut.feature.auth.splash.SplashScreen
import com.changecut.feature.auth.login.LoginScreen
import com.changecut.feature.auth.register.RegisterScreen

enum class AuthNavGraph(val route: String) {
    Splash("splash"),
    Login("login"),
    Register("register"),
    ForgotPassword("forgot_password")
}

fun NavGraphBuilder.authNavGraph(navController: NavHostController) {
    composable(AuthNavGraph.Splash.route) {
        SplashScreen(onNavigateToLogin = {
            navController.navigate(AuthNavGraph.Login.route) {
                popUpTo(AuthNavGraph.Splash.route) { inclusive = true }
            }
        })
    }

    composable(AuthNavGraph.Login.route) {
        LoginScreen(
            onNavigateToRegister = {
                navController.navigate(AuthNavGraph.Register.route)
            },
            onNavigateToForgotPassword = {
                navController.navigate(AuthNavGraph.ForgotPassword.route)
            },
            onLoginSuccess = {
                navController.navigate("home") {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }

    composable(AuthNavGraph.Register.route) {
        RegisterScreen(
            onNavigateBack = { navController.popBackStack() },
            onRegisterSuccess = {
                navController.navigate("home") {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }

    composable(AuthNavGraph.ForgotPassword.route) {
        // TODO: Forgot Password Screen
    }
}
