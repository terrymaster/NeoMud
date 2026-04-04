package com.neomud.client.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neomud.client.model.platform.WorldDetail
import com.neomud.client.network.PlatformApiClient
import com.neomud.client.platform.PlatformAudioManager
import com.neomud.client.platform.serverConfig
import com.neomud.client.ui.screens.GameScreen
import com.neomud.client.ui.screens.LoginScreen
import com.neomud.client.ui.screens.RegistrationScreen
import com.neomud.client.ui.screens.WorldBrowserScreen
import com.neomud.client.ui.screens.WorldDetailScreen
import com.neomud.client.viewmodel.AuthState
import com.neomud.client.viewmodel.AuthViewModel
import com.neomud.client.viewmodel.GameViewModel
import com.neomud.client.viewmodel.WorldBrowserViewModel
import neomud.client.generated.resources.Res

/**
 * Shared navigation graph for all platforms.
 * Platform entry points provide the AudioManager and CompositionLocals
 * (LocalIsLandscape, LocalSetLayoutPreference) before calling this.
 */
@Composable
fun NeoMudApp(
    authViewModel: AuthViewModel,
    audioManager: PlatformAudioManager
) {
    val navController = rememberNavController()

    DisposableEffect(Unit) {
        onDispose { audioManager.release() }
    }

    val authState by authViewModel.authState.collectAsState()
    val connectionState by authViewModel.connectionState.collectAsState()
    val connectionError by authViewModel.connectionError.collectAsState()
    val availableClasses by authViewModel.availableClasses.collectAsState()
    val availableRaces by authViewModel.availableRaces.collectAsState()

    // Selected world from marketplace — drives connection target
    var selectedWorld by remember { mutableStateOf<WorldDetail?>(null) }

    // World browser ViewModel (shared across browser + detail screens)
    val worldBrowserViewModel = remember {
        WorldBrowserViewModel(PlatformApiClient(serverConfig.platformApiUrl))
    }
    val worlds by worldBrowserViewModel.worlds.collectAsState()
    val worldBrowserLoading by worldBrowserViewModel.isLoading.collectAsState()
    val worldBrowserError by worldBrowserViewModel.error.collectAsState()
    val worldDetail by worldBrowserViewModel.selectedWorld.collectAsState()
    val worldDetailLoading by worldBrowserViewModel.isLoadingDetail.collectAsState()

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
        // Handle logout: Idle while on game screen → back to world browser
        if (authState is AuthState.Idle) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute == "game") {
                navController.navigate("worldBrowser") {
                    popUpTo("game") { inclusive = true }
                }
            }
        }
    }

    // Load worlds on first composition
    LaunchedEffect(Unit) {
        worldBrowserViewModel.loadWorlds()
    }

    // Dark background fills edge-to-edge (including safe area zones on iOS)
    // so notch/home indicator areas show dark instead of white
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF080604))) {
    NavHost(navController = navController, startDestination = "worldBrowser") {

        composable("worldBrowser") {
            // Play intro BGM from embedded resource
            LaunchedEffect(Unit) {
                val introUri = Res.getUri("files/intro_theme.mp3")
                audioManager.playBgmFromUri(introUri, "intro_theme")
            }
            WorldBrowserScreen(
                worlds = worlds,
                isLoading = worldBrowserLoading,
                error = worldBrowserError,
                showDirectConnect = serverConfig.showServerConfig,
                onWorldClick = { slug ->
                    worldBrowserViewModel.loadWorldDetail(slug)
                    navController.navigate("worldDetail")
                },
                onSearch = { query -> worldBrowserViewModel.loadWorlds(query) },
                onRetry = { worldBrowserViewModel.loadWorlds() },
                onDirectConnect = {
                    // Skip marketplace, go straight to manual login (dev mode)
                    navController.navigate("login")
                }
            )
        }

        composable("worldDetail") {
            WorldDetailScreen(
                world = worldDetail,
                isLoading = worldDetailLoading,
                error = worldBrowserError,
                onPlay = { world ->
                    selectedWorld = world
                    worldBrowserViewModel.clearSelection()
                    navController.navigate("login")
                },
                onBack = {
                    worldBrowserViewModel.clearSelection()
                    navController.popBackStack()
                },
                onRetry = {
                    worldBrowserViewModel.clearError()
                    // Re-fetch if we have a slug
                    worldDetail?.slug?.let { worldBrowserViewModel.loadWorldDetail(it) }
                }
            )
        }

        composable("login") {
            LoginScreen(
                connectionState = connectionState,
                authState = authState,
                connectionError = connectionError,
                onConnect = { host, port ->
                    // If a world was selected from marketplace, use its server endpoint
                    val world = selectedWorld
                    if (world?.serverEndpoint != null) {
                        // Parse serverEndpoint (e.g., "wss://play.neomud.com:443")
                        // For now, use the host/port from the login screen
                        authViewModel.connect(host, port, serverConfig.useTls)
                    } else {
                        authViewModel.connect(host, port, serverConfig.useTls)
                    }
                },
                onLogin = { username, password -> authViewModel.login(username, password) },
                onNavigateToRegister = {
                    authViewModel.clearError()
                    navController.navigate("register")
                },
                onClearError = { authViewModel.clearError() }
            )
        }

        composable("register") {
            RegistrationScreen(
                authState = authState,
                availableClasses = availableClasses,
                availableRaces = availableRaces,
                serverBaseUrl = authViewModel.serverBaseUrl,
                onRegister = { username, password, characterName, characterClass, race, gender, allocatedStats ->
                    authViewModel.register(username, password, characterName, characterClass, race, gender, allocatedStats)
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
            val initialSkills = authViewModel.availableSkills.value
            val initialRoomInfo = authViewModel.initialRoomInfo.value
            val initialMapData = authViewModel.initialMapData.value
            val initialTutorial = authViewModel.initialTutorial.value
            val gameViewModel = remember {
                GameViewModel(authViewModel.wsClient, serverBaseUrl, audioManager).also {
                    if (initialPlayer != null) it.setInitialPlayer(initialPlayer)
                    it.setInitialCatalogs(classes = initialClasses, items = initialItems, spells = initialSpells, skills = initialSkills)
                    if (initialRoomInfo != null) it.setInitialRoomInfo(initialRoomInfo)
                    if (initialMapData != null) it.setInitialMapData(initialMapData)
                    if (initialTutorial != null) it.setInitialTutorial(initialTutorial)
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
    } // end edge-to-edge dark background
}
