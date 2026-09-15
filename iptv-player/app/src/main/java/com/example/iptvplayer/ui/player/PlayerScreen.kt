package com.example.iptvplayer.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.iptvplayer.domain.model.Channel

/**
 * Finds the host Activity from the Compose context.
 */
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Fullscreen TV-style IPTV player powered by AndroidX Media3 / ExoPlayer.
 * Plays the real stream URL with TV controls, D-pad & mouse support,
 * live edge monitoring, and in-player channel switching.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: Channel?,
    playlistId: String?,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentChannel by viewModel.currentChannel.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val isControlsVisible by viewModel.isControlsVisible.collectAsState()
    val isChannelOverlayOpen by viewModel.isChannelOverlayOpen.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isAspectRatioFit by viewModel.isAspectRatioFit.collectAsState()
    val isLiveStream by viewModel.isLiveStream.collectAsState()
    val liveOffsetMs by viewModel.liveOffsetMs.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()

    val overlayChannels by viewModel.overlayChannels.collectAsState()
    val overlayCategories by viewModel.overlayCategories.collectAsState()
    val selectedCategory by viewModel.selectedOverlayCategory.collectAsState()
    val searchQuery by viewModel.overlaySearchQuery.collectAsState()

    val focusRequester = remember { FocusRequester() }

    // Android Fullscreen / Immersive Mode handling (Requirement 23)
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        if (activity != null) {
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            val activity = context.findActivity()
            if (activity != null) {
                val window = activity.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Automatically start playback when a channel is passed (Requirement 1)
    LaunchedEffect(channel?.id) {
        if (channel != null && currentChannel?.id != channel.id) {
            viewModel.playChannel(channel, playlistId)
        }
    }

    // Request initial focus on the player container for D-pad navigation
    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    // Remote D-pad Back Button handling (Requirement 20)
    BackHandler {
        when {
            isChannelOverlayOpen -> viewModel.closeChannelOverlay()
            isControlsVisible -> viewModel.hideControls()
            else -> onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            // D-Pad and remote key events
            .onKeyEvent { keyEvent ->
                viewModel.userInteracted()
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER -> {
                            if (!isControlsVisible && !isChannelOverlayOpen) {
                                viewModel.showControls()
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            viewModel.togglePlayPause()
                            return@onKeyEvent true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            if (playbackState is PlayerPlaybackState.Paused) {
                                viewModel.togglePlayPause()
                                return@onKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            if (playbackState is PlayerPlaybackState.Playing) {
                                viewModel.togglePlayPause()
                                return@onKeyEvent true
                            }
                        }
                    }
                }
                false
            }
            // Mouse & Touch interaction detection (Requirement 21)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (isChannelOverlayOpen) {
                            viewModel.closeChannelOverlay()
                        } else {
                            viewModel.toggleControls()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Move) {
                            viewModel.userInteracted()
                        }
                    }
                }
            }
            .testTag("player_screen_root")
    ) {
        // Real Media3 PlayerView Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.exoPlayer
                    useController = false // Custom Compose TV Overlay
                    resizeMode = if (isAspectRatioFit) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                    keepScreenOn = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = viewModel.exoPlayer
                playerView.resizeMode = if (isAspectRatioFit) {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("player_surface_view")
        )

        // TV Projector Player Controls Overlay
        PlayerControlsOverlay(
            isVisible = isControlsVisible,
            channel = currentChannel ?: channel,
            playbackState = playbackState,
            isLiveStream = isLiveStream,
            liveOffsetMs = liveOffsetMs,
            isFavorite = isFavorite,
            isMuted = isMuted,
            isAspectRatioFit = isAspectRatioFit,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onToggleFavorite = { viewModel.toggleFavorite() },
            onToggleMute = { viewModel.toggleMute() },
            onToggleAspectRatio = { viewModel.toggleAspectRatio() },
            onRetry = { viewModel.retry() },
            onOpenChannelOverlay = { viewModel.openChannelOverlay() },
            onBackClick = onBack,
            onUserInteraction = { viewModel.userInteracted() }
        )

        // In-Player Channel List Overlay (Channel -> Player without returning to Home)
        PlayerChannelOverlay(
            isOpen = isChannelOverlayOpen,
            channels = overlayChannels,
            categories = overlayCategories,
            selectedCategory = selectedCategory,
            searchQuery = searchQuery,
            currentChannelId = (currentChannel ?: channel)?.id,
            onSearchChange = { viewModel.setOverlaySearchQuery(it) },
            onSelectCategory = { viewModel.selectOverlayCategory(it) },
            onSelectChannel = { selectedChannel ->
                viewModel.selectChannelFromOverlay(selectedChannel)
            },
            onClose = { viewModel.closeChannelOverlay() }
        )
    }
}
