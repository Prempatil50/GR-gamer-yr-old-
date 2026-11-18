package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.ai.GeminiReplyEngine
import com.example.data.firebase.FirestoreRepository
import com.example.data.model.AiSettings
import com.example.data.model.ChannelConfig
import com.example.data.model.UiState
import com.example.data.youtube.YouTubeRepository
import com.example.ui.screens.AiPromptSettingsScreen
import com.example.ui.screens.AiReplySettingsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.AutoReplyScreen
import com.example.ui.screens.ChannelConnectScreen
import com.example.ui.screens.CommentManagementScreen
import com.example.ui.screens.DashboardDestination
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LanguageSettingsScreen
import com.example.ui.screens.ReplyHistoryScreen
import com.example.ui.screens.signOutUser
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.DeepSpaceSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object GrRoutes {
    const val DASHBOARD = "dashboard"
    const val CHANNEL_CONNECT = "channel_connect"
    const val AUTO_REPLY = "auto_reply"
    const val AI_SETTINGS = "ai_settings"
    const val REPLY_HISTORY = "reply_history"
    const val COMMENT_MANAGEMENT = "comment_management"
    const val LANGUAGE_SETTINGS = "language_settings"
    const val AI_PROMPT_SETTINGS = "ai_prompt_settings"
}

private data class TopLevelTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val TOP_LEVEL_TABS = listOf(
    TopLevelTab(
        route = GrRoutes.DASHBOARD,
        label = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    TopLevelTab(
        route = GrRoutes.COMMENT_MANAGEMENT,
        label = "Comments",
        selectedIcon = Icons.AutoMirrored.Filled.Comment,
        unselectedIcon = Icons.AutoMirrored.Outlined.Comment
    ),
    TopLevelTab(
        route = GrRoutes.AUTO_REPLY,
        label = "Auto Reply",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    ),
    TopLevelTab(
        route = GrRoutes.REPLY_HISTORY,
        label = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            GrGamerAuthenticatedShell(
                currentUserId = user.uid,
                userEmail = user.email
            )
        }
    }
}

