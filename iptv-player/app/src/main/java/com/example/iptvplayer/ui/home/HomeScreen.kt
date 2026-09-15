package com.example.iptvplayer.ui.home

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.domain.model.Playlist
import com.example.iptvplayer.domain.model.PlaylistType
import com.example.iptvplayer.domain.model.StreamStatus
import com.example.iptvplayer.domain.model.User
import com.example.iptvplayer.navigation.Screen
import com.example.iptvplayer.ui.components.TVButton
import com.example.iptvplayer.ui.components.TVCard
import com.example.iptvplayer.ui.components.TVSectionTitle
import com.example.ui.theme.BorderFocused
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryElectricCyan
import com.example.ui.theme.PrimarySapphire
import com.example.ui.theme.StatusOfflineRed
import com.example.ui.theme.StatusOnlineGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardFocused
import com.example.ui.theme.SurfaceCardHover
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (Screen) -> Unit,
    onNavigateToAddPlaylist: () -> Unit,
    onOpenPlaylistChannels: (String) -> Unit,
    onPlayChannel: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeUser by viewModel.activeUser.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val recentChannels by viewModel.recentChannels.collectAsState()
    val onlineCount by viewModel.onlineCount.collectAsState()
    val offlineCount by viewModel.offlineCount.collectAsState()
    val verifiedCount by viewModel.verifiedCount.collectAsState()
    val numberFormat = remember { NumberFormat.getInstance(Locale("pt", "PT")) }

    val userName = activeUser?.displayTitle ?: stringResource(R.string.user_default_name)
    val avatarColor = activeUser?.avatarColor ?: 0xFF00E5FF

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_content")
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentPadding = PaddingValues(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Personalized Welcome Banner for Active User (16:9 TV Layout)
        item {
            TVCard(
                onClick = { onNavigate(Screen.Users) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "home_hero_card"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(avatarColor))
                                .border(2.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userName.firstOrNull()?.uppercase() ?: "U",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black
                                )
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = PrimaryElectricCyan.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, PrimaryElectricCyan)
                                ) {
                                    Text(
                                        text = stringResource(R.string.active_profile_label),
                                        color = PrimaryElectricCyan,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.greeting_user, userName),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.banner_tip_remote),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // Profile Quick Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TVButton(
                            text = stringResource(R.string.action_switch_user),
                            icon = Icons.Default.SwitchAccount,
                            onClick = { onNavigate(Screen.SelectProfile) },
                            isPrimary = false,
                            testTag = "hero_switch_user_btn"
                        )
                    }
                }
            }
        }

        // Section: Estado dos canais (Shown ONLY when real verification data exists)
        if (verifiedCount > 0) {
            item {
                TVCard(
                    onClick = { onNavigate(Screen.Checker) },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "home_channel_status_card"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Estado dos canais",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🟢 ${numberFormat.format(onlineCount)} Online",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StatusOnlineGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "🔴 ${numberFormat.format(offlineCount)} Offline",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StatusOfflineRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        TVButton(
                            text = "Ver detalhes",
                            icon = Icons.Default.CheckCircleOutline,
                            onClick = { onNavigate(Screen.Checker) },
                            isPrimary = false,
                            testTag = "btn_home_view_checker"
                        )
                    }
                }
            }
        }

        // Section: Guia Quando Não Tem Playlists (Zero Playlists State)
        if (playlists.isEmpty()) {
            item {
                HomeEmptyPlaylistsCard(
                    onAddPlaylist = onNavigateToAddPlaylist,
                    onViewPlaylists = { onNavigate(Screen.Playlists) }
                )
            }
        }

        // Section: Main Dashboard Cards (Logical D-Pad & Mouse Navigation)
        // Focus order: Adicionar playlist -> Playlists -> Canais -> Favoritos -> Verificar canais -> Utilizadores -> Definições
        item {
            Column {
                TVSectionTitle(
                    title = "Navegação Principal",
                    badgeText = "7 atalhos"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeNavCard(
                        title = stringResource(R.string.action_add_playlist),
                        icon = Icons.Default.Add,
                        accentColor = PrimaryElectricCyan,
                        badge = if (playlists.isEmpty()) "Comece aqui" else null,
                        onClick = onNavigateToAddPlaylist,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_card_add_playlist"
                    )
                    HomeNavCard(
                        title = stringResource(R.string.nav_playlists),
                        icon = Icons.Default.PlaylistPlay,
                        accentColor = Color(0xFF64B5F6),
                        badge = "${playlists.size}",
                        onClick = { onNavigate(Screen.Playlists) },
                        modifier = Modifier.weight(1f),
                        testTag = "nav_card_playlists"
                    )
                    HomeNavCard(
                        title = stringResource(R.string.nav_channels),
                        icon = Icons.Default.Tv,
                        accentColor = Color(0xFF81C784),
                        onClick = { onNavigate(Screen.Channels) },
                        modifier = Modifier.weight(1f),
                        testTag = "nav_card_channels"
                    )
                    HomeNavCard(
                        title = stringResource(R.string.nav_favorites),
                        icon = Icons.Default.Favorite,
                        accentColor = Color(0xFFFF5252),
                        badge = "${favoriteChannels.size}",
                        onClick = { onNavigate(Screen.Favorites) },
                        modifier = Modifier.weight(1f),
                        testTag = "nav_card_favorites"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeNavCard(
                        title = stringResource(R.string.nav_checker),
                        icon = Icons.Default.CheckCircleOutline,
                        accentColor = Color(0xFFFFB74D),
                        onClick = { onNavigate(Screen.Checker) },
                        modifier = Modifier.weight(1f),
                        testTag = "nav_card_checker"
                    )
                    HomeNavCard(
                        title = stringResource(R.string.nav_users),
                        icon = Icons.Default.ManageAccounts,
                        accentColor = Color(0xFFBA68C8),
                        onClick = { onNavigate(Screen.Users) },
                        modifier = Modifier.weight(1f),
                        testTag = "nav_card_users"
                    )
                    HomeNavCard(
                        title = stringResource(R.string.nav_settings),
                        icon = Icons.Default.Settings,
                        accentColor = Color(0xFF90A4AE),
                        onClick = { onNavigate(Screen.Settings) },
                        modifier = Modifier.weight(1f),
                        testTag = "nav_card_settings"
                    )
                }
            }
        }

        // Section: Ações Rápidas
        item {
            Column {
                TVSectionTitle(
                    title = stringResource(R.string.section_quick_actions),
                    badgeText = "Acesso direto"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        title = stringResource(R.string.action_add_playlist),
                        icon = Icons.Default.Add,
                        onClick = onNavigateToAddPlaylist,
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_add_playlist"
                    )
                    QuickActionButton(
                        title = stringResource(R.string.action_view_channels),
                        icon = Icons.Default.Tv,
                        onClick = { onNavigate(Screen.Channels) },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_view_channels"
                    )
                    QuickActionButton(
                        title = stringResource(R.string.nav_favorites),
                        icon = Icons.Default.Favorite,
                        onClick = { onNavigate(Screen.Favorites) },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_favorites"
                    )
                    QuickActionButton(
                        title = stringResource(R.string.action_check_channels),
                        icon = Icons.Default.CheckCircleOutline,
                        onClick = { onNavigate(Screen.Checker) },
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_check_channels"
                    )
                }
            }
        }

        // Section: Minhas Playlists (When Playlists Exist)
        if (playlists.isNotEmpty()) {
            item {
                Column {
                    TVSectionTitle(
                        title = stringResource(R.string.section_my_playlists),
                        badgeText = "${playlists.size} listas"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        playlists.forEach { playlist ->
                            HomePlaylistCard(
                                playlist = playlist,
                                onClick = { onOpenPlaylistChannels(playlist.id) },
                                onAddAnother = onNavigateToAddPlaylist
                            )
                        }
                    }
                }
            }
        }

        // Section: Canais Recentes
        item {
            Column {
                TVSectionTitle(
                    title = stringResource(R.string.section_recent_channels),
                    badgeText = "${recentChannels.size}"
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (recentChannels.isEmpty()) {
                    HomeNoticeCard(
                        message = stringResource(R.string.empty_recent_channels_notice),
                        icon = Icons.Default.History,
                        testTag = "notice_empty_recents"
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentChannels, key = { it.id }) { channel ->
                            HomeChannelCard(
                                channel = channel,
                                onPlay = {
                                    viewModel.markChannelPlayed(channel.id)
                                    onPlayChannel(channel)
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(channel.id) },
                                testTag = "recent_channel_${channel.id}"
                            )
                        }
                    }
                }
            }
        }

        // Section: Favoritos
        item {
            Column {
                TVSectionTitle(
                    title = stringResource(R.string.section_favorites),
                    badgeText = "${favoriteChannels.size}"
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (favoriteChannels.isEmpty()) {
                    HomeNoticeCard(
                        message = stringResource(R.string.empty_favorites_notice),
                        icon = Icons.Default.FavoriteBorder,
                        actionText = stringResource(R.string.action_view_channels),
                        onAction = { onNavigate(Screen.Channels) },
                        testTag = "notice_empty_favorites"
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(favoriteChannels, key = { it.id }) { channel ->
                            HomeChannelCard(
                                channel = channel,
                                onPlay = {
                                    viewModel.markChannelPlayed(channel.id)
                                    onPlayChannel(channel)
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(channel.id) },
                                testTag = "favorite_channel_${channel.id}"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Empty playlists guided card for newly created users or users with zero lists.
 */
@Composable
private fun HomeEmptyPlaylistsCard(
    onAddPlaylist: () -> Unit,
    onViewPlaylists: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> BorderFocused
            isHovered -> BorderFocused.copy(alpha = 0.6f)
            else -> PrimarySapphire.copy(alpha = 0.5f)
        },
        label = "empty_card_border"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_empty_playlists_guidance_card")
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = PrimarySapphire.copy(alpha = 0.35f),
                shape = CircleShape,
                border = BorderStroke(1.dp, PrimaryElectricCyan.copy(alpha = 0.4f)),
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = PrimaryElectricCyan,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.welcome_home_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.welcome_home_desc),
                style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TVButton(
                    text = stringResource(R.string.action_add_playlist_primary),
                    icon = Icons.Default.Add,
                    onClick = onAddPlaylist,
                    isPrimary = true,
                    testTag = "btn_empty_home_add_playlist"
                )

                TVButton(
                    text = stringResource(R.string.action_view_playlists),
                    icon = Icons.Default.PlaylistPlay,
                    onClick = onViewPlaylists,
                    isPrimary = false,
                    testTag = "btn_empty_home_view_playlists"
                )
            }
        }
    }
}

/**
 * Large, attractive, functional TV navigation card.
 * High contrast, responsive hover & focus states, mouse- and D-pad friendly.
 */
@Composable
private fun HomeNavCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    testTag: String = "home_nav_card"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> BorderFocused
            isHovered -> BorderFocused.copy(alpha = 0.7f)
            else -> BorderSubtle
        },
        label = "nav_border"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> SurfaceCardFocused
            isHovered -> SurfaceCardHover
            else -> SurfaceCard
        },
        label = "nav_bg"
    )

    Surface(
        modifier = modifier
            .height(115.dp)
            .testTag(testTag)
            .pointerHoverIcon(PointerIcon.Hand)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        color = backgroundColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (badge != null) {
                    Surface(
                        color = if (isFocused) PrimaryElectricCyan else PrimarySapphire.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isFocused) Color.White else BorderSubtle)
                    ) {
                        Text(
                            text = badge,
                            color = if (isFocused) Color.Black else TextPrimary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) PrimaryElectricCyan else TextPrimary,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Quick Action Button with full focus and hover states.
 */
@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    testTag: String = "quick_action"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> BorderFocused
            isHovered -> BorderFocused.copy(alpha = 0.7f)
            isPrimary -> PrimaryElectricCyan.copy(alpha = 0.5f)
            else -> BorderSubtle
        },
        label = "qa_border"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> PrimaryElectricCyan
            isHovered -> SurfaceCardHover
            isPrimary -> PrimarySapphire.copy(alpha = 0.35f)
            else -> SurfaceCard
        },
        label = "qa_bg"
    )

    val contentColor = when {
        isFocused -> Color.Black
        isPrimary -> PrimaryElectricCyan
        else -> TextPrimary
    }

    Surface(
        modifier = modifier
            .height(50.dp)
            .testTag(testTag)
            .pointerHoverIcon(PointerIcon.Hand)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Data-driven Playlist Card on the Home Screen.
 * Shows real Name, real formatted Channel Count, Type (Local/URL), and Last Update.
 * Clicking directly opens the channel list for that playlist.
 */
@Composable
private fun HomePlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onAddAnother: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> BorderFocused
            isHovered -> BorderFocused.copy(alpha = 0.6f)
            else -> BorderSubtle
        },
        label = "pl_border"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> SurfaceCardFocused
            isHovered -> SurfaceCardHover
            else -> SurfaceCard
        },
        label = "pl_bg"
    )

    val formattedChannelCount = NumberFormat.getInstance(Locale.getDefault()).format(playlist.channelCount)
    val typeLabel = if (playlist.type == PlaylistType.URL) {
        stringResource(R.string.playlist_type_url_short)
    } else {
        stringResource(R.string.playlist_type_local_short)
    }

    val updatedDateText = remember(playlist.updatedAt) {
        val date = Date(playlist.updatedAt)
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_playlist_card_${playlist.id}")
            .pointerHoverIcon(PointerIcon.Hand)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        color = backgroundColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = if (playlist.type == PlaylistType.URL) Color(0xFF7C4DFF).copy(alpha = 0.25f) else PrimaryElectricCyan.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (playlist.type == PlaylistType.URL) Color(0xFF7C4DFF) else PrimaryElectricCyan
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (playlist.type == PlaylistType.URL) Icons.Default.Link else Icons.Default.FileOpen,
                            contentDescription = null,
                            tint = if (playlist.type == PlaylistType.URL) Color(0xFFB388FF) else PrimaryElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Channel count badge
                        Surface(
                            color = PrimarySapphire.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.playlist_channels_count_formatted, formattedChannelCount),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryElectricCyan
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Playlist type
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )

                        // Last update
                        Text(
                            text = "Atualizada: $updatedDateText",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }
            }

            // Action: Abrir canais
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = if (isFocused) PrimaryElectricCyan else PrimarySapphire.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isFocused) Color.White else PrimaryElectricCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isFocused) Color.Black else PrimaryElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_open_playlist),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isFocused) Color.Black else TextPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * TV Card for a single channel (used in Recent and Favorites sections).
 */
