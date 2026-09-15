package com.example.iptvplayer.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.BorderFocused
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryElectricCyan
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardHover
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Channel Overlay appearing directly over the player video without returning to Home.
 * Supports searching, category filtering, status indicators, logos, and instant channel switching.
 */
@Composable
fun PlayerChannelOverlay(
    isOpen: Boolean,
    channels: List<Channel>,
    categories: List<String>,
    selectedCategory: String?,
    searchQuery: String,
    currentChannelId: String?,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSelectChannel: (Channel) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { -it }),
        exit = slideOutHorizontally(targetOffsetX = { -it }),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onClose)
                .testTag("player_channel_overlay_scrim")
        ) {
            Surface(
                color = Color(0xFF0F1520).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(420.dp)
                    .clickable(enabled = false, onClick = {}) // Block scrim clicks inside drawer
                    .testTag("player_channel_overlay_drawer")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
                ) {
                    // Header: Title & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📋 ${stringResource(R.string.player_channels)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        PlayerIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_close_dialog),
                            onClick = onClose,
                            testTag = "player_overlay_close_btn"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.player_search_channels),
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = PrimaryElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryElectricCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_overlay_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Chips
                    if (categories.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { category ->
                                val isSelected = (category == (selectedCategory ?: "Todos"))
                                CategoryFilterChip(
                                    category = category,
                                    isSelected = isSelected,
                                    onClick = { onSelectCategory(category) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Channel Count Subtitle
                    Text(
                        text = "${channels.size} canais disponíveis",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Channel List
                    if (channels.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.empty_search_no_results_notice),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("player_overlay_channel_list")
                        ) {
                            items(channels, key = { it.id }) { channel ->
                                val isCurrentlyPlaying = channel.id == currentChannelId
                                ChannelOverlayItem(
                                    channel = channel,
                                    isPlaying = isCurrentlyPlaying,
                                    onClick = { onSelectChannel(channel) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Category filter chip for the player overlay.
 */
@Composable
private fun CategoryFilterChip(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when {
            isSelected -> PrimaryElectricCyan
            isFocused || isHovered -> SurfaceCardHover
            else -> SurfaceDark
        },
        border = BorderStroke(
            1.dp,
            if (isFocused || isHovered) BorderFocused else BorderSubtle
        ),
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
    ) {
        Text(
            text = category,
            color = if (isSelected) Color.Black else TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Individual channel item in the player overlay drawer.
 */
@Composable
private fun ChannelOverlayItem(
    channel: Channel,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = when {
            isPlaying -> Color(0xFF003847)
            isFocused -> SurfaceCardHover
            isHovered -> SurfaceDark
            else -> SurfaceCard
        },
        border = BorderStroke(
            1.5.dp,
            when {
                isPlaying -> PrimaryElectricCyan
                isFocused || isHovered -> BorderFocused
                else -> BorderSubtle
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .testTag("player_overlay_channel_${channel.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Logo
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.size(38.dp)
            ) {
                if (channel.logoUrl != null && channel.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(channel.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = channel.name.take(2).uppercase(),
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Channel Name & Category
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPlaying) PrimaryElectricCyan else TextPrimary,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Stream Status Badge
            Text(
                text = "${channel.status.symbol} ${channel.status.labelPt}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 11.sp
            )

            // Playing Indicator Icon
            if (isPlaying) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = PrimaryElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
