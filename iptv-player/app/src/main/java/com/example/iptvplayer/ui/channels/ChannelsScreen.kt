package com.example.iptvplayer.ui.channels

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.domain.model.ChannelSortOption
import com.example.iptvplayer.domain.model.Playlist
import com.example.iptvplayer.domain.model.StreamStatus
import com.example.iptvplayer.domain.repository.ChannelRepository
import com.example.iptvplayer.domain.repository.PlaylistRepository
import com.example.iptvplayer.domain.repository.UserRepository
import com.example.iptvplayer.ui.components.TVButton
import com.example.iptvplayer.ui.components.TVEmptyState
import com.example.iptvplayer.ui.components.TVSectionTitle
import com.example.ui.theme.BorderFocused
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryElectricCyan
import com.example.ui.theme.PrimarySapphire
import com.example.ui.theme.StatusCheckingYellow
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
fun ChannelsScreen(
    channelRepository: ChannelRepository,
    playlistRepository: PlaylistRepository,
    userRepository: UserRepository,
    selectedPlaylistId: String? = null,
    onChannelClick: (Channel) -> Unit = {},
    onNavigateToAddPlaylist: () -> Unit = {},
    onClearPlaylistFilter: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ChannelsViewModel = viewModel(
        factory = ChannelsViewModel.Factory(
            channelRepository = channelRepository,
            playlistRepository = playlistRepository,
            userRepository = userRepository
        )
    )
) {
    LaunchedEffect(selectedPlaylistId) {
        if (selectedPlaylistId != null) {
            viewModel.setSelectedPlaylist(selectedPlaylistId)
        }
    }

    val playlists by viewModel.playlists.collectAsState()
    val activePlaylistId by viewModel.selectedPlaylistId.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val hasHistory by viewModel.hasHistory.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val filteredCount by viewModel.filteredCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val selectedChannelForDetails by viewModel.selectedChannelForDetails.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    val formattedTotal = remember(totalCount) {
        NumberFormat.getInstance(Locale("pt", "AO")).format(totalCount)
    }

    // Modal Channel Details Dialog
    if (selectedChannelForDetails != null) {
        ChannelDetailsDialog(
            channel = selectedChannelForDetails!!,
            onDismiss = { viewModel.closeChannelDetails() },
            onPlay = {
                viewModel.markChannelPlayed(it.id)
                viewModel.closeChannelDetails()
                onChannelClick(it)
            },
            onToggleFavorite = {
                viewModel.toggleFavorite(it.id)
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
            .testTag("channels_screen_root")
    ) {
        // Section Title with real count badge
        item(span = { GridItemSpan(maxLineSpan) }) {
            TVSectionTitle(
                title = stringResource(R.string.channels_header_title),
                badgeText = "$filteredCount ${if (filteredCount == 1) "canal" else "canais"}"
            )
        }

        // Empty state when user has NO playlists at all
        if (playlists.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                TVEmptyState(
                    title = stringResource(R.string.empty_channels_no_playlist_notice),
                    description = stringResource(R.string.empty_channels_desc),
                    icon = Icons.Default.PlaylistPlay,
                    actionText = stringResource(R.string.action_add_playlist_primary),
                    onActionClick = onNavigateToAddPlaylist
                )
            }
        } else {
            // 1. Playlist Selection Area (if multiple playlists exist or user switches)
            item(span = { GridItemSpan(maxLineSpan) }) {
                PlaylistSelectorRow(
                    playlists = playlists,
                    selectedPlaylistId = activePlaylistId,
                    onSelectPlaylist = { playlistId ->
                        viewModel.setSelectedPlaylist(playlistId)
                        if (playlistId == null) {
                            onClearPlaylistFilter()
                        }
                    }
                )
            }

            // 2. Search Bar and Sorting Control Row
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("channels_search_input"),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_channels_prompt),
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.action_search),
                                tint = PrimaryElectricCyan
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.action_clear_search_btn),
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BorderFocused,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SurfaceCardHover,
                            unfocusedContainerColor = SurfaceCard,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // Sorting Button & Dropdown Menu
                    Box {
                        val sortInteractionSource = remember { MutableInteractionSource() }
                        val isSortFocused by sortInteractionSource.collectIsFocusedAsState()
                        val isSortHovered by sortInteractionSource.collectIsHoveredAsState()

                        Surface(
                            modifier = Modifier
                                .testTag("btn_channel_sort")
                                .pointerHoverIcon(PointerIcon.Hand)
                                .border(
                                    width = if (isSortFocused) 2.dp else 1.dp,
                                    color = if (isSortFocused) BorderFocused else BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = sortInteractionSource,
                                    indication = null,
                                    onClick = { showSortMenu = true }
                                )
                                .focusable(interactionSource = sortInteractionSource),
                            color = if (isSortFocused) SurfaceCardFocused else if (isSortHovered) SurfaceCardHover else SurfaceCard,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = stringResource(R.string.sort_dialog_title),
                                    tint = PrimaryElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = sortOption.labelPt,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(SurfaceCard)
                        ) {
                            val availableOptions = buildList {
                                add(ChannelSortOption.NAME_ASC)
                                add(ChannelSortOption.NAME_DESC)
                                add(ChannelSortOption.CATEGORY)
                                if (hasHistory) {
                                    add(ChannelSortOption.RECENT)
                                    add(ChannelSortOption.MOST_PLAYED)
                                }
                            }

                            availableOptions.forEach { option ->
                                val isSelected = sortOption == option
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = option.labelPt,
                                                color = if (isSelected) PrimaryElectricCyan else TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = PrimaryElectricCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Category Navigation Rail (with "Todos · 2.458")
            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoryNavigationRow(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    formattedTotal = formattedTotal,
                    onSelectCategory = { category ->
                        viewModel.selectCategory(category)
                    }
                )
            }

            // 4. Empty State: Search or Empty Playlist
            if (channels.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    if (searchQuery.isNotBlank()) {
                        TVEmptyState(
                            title = stringResource(R.string.empty_search_no_results_notice),
                            description = "Não foram encontrados canais correspondentes à pesquisa \"$searchQuery\".",
                            icon = Icons.Default.Search,
                            actionText = stringResource(R.string.action_clear_search_btn),
                            onActionClick = { viewModel.setSearchQuery("") }
                        )
                    } else if (totalCount == 0) {
                        TVEmptyState(
                            title = stringResource(R.string.empty_playlist_no_channels_notice),
                            description = "A lista selecionada não possui canais disponíveis para reprodução.",
                            icon = Icons.Default.Tv
                        )
                    } else {
                        TVEmptyState(
                            title = stringResource(R.string.empty_search_no_results_notice),
                            description = "Não existem canais nesta categoria.",
                            icon = Icons.Default.Search,
                            actionText = "Ver todos os canais",
                            onActionClick = { viewModel.selectCategory("Todos") }
                        )
                    }
                }
            } else {
                // 5. Channel List (TV-Friendly Cards)
                items(channels, key = { it.id }) { channel ->
                    ChannelTVCard(
                        channel = channel,
                        onPlay = {
                            viewModel.markChannelPlayed(channel.id)
                            onChannelClick(channel)
                        },
                        onOpenInfo = {
                            viewModel.openChannelDetails(channel)
                        },
                        onToggleFavorite = {
                            viewModel.toggleFavorite(channel.id)
                        }
                    )
                }

                // 6. Pagination / Load More Button for large playlists (5,000 to 20,000+ channels)
                if (channels.size < filteredCount) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TVButton(
                                text = "${stringResource(R.string.load_more_channels)} (${channels.size} de $filteredCount)",
                                icon = Icons.Default.ArrowDropDown,
                                onClick = { viewModel.loadMore() },
                                isPrimary = false,
                                testTag = "btn_load_more_channels"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1. Playlist selection bar showing available user playlists.
 * Allows switching between playlists or viewing all user channels.
 */
@Composable
private fun PlaylistSelectorRow(
    playlists: List<Playlist>,
    selectedPlaylistId: String?,
    onSelectPlaylist: (String?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.select_playlist_title),
            style = MaterialTheme.typography.labelMedium.copy(
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // "Todas as listas" chip
            item {
                val isAllSelected = selectedPlaylistId == null
                PlaylistChip(
                    title = stringResource(R.string.all_playlists_chip),
                    isSelected = isAllSelected,
                    onClick = { onSelectPlaylist(null) },
                    testTag = "playlist_chip_all"
                )
            }

            // Real user playlist chips
            items(playlists, key = { it.id }) { playlist ->
                val isSelected = selectedPlaylistId == playlist.id
                PlaylistChip(
                    title = playlist.name,
                    count = playlist.channelCount,
                    isSelected = isSelected,
                    onClick = { onSelectPlaylist(playlist.id) },
                    testTag = "playlist_chip_${playlist.id}"
                )
            }
        }
    }
}

@Composable
private fun PlaylistChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    count: Int? = null,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> BorderFocused
            isSelected -> PrimaryElectricCyan
            isHovered -> BorderFocused.copy(alpha = 0.6f)
            else -> BorderSubtle
        },
        label = "pl_chip_border"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> PrimaryElectricCyan.copy(alpha = 0.35f)
            isSelected -> PrimarySapphire.copy(alpha = 0.55f)
            isHovered -> SurfaceCardHover
            else -> SurfaceCard
        },
        label = "pl_chip_bg"
    )

    Surface(
        modifier = Modifier
            .testTag(testTag)
            .pointerHoverIcon(PointerIcon.Hand)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        color = backgroundColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = if (isSelected || isFocused) TextPrimary else TextSecondary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            )
            if (count != null && count > 0) {
                Surface(
                    color = if (isSelected) PrimaryElectricCyan.copy(alpha = 0.25f) else SurfaceCardHover,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) PrimaryElectricCyan else TextMuted,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * 3. Category navigation row with "Todos · 2.458" and actual group-title categories.
 */
@Composable
private fun CategoryNavigationRow(
    categories: List<String>,
    selectedCategory: String,
    formattedTotal: String,
    onSelectCategory: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories, key = { it }) { category ->
            val isSelected = selectedCategory.equals(category, ignoreCase = true)
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            val isHovered by interactionSource.collectIsHoveredAsState()

            val borderColor by animateColorAsState(
                targetValue = when {
                    isFocused -> BorderFocused
                    isSelected -> PrimaryElectricCyan
                    isHovered -> BorderFocused.copy(alpha = 0.5f)
                    else -> BorderSubtle
                },
                label = "cat_chip_border"
            )

            val backgroundColor by animateColorAsState(
                targetValue = when {
                    isFocused -> PrimaryElectricCyan.copy(alpha = 0.35f)
                    isSelected -> PrimarySapphire.copy(alpha = 0.5f)
                    isHovered -> SurfaceCardHover
                    else -> SurfaceCard
                },
                label = "cat_chip_bg"
            )

            val labelText = if (category.equals("Todos", ignoreCase = true)) {
                "Todos · $formattedTotal"
            } else {
                category
            }

            Surface(
                modifier = Modifier
                    .testTag("category_chip_$category")
                    .pointerHoverIcon(PointerIcon.Hand)
                    .border(
                        width = if (isFocused) 2.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onSelectCategory(category) }
                    )
                    .focusable(interactionSource = interactionSource),
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = labelText,
                    color = if (isSelected || isFocused) TextPrimary else TextSecondary,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * 2. TV-friendly Channel Card.
 * Displays logo (or TV icon fallback), channel name, category tag, status indicator,
 * favorite button, and play / detail trigger.
 */
@Composable
private fun ChannelTVCard(
    channel: Channel,
    onPlay: () -> Unit,
    onOpenInfo: () -> Unit,
    onToggleFavorite: () -> Unit,
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
        label = "ch_card_border"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> SurfaceCardFocused
            isHovered -> SurfaceCardHover
            else -> SurfaceCard
        },
        label = "ch_card_bg"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(115.dp)
            .testTag("channel_card_${channel.id}")
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
                // Channel Logo with Fallback Icon
                ChannelLogoView(
                    logoUrl = channel.logoUrl,
                    channelName = channel.name,
                    size = 46.dp
                )

                // Channel Name, Category & Status Indicator
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
                        // Category pill
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

                        // Stream Status indicator: 🟢 🔴 🟡 ⚪
                        StatusBadge(status = channel.status)
                    }
                }
            }

            // Quick Actions: Heart (Favorite) & Play
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Favorite Button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("btn_fav_${channel.id}")
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (channel.isFavorite) {
                            stringResource(R.string.action_favorite_remove)
                        } else {
                            stringResource(R.string.action_favorite_add)
                        },
                        tint = if (channel.isFavorite) Color(0xFFFF5252) else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Info Action Button
                IconButton(
                    onClick = onOpenInfo,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("btn_info_${channel.id}")
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

/**
 * 10. Channel Details Dialog.
 * Shows useful, user-friendly channel metadata:
 * Nome, Categoria, TVG ID, TVG name, Estado, Última verificação.
 * Actions: Reproduzir canal, Favorito, Fechar.
 */
@Composable
fun ChannelDetailsDialog(
    channel: Channel,
    onDismiss: () -> Unit,
    onPlay: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit
) {
    val lastCheckedText = remember(channel.lastCheckedTimestamp) {
        if (channel.lastCheckedTimestamp == null || channel.lastCheckedTimestamp == 0L) {
            "Nunca verificado"
        } else {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "AO"))
            sdf.format(Date(channel.lastCheckedTimestamp))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(max = 520.dp)
            .testTag("dialog_channel_details"),
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChannelLogoView(
                    logoUrl = channel.logoUrl,
                    channelName = channel.name,
                    size = 40.dp
                )
                Column {
                    Text(
                        text = stringResource(R.string.channel_info_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryElectricCyan
                        )
                    )
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow(
                    label = stringResource(R.string.channel_info_name),
                    value = channel.name
                )
                DetailRow(
                    label = stringResource(R.string.channel_info_category),
                    value = channel.category
                )
                DetailRow(
                    label = stringResource(R.string.channel_info_tvg_id),
                    value = channel.tvgId ?: stringResource(R.string.channel_info_not_specified)
                )
                DetailRow(
                    label = stringResource(R.string.channel_info_tvg_name),
                    value = channel.tvgName ?: stringResource(R.string.channel_info_not_specified)
                )
                DetailRow(
                    label = stringResource(R.string.channel_info_status),
                    value = "${channel.status.symbol} ${channel.status.labelPt}"
                )
                DetailRow(
                    label = stringResource(R.string.channel_info_last_check),
                    value = lastCheckedText
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorite Toggle
                TVButton(
                    text = if (channel.isFavorite) {
                        stringResource(R.string.action_favorite_remove)
                    } else {
                        stringResource(R.string.action_favorite_add)
                    },
                    icon = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    onClick = { onToggleFavorite(channel) },
                    isPrimary = false,
                    testTag = "btn_dialog_favorite"
                )

                // Play Channel Button
                TVButton(
                    text = stringResource(R.string.action_play_channel_btn),
                    icon = Icons.Default.PlayArrow,
                    onClick = { onPlay(channel) },
                    isPrimary = true,
                    testTag = "btn_dialog_play"
                )
            }
        },
        dismissButton = {
            TVButton(
                text = stringResource(R.string.action_close_dialog),
                onClick = onDismiss,
                isPrimary = false,
                testTag = "btn_dialog_close"
            )
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 240.dp)
        )
    }
}