@Composable
private fun HomeChannelCard(
    channel: Channel,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    testTag: String = "home_channel_card"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> BorderFocused
            isHovered -> BorderFocused.copy(alpha = 0.6f)
            else -> BorderSubtle
        },
        label = "ch_border"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> SurfaceCardFocused
            isHovered -> SurfaceCardHover
            else -> SurfaceCard
        },
        label = "ch_bg"
    )

    Surface(
        modifier = Modifier
            .width(220.dp)
            .height(115.dp)
            .testTag(testTag)
            .pointerHoverIcon(PointerIcon.Hand)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onPlay
            )
            .focusable(interactionSource = interactionSource),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category badge
                Surface(
                    color = PrimarySapphire.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = channel.category,
                        style = MaterialTheme.typography.labelSmall.copy(color = PrimaryElectricCyan),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Heart favorite toggle button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (channel.isFavorite) Color(0xFFFF5252) else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isFocused) PrimaryElectricCyan else TextPrimary,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (channel.status) {
                        StreamStatus.ONLINE -> StatusOnlineGreen
                        StreamStatus.OFFLINE -> ErrorRed
                        else -> TextMuted
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = channel.status.name,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }
            }
        }
    }
}

/**
 * Generic subtle informational notice card when a section is empty.
 */
@Composable
private fun HomeNoticeCard(
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    testTag: String = "home_notice_card"
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
        color = SurfaceCard.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }

            if (actionText != null && onAction != null) {
                TVButton(
                    text = actionText,
                    onClick = onAction,
                    isPrimary = false
                )
            }
        }
    }
}
