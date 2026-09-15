package com.example.iptvplayer.ui.favorites

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.iptvplayer.domain.repository.ChannelRepository
import com.example.iptvplayer.domain.repository.UserRepository
import com.example.iptvplayer.ui.channels.ChannelDetailsDialog
import com.example.iptvplayer.ui.channels.ChannelLogoView
import com.example.iptvplayer.ui.channels.StatusBadge
import com.example.iptvplayer.ui.components.TVEmptyState
import com.example.iptvplayer.ui.components.TVSectionTitle
import com.example.ui.theme.BorderFocused
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryElectricCyan
import com.example.ui.theme.PrimarySapphire
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardFocused
import com.example.ui.theme.SurfaceCardHover
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Stage 4 Favorites Screen:
 * Displays the current authenticated user's real favorite channels.
 * Shows: "Os meus favoritos" header.
 * Empty state: "Não tem canais favoritos."
 */
@Composable
fun FavoritesScreen(
    channelRepository: ChannelRepository,
    userRepository: UserRepository,
    onChannelClick: (Channel) -> Unit = {},
    onNavigateToChannels: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val activeUser by userRepository.activeUser.collectAsState()
    val userId = activeUser?.id.orEmpty()

    val favoritesFlow = remember(userId) {
        if (userId.isNotBlank()) {
            channelRepository.getFavoriteChannels(userId)
        } else {
            flowOf(emptyList())
        }
    }
    val favorites by favoritesFlow.collectAsState(initial = emptyList())

    var selectedChannelForDetails by remember { mutableStateOf<Channel?>(null) }

    // Channel Details Dialog
    if (selectedChannelForDetails != null) {
        ChannelDetailsDialog(
            channel = selectedChannelForDetails!!,
            onDismiss = { selectedChannelForDetails = null },
            onPlay = { channel ->
                selectedChannelForDetails = null
                onChannelClick(channel)
            },
            onToggleFavorite = { channel ->
                scope.launch {
                    channelRepository.toggleFavorite(channel.id)
                    selectedChannelForDetails = null
                }
            }
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("favorites_screen_root")
    ) {
        // Section Title: "Os meus favoritos"
        item(span = { GridItemSpan(maxLineSpan) }) {
            TVSectionTitle(
                title = stringResource(R.string.favorites_header_title),
                badgeText = "${favorites.size} ${if (favorites.size == 1) "favorito" else "favoritos"}"
            )
        }

        // Empty State: "Não tem canais favoritos."
        if (favorites.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                TVEmptyState(
                    title = stringResource(R.string.empty_favorites_notice),
                    description = "Marque canais com o ícone de coração nos canais para acesso rápido e direto.",
                    icon = Icons.Default.Favorite,
                    actionText = stringResource(R.string.action_view_channels),
                    onActionClick = onNavigateToChannels
                )
            }
        } else {
            // Real Favorites List
            items(favorites, key = { it.id }) { channel ->
                FavoriteChannelTVCard(
                    channel = channel,
                    onPlay = { onChannelClick(channel) },
                    onOpenInfo = { selectedChannelForDetails = channel },
                    onRemoveFavorite = {
                        scope.launch {
                            channelRepository.toggleFavorite(channel.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FavoriteChannelTVCard(
    channel: Channel,
    onPlay: () -> Unit,
    onOpenInfo: () -> Unit,
    onRemoveFavorite: () -> Unit,
    modifier: Modifier = Modifier
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
        label = "fav_card_border"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> SurfaceCardFocused
            isHovered -> SurfaceCardHover
            else -> SurfaceCard
        },
        label = "fav_card_bg"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(115.dp)
            .testTag("favorite_card_${channel.id}")
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
                onClick = onOpenInfo
            )
            .focusable(interactionSource = interactionSource),
        color = backgroundColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Logo or TV Fallback
                ChannelLogoView(
                    logoUrl = channel.logoUrl,
                    channelName = channel.name,
                    size = 46.dp
                )

                // Channel Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = PrimarySapphire.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = channel.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = PrimaryElectricCyan,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        StatusBadge(status = channel.status)
                    }
                }
            }

            // Quick Actions: Remove from favorites, Info & Play
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Favorite Button (Red Heart)
                IconButton(
                    onClick = onRemoveFavorite,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("btn_remove_fav_${channel.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = stringResource(R.string.action_favorite_remove),
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Info Button
                IconButton(
                    onClick = onOpenInfo,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("btn_fav_info_${channel.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.channel_info_title),
                        tint = if (isFocused) PrimaryElectricCyan else TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Play Button
                Surface(
                    color = if (isFocused) PrimaryElectricCyan else PrimarySapphire.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isFocused) Color.White else PrimaryElectricCyan.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(onClick = onPlay)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isFocused) Color.Black else PrimaryElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.action_play),
                            style = MaterialTheme.typography.labelSmall.copy(
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
