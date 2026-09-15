package com.example.iptvplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.app.Application
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.IPTVApplication
import com.example.R
import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.navigation.Screen
import com.example.iptvplayer.ui.channels.ChannelsScreen
import com.example.iptvplayer.ui.checker.CheckerScreen
import com.example.iptvplayer.ui.components.ProjectorHeader
import com.example.iptvplayer.ui.components.TVNavRailItem
import com.example.iptvplayer.ui.favorites.FavoritesScreen
import com.example.iptvplayer.ui.home.HomeScreen
import com.example.iptvplayer.ui.player.PlayerPreviewScreen
import com.example.iptvplayer.ui.player.PlayerScreen
import com.example.iptvplayer.ui.player.PlayerViewModel
import com.example.iptvplayer.ui.playlists.PlaylistViewModel
import com.example.iptvplayer.ui.playlists.PlaylistsScreen
import com.example.iptvplayer.ui.settings.SettingsScreen
import com.example.iptvplayer.ui.splash.SplashScreen
import com.example.iptvplayer.ui.users.UserViewModel
import com.example.iptvplayer.ui.users.UsersScreen
import com.example.iptvplayer.ui.users.login.SelectProfileScreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryElectricCyan
import com.example.ui.theme.PrimarySapphire
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun IPTVPlayerApp() {
    val context = LocalContext.current
    val appContainer = remember {
        (context.applicationContext as IPTVApplication).container
    }

    val userViewModel: UserViewModel = viewModel(
        factory = UserViewModel.provideFactory(appContainer.userRepository)
    )

    val playlistViewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModel.provideFactory(
            appContainer.playlistRepository,
            appContainer.userRepository
        )
    )

    val homeViewModel: com.example.iptvplayer.ui.home.HomeViewModel = viewModel(
        factory = com.example.iptvplayer.ui.home.HomeViewModel.provideFactory(
            appContainer.playlistRepository,
            appContainer.channelRepository,
            appContainer.userRepository,
            appContainer.streamCheckerRepository
        )
    )

    val activeUser by userViewModel.activeUser.collectAsState()
    val allUsers by userViewModel.users.collectAsState()

    val playerViewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.provideFactory(
            application = context.applicationContext as Application,
            channelRepository = appContainer.channelRepository,
            userRepository = appContainer.userRepository,
            playlistRepository = appContainer.playlistRepository
        )
    )

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var triggerAddPlaylist by remember { mutableStateOf(false) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var playingChannel by remember { mutableStateOf<Channel?>(null) }
    var playingPlaylistId by remember { mutableStateOf<String?>(null) }

    // Back button handling for remote control / D-pad
    BackHandler(
        enabled = currentScreen != Screen.Splash &&
                currentScreen != Screen.Home &&
                currentScreen != Screen.Player &&
                !(currentScreen == Screen.SelectProfile && activeUser == null)
    ) {
        if (currentScreen == Screen.SelectProfile && activeUser != null) {
            currentScreen = Screen.Home
        } else if (currentScreen != Screen.Home) {
            currentScreen = Screen.Home
        }
    }

    when (val screen = currentScreen) {
        is Screen.Splash -> {
            SplashScreen(
                onSplashFinished = {
                    // Stage 2 Flow: If an active authenticated session exists, go straight to Home.
                    // Otherwise, display profile selection screen ("Quem está a assistir?").
                    if (activeUser != null) {
                        currentScreen = Screen.Home
                    } else {
                        currentScreen = Screen.SelectProfile
                    }
                }
            )
        }
        is Screen.SelectProfile -> {
            SelectProfileScreen(
                viewModel = userViewModel,
                canGoBack = activeUser != null,
                onNavigateBack = { currentScreen = Screen.Home },
                onLoginSuccess = {
                    currentScreen = Screen.Home
                }
            )
        }
        is Screen.Player -> {
            PlayerScreen(
                channel = playingChannel,
                playlistId = playingPlaylistId,
                viewModel = playerViewModel,
                onBack = { currentScreen = Screen.Home }
            )
        }
        else -> {
            // Main 16:9 Projector Layout: Sidebar Navigation + Content Pane
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("app_main_16_9_container")
                    .background(BackgroundDark)
            ) {
                // Left Navigation Sidebar (Projector & TV Navigation Menu)
                TVSidebarNavigation(
                    currentScreen = screen,
                    onScreenSelected = { currentScreen = it },
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                )

                // Main Content Workspace
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    // Top Projector Header with Clock, User Profile & Status
                    ProjectorHeader(
                        currentTitle = stringResource(screen.titleRes),
                        activeUser = activeUser,
                        onProfileClick = { currentScreen = Screen.Users }
                    )

                    // Active Screen Content with smooth transition
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { targetScreen ->
                        when (targetScreen) {
                            Screen.Home -> HomeScreen(
                                viewModel = homeViewModel,
                                onNavigate = { currentScreen = it },
                                onNavigateToAddPlaylist = {
                                    triggerAddPlaylist = true
                                    currentScreen = Screen.Playlists
                                },
                                onOpenPlaylistChannels = { playlistId ->
                                    selectedPlaylistId = playlistId
                                    currentScreen = Screen.Channels
                                },
                                 onPlayChannel = { channel ->
                                    playingChannel = channel
                                    playingPlaylistId = channel.playlistId.ifBlank { selectedPlaylistId }
                                    currentScreen = Screen.Player
                                }
                            )
                            Screen.Playlists -> {
                                PlaylistsScreen(
                                    viewModel = playlistViewModel,
                                    initialOpenAddDialog = triggerAddPlaylist,
                                    onOpenChannels = { playlist ->
                                        selectedPlaylistId = playlist.id
                                        currentScreen = Screen.Channels
                                    }
                                )
                                // Reset add dialog trigger once rendered
                                if (triggerAddPlaylist) {
                                    triggerAddPlaylist = false
                                }
                            }
                            Screen.Channels -> ChannelsScreen(
                                channelRepository = appContainer.channelRepository,
                                playlistRepository = appContainer.playlistRepository,
                                userRepository = appContainer.userRepository,
                                selectedPlaylistId = selectedPlaylistId,
                                onChannelClick = { channel ->
                                    playingChannel = channel
                                    playingPlaylistId = channel.playlistId.ifBlank { selectedPlaylistId }
                                    currentScreen = Screen.Player
                                },
                                onNavigateToAddPlaylist = {
                                    triggerAddPlaylist = true
                                    currentScreen = Screen.Playlists
                                },
                                onClearPlaylistFilter = {
                                    selectedPlaylistId = null
                                }
                            )
                            Screen.Favorites -> FavoritesScreen(
                                channelRepository = appContainer.channelRepository,
                                userRepository = appContainer.userRepository,
                                onChannelClick = { channel ->
                                    playingChannel = channel
                                    playingPlaylistId = channel.playlistId.ifBlank { selectedPlaylistId }
                                    currentScreen = Screen.Player
                                },
                                onNavigateToChannels = {
                                    selectedPlaylistId = null
                                    currentScreen = Screen.Channels
                                }
                            )
                            Screen.Users -> UsersScreen(
                                viewModel = userViewModel,
                                onNavigateToProfileSelect = { currentScreen = Screen.SelectProfile }
                            )
                            Screen.Checker -> CheckerScreen(
                                checkerRepository = appContainer.streamCheckerRepository,
                                playlistRepository = appContainer.playlistRepository,
                                userRepository = appContainer.userRepository
                            )
                            Screen.Settings -> SettingsScreen()
                            else -> HomeScreen(
                                viewModel = homeViewModel,
                                onNavigate = { currentScreen = it },
                                onNavigateToAddPlaylist = {
                                    triggerAddPlaylist = true
                                    currentScreen = Screen.Playlists
                                },
                                onOpenPlaylistChannels = { playlistId ->
                                    selectedPlaylistId = playlistId
                                    currentScreen = Screen.Channels
                                },
                                onPlayChannel = { channel ->
                                    playingChannel = channel
                                    playingPlaylistId = channel.playlistId.ifBlank { selectedPlaylistId }
                                    currentScreen = Screen.Player
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 16:9 TV Navigation Sidebar.
 * Displays icons + text, clearly selected/focused states,
 * fully navigable via mouse hover/click and D-pad directional controls.
 */
@Composable
private fun TVSidebarNavigation(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("tv_sidebar_navigation"),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Brand Logo & Title in Sidebar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Surface(
                    color = PrimarySapphire.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PrimaryElectricCyan),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tv_display),
                            contentDescription = null,
                            tint = PrimaryElectricCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Navigation Menu Items
            Screen.navItems.forEach { screenItem ->
                if (screenItem.icon != null) {
                    TVNavRailItem(
                        icon = screenItem.icon,
                        label = stringResource(screenItem.titleRes),
                        isSelected = currentScreen == screenItem,
                        onClick = { onScreenSelected(screenItem) },
                        modifier = Modifier.padding(vertical = 4.dp),
                        testTag = "nav_item_${screenItem.route}"
                    )
                }
            }
        }
    }
}
