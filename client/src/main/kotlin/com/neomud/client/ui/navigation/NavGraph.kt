package com.neomud.client.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neomud.client.ui.screens.GameScreen
import com.neomud.client.ui.screens.LoginScreen
import com.neomud.client.ui.screens.RegistrationScreen
import com.neomud.client.viewmodel.AuthState
import com.neomud.client.viewmodel.AuthViewModel
import com.neomud.client.viewmodel.GameViewModel

@Composable
fun NeoMudNavGraph(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    val connectionState by authViewModel.connectionState.collectAsState()
    val connectionError by authViewModel.connectionError.collectAsState()
    val availableClasses by authViewModel.availableClasses.collectAsState()

    // Navigate on auth state changes
    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedIn) {
            navController.navigate("game") {
                popUpTo("login") { inclusive = true }
            }
        }
        if (authState is AuthState.Registered) {
            navController.navigate("login") {
                popUpTo("register") { inclusive = true }
            }
        }
        // Handle logout: Idle while on game screen → back to login
        if (authState is AuthState.Idle) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute == "game") {
                navController.navigate("login") {
                    popUpTo("game") { inclusive = true }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                connectionState = connectionState,
                authState = authState,
                connectionError = connectionError,
                onConnect = { host, port -> authViewModel.connect(host, port) },
                onLogin = { username, password -> authViewModel.login(username, password) },
                onNavigateToRegister = { navController.navigate("register") },
                onClearError = { authViewModel.clearError() }
            )
        }

        composable("register") {
            RegistrationScreen(
                authState = authState,
                availableClasses = availableClasses,
                onRegister = { username, password, characterName, characterClass ->
                    authViewModel.register(username, password, characterName, characterClass)
                },
                onBack = { navController.popBackStack() },
                onClearError = { authViewModel.clearError() }
            )
        }

        composable("game") {
            val initialPlayer = (authState as? AuthState.LoggedIn)?.player
            val serverBaseUrl = authViewModel.serverBaseUrl
            val gameViewModel = remember {
                GameViewModel(authViewModel.wsClient, serverBaseUrl).also {
                    if (initialPlayer != null) it.setInitialPlayer(initialPlayer)
                    it.startCollecting()
                }
            }
            GameScreen(
                gameViewModel = gameViewModel,
                onLogout = { authViewModel.logout() }
            )
        }
    }
}
