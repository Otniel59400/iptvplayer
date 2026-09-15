package com.example.iptvplayer.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.domain.model.StreamStatus
import com.example.iptvplayer.ui.components.TVButton
import com.example.ui.theme.BorderFocused
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryElectricCyan
import com.example.ui.theme.StatusCheckingYellow
import com.example.ui.theme.StatusLiveRed
import com.example.ui.theme.SurfaceCardHover
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun PlayerControlsOverlay(
    isVisible: Boolean,
    channel: Channel?,
    playbackState: PlayerPlaybackState,
    isLiveStream: Boolean,
    liveOffsetMs: Long?,
    isFavorite: Boolean,
    isMuted: Boolean,
    isAspectRatioFit: Boolean,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleAspectRatio: () -> Unit,
    onRetry: () -> Unit,
    onOpenChannelOverlay: () -> Unit,
    onBackClick: () -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("player_controls_root")
    ) {
        // Center State Indicators (always visible when loading, buffering, reconnecting, or error)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (playbackState) {
                is PlayerPlaybackState.Loading -> {
                    PlayerStateLoadingBanner(
                        message = playbackState.message,
                        channelName = channel?.name
                    )
                }
                is PlayerPlaybackState.Buffering -> {
                    PlayerStateBufferingBanner(message = playbackState.message)
                }
                is PlayerPlaybackState.Reconnecting -> {
                    PlayerStateReconnectingBanner(
                        attempt = playbackState.attempt,
                        maxAttempts = playbackState.maxAttempts
                    )
                }
                is PlayerPlaybackState.Error -> {
                    PlayerStateErrorBanner(
                        message = playbackState.message,
                        onRetry = onRetry,
                        onBack = onBackClick
                    )
                }
                is PlayerPlaybackState.Paused -> {
                    if (isVisible) {
                        PlayerCenterPlayButton(
                            isPlaying = false,
                            onClick = onTogglePlayPause
                        )
                    }
                }
                is PlayerPlaybackState.Playing -> {
                    if (isVisible) {
                        PlayerCenterPlayButton(
                            isPlaying = true,
                            onClick = onTogglePlayPause
                        )
                    }
                }
                else -> Unit
            }
        }

        // Top & Bottom Controls (Animated visibility with subtle gradient scrims)
        AnimatedVisibility(
            visible = isVisible && playbackState !is PlayerPlaybackState.Error,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Gradient Scrim & Top Bar Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    PlayerTopBar(
                        channel = channel,
                        isLiveStream = isLiveStream,
                        liveOffsetMs = liveOffsetMs,
                        isFavorite = isFavorite,
                        onToggleFavorite = onToggleFavorite,
                        onBackClick = onBackClick,
                        onUserInteraction = onUserInteraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 18.dp)
                    )
                }

                // Bottom Gradient Scrim & Bottom Bar Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                ) {
                    PlayerBottomBar(
                        isMuted = isMuted,
                        isAspectRatioFit = isAspectRatioFit,
                        onOpenChannelOverlay = onOpenChannelOverlay,
                        onToggleMute = onToggleMute,
                        onToggleAspectRatio = onToggleAspectRatio,
                        onRetry = onRetry,
                        onUserInteraction = onUserInteraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 18.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

/**
 * Top bar displaying Back button, Channel metadata, Live Edge delay badge, and Favorite toggle.
 */
@Composable
private fun PlayerTopBar(
    channel: Channel?,
    isLiveStream: Boolean,
    liveOffsetMs: Long?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBackClick: () -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back Button + Channel Info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            PlayerIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.player_action_back),
                onClick = {
                    onUserInteraction()
                    onBackClick()
                },
                testTag = "player_btn_back"
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Channel Logo Thumbnail
            if (channel?.logoUrl != null && channel.logoUrl.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.size(44.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(channel.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Channel Name & Category
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel?.name ?: "Canal IPTV",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Channel Verification Status Badge
                    channel?.status?.let { status ->
                        Spacer(modifier = Modifier.width(8.dp))
                        ChannelStatusBadge(status = status)
                    }
                }

                Text(
                    text = channel?.category ?: "Geral",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Live Edge / Delay Badge + Favorite Toggle Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Delay Display (Calculated strictly from real playback/live-edge offset)
            if (isLiveStream) {
                LiveDelayBadge(liveOffsetMs = liveOffsetMs)
            }

            // Favorite Button: ♡ Favoritar / ❤️ Remover dos favoritos
            PlayerIconButton(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) {
                    stringResource(R.string.player_favorite_remove)
                } else {
                    stringResource(R.string.player_favorite_add)
                },
                tint = if (isFavorite) Color(0xFFFF5252) else TextPrimary,
                onClick = {
                    onUserInteraction()
                    onToggleFavorite()
                },
                testTag = "player_btn_favorite"
            )
        }
    }
}

/**
 * Bottom bar containing:
 * - BOTTOM-LEFT CORNER: Channel List overlay button (📋 Canais)
 * - Additional projector controls on the right (Mute, Aspect Ratio, Refresh)
 */
