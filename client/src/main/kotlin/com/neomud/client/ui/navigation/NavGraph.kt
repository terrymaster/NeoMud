package com.neomud.client.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neomud.client.audio.AudioManager
import com.neomud.client.ui.screens.GameScreen
import com.neomud.client.ui.screens.LoginScreen
import com.neomud.client.ui.screens.RegistrationScreen
import com.neomud.client.viewmodel.AuthState
import com.neomud.client.viewmodel.AuthViewModel
import com.neomud.client.viewmodel.GameViewModel

@Composable
fun NeoMudNavGraph(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val audioManager = remember { AudioManager(context) }

    DisposableEffect(Unit) {
        onDispose { audioManager.release() }
    }
    val authState by authViewModel.authState.collectAsState()
    val connectionState by authViewModel.connectionState.collectAsState()
    val connectionError by authViewModel.connectionError.collectAsState()
    val availableClasses by authViewModel.availableClasses.collectAsState()
    val availableRaces by authViewModel.availableRaces.collectAsState()

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
            // Play intro BGM while on login screen
            val loginServerBaseUrl = authViewModel.serverBaseUrl
            LaunchedEffect(Unit) {
                if (loginServerBaseUrl.isNotBlank()) {
                    audioManager.playBgm(loginServerBaseUrl, "intro_theme")
                }
            }
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
                availableRaces = availableRaces,
                onRegister = { username, password, characterName, characterClass, race, allocatedStats ->
                    authViewModel.register(username, password, characterName, characterClass, race, allocatedStats)
                },
                onBack = { navController.popBackStack() },
                onClearError = { authViewModel.clearError() }
            )
        }

        composable("game") {
            val initialPlayer = (authState as? AuthState.LoggedIn)?.player
            val serverBaseUrl = authViewModel.serverBaseUrl
            val initialClasses = authViewModel.availableClasses.value
            val initialItems = authViewModel.availableItems.value
            val initialSpells = authViewModel.availableSpells.value
            val gameViewModel = remember {
                GameViewModel(authViewModel.wsClient, serverBaseUrl, audioManager).also {
                    if (initialPlayer != null) it.setInitialPlayer(initialPlayer)
                    it.setInitialCatalogs(classes = initialClasses, items = initialItems, spells = initialSpells)
                    it.startCollecting()
                }
            }
            GameScreen(
                gameViewModel = gameViewModel,
                onLogout = { authViewModel.logout() },
                audioManager = audioManager
            )
        }
    }
}