@Composable
private fun GrGamerAuthenticatedShell(
    currentUserId: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    firestoreRepo = FirestoreRepository(db),
                    youtubeRepo = YouTubeRepository(),
                    geminiEngine = GeminiReplyEngine(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val channelState by viewModel.channelState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val commentsState by viewModel.commentsState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    val runtime by viewModel.runtime.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(runtime.statusBanner, runtime.errorBanner) {
        val message = runtime.errorBanner ?: runtime.statusBanner
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearBanners()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: GrRoutes.DASHBOARD
    val isTopLevelRoute = TOP_LEVEL_TABS.any { it.route == currentRoute }

    val channel = (channelState as? UiState.Success)?.data ?: ChannelConfig(userId = currentUserId)
    val settings = (settingsState as? UiState.Success)?.data ?: AiSettings(userId = currentUserId)
    val comments = (commentsState as? UiState.Success)?.data.orEmpty()
    val history = (historyState as? UiState.Success)?.data.orEmpty()

    val streamError = listOf(channelState, settingsState, commentsState, historyState)
        .filterIsInstance<UiState.Error>()
        .firstOrNull()
        ?.message

    LaunchedEffect(streamError) {
        if (!streamError.isNullOrBlank()) {
            snackbarHostState.showSnackbar(streamError)
        }
    }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBlack,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    val isError = runtime.errorBanner != null || streamError != null
                    Snackbar(
                        snackbarData = data,
                        containerColor = if (isError) CrimsonAlert else CarbonCard,
                        contentColor = TextPrimary
                    )
                }
            },
            bottomBar = {
                if (!useNavigationRail && isTopLevelRoute) {
                    NavigationBar(
                        containerColor = DeepSpaceSurface,
                        contentColor = NeonCyan,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        TOP_LEVEL_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = DeepCyanContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail && isTopLevelRoute) {
                    NavigationRail(
                        containerColor = DeepSpaceSurface,
                        contentColor = NeonCyan,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        TOP_LEVEL_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = DeepCyanContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (channelState is UiState.Loading && settingsState is UiState.Loading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NeonCyan)
                        }
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = GrRoutes.DASHBOARD,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 840.dp)
                                .then(
                                    if (isTopLevelRoute) Modifier.windowInsetsPadding(WindowInsets.statusBars)
                                    else Modifier
                                )
                        ) {
                            composable(GrRoutes.DASHBOARD) {
                                HomeDashboardScreen(
                                    channel = channel,
                                    settings = settings,
                                    runtime = runtime,
                                    onNavigate = { dest ->
                                        val target = when (dest) {
                                            DashboardDestination.CHANNEL_CONNECT -> GrRoutes.CHANNEL_CONNECT
                                            DashboardDestination.AUTO_REPLY -> GrRoutes.AUTO_REPLY
                                            DashboardDestination.AI_SETTINGS -> GrRoutes.AI_SETTINGS
                                            DashboardDestination.REPLY_HISTORY -> GrRoutes.REPLY_HISTORY
                                            DashboardDestination.COMMENT_MANAGEMENT -> GrRoutes.COMMENT_MANAGEMENT
                                            DashboardDestination.LANGUAGE_SETTINGS -> GrRoutes.LANGUAGE_SETTINGS
                                            DashboardDestination.AI_PROMPT_SETTINGS -> GrRoutes.AI_PROMPT_SETTINGS
                                        }
                                        navController.navigate(target) { launchSingleTop = true }
                                    },
                                    onStartAutoReply = { viewModel.startAutoReply() },
                                    onStopAutoReply = { viewModel.stopAutoReply() },
                                    onToggleAutoReply = { viewModel.toggleAutoReply(it) },
                                    onDisconnectChannel = { viewModel.disconnectYouTubeChannel() },
                                    onSignOut = {
                                        viewModel.stopAutoReply()
                                        signOutUser(credentialManager, scope)
                                    }
                                )
                            }

                            composable(GrRoutes.CHANNEL_CONNECT) {
                                ChannelConnectScreen(
                                    channel = channel,
                                    userEmail = userEmail,
                                    runtime = runtime,
                                    onBack = { navController.popBackStack() },
                                    onStartConnecting = { viewModel.setConnectingChannel(true) },
                                    onTokenAcquired = { token -> viewModel.onOAuthTokenAcquired(token) },
                                    onAuthError = { msg, needsCloud -> viewModel.reportChannelError(msg, needsCloud) },
                                    onSelectChannel = { opt -> viewModel.connectSelectedChannel(opt) },
                                    onSwitchChannel = { viewModel.openChannelPickerWithCurrentToken() },
                                    onDismissChannelPicker = { viewModel.dismissChannelPicker() },
                                    onDisconnect = { viewModel.disconnectYouTubeChannel() }
                                )
                            }

                            composable(GrRoutes.AUTO_REPLY) {
                                val cameFromDashboard = navController.previousBackStackEntry != null
                                AutoReplyScreen(
                                    channel = channel,
                                    settings = settings,
                                    runtime = runtime,
                                    showBackArrow = cameFromDashboard,
                                    onBack = { navController.popBackStack() },
                                    onStartAutoReply = { viewModel.startAutoReply() },
                                    onStopAutoReply = { viewModel.stopAutoReply() },
                                    onToggleAutoReply = { viewModel.toggleAutoReply(it) },
                                    onSyncAndReplyNow = { viewModel.syncCommentsFromYouTube(autoReplyAfterSync = true) },
                                    onTestAiReply = { sample -> viewModel.testAiReplyGeneration(sample) }
                                )
                            }

                            composable(GrRoutes.COMMENT_MANAGEMENT) {
                                val cameFromDashboard = navController.previousBackStackEntry != null
                                CommentManagementScreen(
                                    channel = channel,
                                    comments = comments,
                                    runtime = runtime,
                                    showBackArrow = cameFromDashboard,
                                    onBack = { navController.popBackStack() },
                                    onFetchComments = { viewModel.syncCommentsFromYouTube(autoReplyAfterSync = false) },
                                    onReplyToComment = { comment -> viewModel.replyToSingleComment(comment) },
                                    onIgnoreComment = { comment -> viewModel.ignoreComment(comment) }
                                )
                            }

                            composable(GrRoutes.REPLY_HISTORY) {
                                val cameFromDashboard = navController.previousBackStackEntry != null
                                ReplyHistoryScreen(
                                    history = history,
                                    showBackArrow = cameFromDashboard,
                                    onBack = { navController.popBackStack() },
                                    onDeleteEntry = { docId -> viewModel.deleteHistoryItem(docId) }
                                )
                            }

                            composable(GrRoutes.LANGUAGE_SETTINGS) {
                                LanguageSettingsScreen(
                                    settings = settings,
                                    onBack = { navController.popBackStack() },
                                    onSaveLanguageSettings = { auto, mode, langs ->
                                        viewModel.saveLanguageSettings(auto, mode, langs)
                                    }
                                )
                            }

                            composable(GrRoutes.AI_SETTINGS) {
                                AiReplySettingsScreen(
                                    settings = settings,
                                    onBack = { navController.popBackStack() },
                                    onSaveReplySettings = { spam, abuse, dedupe, tone, maxLen, interval ->
                                        viewModel.saveReplySettings(spam, abuse, dedupe, tone, maxLen, interval)
                                    }
                                )
                            }

                            composable(GrRoutes.AI_PROMPT_SETTINGS) {
                                AiPromptSettingsScreen(
                                    settings = settings,
                                    onBack = { navController.popBackStack() },
                                    onSavePrompt = { prompt -> viewModel.saveCustomPrompt(prompt) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