@Composable
private fun PlayerBottomBar(
    isMuted: Boolean,
    isAspectRatioFit: Boolean,
    onOpenChannelOverlay: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleAspectRatio: () -> Unit,
    onRetry: () -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // MANDATORY REQUIREMENT: Channel list button in BOTTOM-LEFT CORNER!
        // Simple icon: 📋 / FormatListBulleted + "Canais"
        TVButton(
            text = "📋 ${stringResource(R.string.player_channels)}",
            icon = Icons.Default.FormatListBulleted,
            onClick = {
                onUserInteraction()
                onOpenChannelOverlay()
            },
            isPrimary = true,
            modifier = Modifier.testTag("player_btn_channel_list")
        )

        // Secondary Projector Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mute / Unmute
            PlayerIconButton(
                icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) {
                    stringResource(R.string.player_unmute)
                } else {
                    stringResource(R.string.player_mute)
                },
                onClick = {
                    onUserInteraction()
                    onToggleMute()
                },
                testTag = "player_btn_mute"
            )

            // Aspect Ratio (16:9 Fit vs Zoom)
            PlayerIconButton(
                icon = Icons.Default.AspectRatio,
                contentDescription = stringResource(R.string.player_aspect_fit),
                tint = if (isAspectRatioFit) PrimaryElectricCyan else TextPrimary,
                onClick = {
                    onUserInteraction()
                    onToggleAspectRatio()
                },
                testTag = "player_btn_aspect_ratio"
            )

            // Refresh / Reconnect Stream
            PlayerIconButton(
                icon = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.player_action_retry),
                onClick = {
                    onUserInteraction()
                    onRetry()
                },
                testTag = "player_btn_refresh"
            )
        }
    }
}

/**
 * Big center play/pause button for projector remotes and mouse clicks.
 */
@Composable
private fun PlayerCenterPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        shape = CircleShape,
        color = when {
            isFocused || isHovered -> PrimaryElectricCyan
            else -> Color.Black.copy(alpha = 0.65f)
        },
        border = BorderStroke(
            2.dp,
            if (isFocused || isHovered) BorderFocused else BorderSubtle
        ),
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .testTag("player_center_play_pause_btn")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) {
                    stringResource(R.string.player_action_pause)
                } else {
                    stringResource(R.string.player_action_play)
                },
                tint = if (isFocused || isHovered) Color.Black else Color.White,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

/**
 * Loading state banner ("Carregando…")
 */
@Composable
private fun PlayerStateLoadingBanner(
    message: String,
    channelName: String?
) {
    Surface(
        color = Color.Black.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.testTag("player_state_loading")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = PrimaryElectricCyan,
                modifier = Modifier.size(42.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            if (!channelName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Buffering state indicator ("Buffering…")
 */
@Composable
private fun PlayerStateBufferingBanner(message: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.8f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.testTag("player_state_buffering")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = PrimaryElectricCyan,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Reconnecting state indicator ("Reconectando (1/3)…")
 */
@Composable
private fun PlayerStateReconnectingBanner(
    attempt: Int,
    maxAttempts: Int
) {
    Surface(
        color = Color.Black.copy(alpha = 0.88f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, StatusCheckingYellow),
        modifier = Modifier.testTag("player_state_reconnecting")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = StatusCheckingYellow,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.player_reconnecting),
                    style = MaterialTheme.typography.titleSmall,
                    color = StatusCheckingYellow,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.player_reconnecting_attempt, attempt, maxAttempts),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Playback Error card ("Não foi possível reproduzir este canal." + Repetir / Voltar)
 */
@Composable
private fun PlayerStateErrorBanner(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.92f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, ErrorRed),
        modifier = Modifier
            .padding(24.dp)
            .testTag("player_state_error")
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.player_error_playback),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "O servidor do canal pode estar temporariamente indisponível.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TVButton(
                    text = stringResource(R.string.player_action_retry),
                    icon = Icons.Default.Refresh,
                    onClick = onRetry,
                    isPrimary = true,
                    testTag = "player_btn_error_retry"
                )
                TVButton(
                    text = stringResource(R.string.player_action_back),
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack,
                    isPrimary = false,
                    testTag = "player_btn_error_back"
                )
            }
        }
    }
}

/**
 * Live delay badge calculated strictly from real playback information.
 * Never invents fake delay.
 */
@Composable
private fun LiveDelayBadge(liveOffsetMs: Long?) {
    Surface(
        color = Color.Black.copy(alpha = 0.75f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, StatusLiveRed),
        modifier = Modifier.testTag("player_live_delay_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(StatusLiveRed, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val text = when {
                liveOffsetMs != null && liveOffsetMs >= 0 -> {
                    val sec = liveOffsetMs / 1000.0
                    stringResource(
                        R.string.player_live_delay_format,
                        String.format(Locale.US, "%.1f", sec)
                    )
                }
                else -> stringResource(R.string.player_live_delay_unknown)
            }
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Channel verification status badge (🟢 Online, 🔴 Offline, 🟡 A verificar, ⚪ Indeterminado)
 */
@Composable
private fun ChannelStatusBadge(status: StreamStatus) {
    Surface(
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${status.symbol} ${status.labelPt}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * TV Projector Styled Icon Button supporting mouse hover and D-pad remote focus.
 */
@Composable
fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        shape = CircleShape,
        color = when {
            isFocused -> SurfaceCardHover
            isHovered -> SurfaceDark
            else -> Color.Black.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            1.5.dp,
            if (isFocused || isHovered) BorderFocused else BorderSubtle
        ),
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isFocused || isHovered) PrimaryElectricCyan else tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