/**
 * Reusable Channel Logo with Coil image loading and clean TV fallback icon.
 */
@Composable
fun ChannelLogoView(
    logoUrl: String?,
    channelName: String,
    size: androidx.compose.ui.unit.Dp
) {
    val context = LocalContext.current

    Surface(
        color = PrimarySapphire.copy(alpha = 0.35f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, PrimaryElectricCyan.copy(alpha = 0.35f)),
        modifier = Modifier.size(size)
    ) {
        if (!logoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = channelName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                error = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = channelName,
                            tint = PrimaryElectricCyan,
                            modifier = Modifier.size(size * 0.55f)
                        )
                    }
                },
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = channelName,
                            tint = PrimaryElectricCyan.copy(alpha = 0.5f),
                            modifier = Modifier.size(size * 0.55f)
                        )
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = channelName,
                    tint = PrimaryElectricCyan,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
    }
}

/**
 * 9. Stream Status Badge:
 * 🟢 Online
 * 🔴 Offline
 * 🟡 A verificar
 * ⚪ Indeterminado (default when never checked)
 */
@Composable
fun StatusBadge(status: StreamStatus) {
    val statusColor = when (status) {
        StreamStatus.ONLINE -> StatusOnlineGreen
        StreamStatus.OFFLINE -> ErrorRed
        StreamStatus.CHECKING -> StatusCheckingYellow
        StreamStatus.UNKNOWN -> TextMuted
    }
    val labelText = when (status) {
        StreamStatus.ONLINE -> "Online"
        StreamStatus.OFFLINE -> "Offline"
        StreamStatus.CHECKING -> "A verificar"
        StreamStatus.UNKNOWN -> "Indeterminado"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Text(
            text = labelText,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontSize = 11.sp
            )
        )
    }
}
