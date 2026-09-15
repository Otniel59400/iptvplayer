package com.example.iptvplayer.ui.checker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.domain.model.Playlist
import com.example.iptvplayer.domain.model.StreamStatus
import com.example.iptvplayer.domain.repository.PlaylistRepository
import com.example.iptvplayer.domain.repository.StreamCheckerRepository
import com.example.iptvplayer.domain.repository.UserRepository
import com.example.iptvplayer.ui.components.TVButton
import com.example.iptvplayer.ui.components.TVCard
import com.example.iptvplayer.ui.components.TVEmptyState
import com.example.iptvplayer.ui.components.TVSectionTitle
import com.example.iptvplayer.ui.components.TVStatusBadge
import com.example.ui.theme.BorderFocused
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryElectricCyan
import com.example.ui.theme.PrimarySapphire
import com.example.ui.theme.StatusCheckingYellow
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
fun CheckerScreen(
    checkerRepository: StreamCheckerRepository,
    playlistRepository: PlaylistRepository,
    userRepository: UserRepository,
    modifier: Modifier = Modifier,
    viewModel: CheckerViewModel = viewModel(
        factory = CheckerViewModel.provideFactory(
            checkerRepository = checkerRepository,
            playlistRepository = playlistRepository,
            userRepository = userRepository
        )
    )
) {
    val numberFormat = remember { NumberFormat.getInstance(Locale("pt", "PT")) }
    val playlists by viewModel.userPlaylists.collectAsState()
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val progressState by viewModel.progressState.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val totalFilteredCount by viewModel.totalFilteredCount.collectAsState()

    val selectedPlaylist = playlists.find { it.id == selectedPlaylistId }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("checker_screen_root")
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Header
        item {
            TVSectionTitle(
                title = stringResource(R.string.checker_header_title),
                badgeText = selectedPlaylist?.name ?: "Diagnóstico"
            )
        }

        // 2. Playlist Selector (if user has multiple playlists or needs selection)
        if (playlists.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.select_playlist_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            val isSelected = playlist.id == selectedPlaylistId
                            TVPlaylistChip(
                                playlist = playlist,
                                isSelected = isSelected,
                                onClick = { viewModel.selectPlaylist(playlist.id) },
                                testTag = "checker_playlist_chip_${playlist.id}"
                            )
                        }
                    }
                }
            }
        }

        // 3. Real Database Statistics Panel
        item {
            TVCard(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                testTag = "checker_statistics_card"
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estatísticas da Playlist",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${stringResource(R.string.checker_stat_total)}: ${numberFormat.format(stats.total)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatTile(
                            symbol = "🟢",
                            label = stringResource(R.string.checker_stat_online),
                            count = stats.online,
                            color = StatusOnlineGreen,
                            modifier = Modifier.weight(1f),
                            testTag = "stat_online_count"
                        )
                        StatTile(
                            symbol = "🔴",
                            label = stringResource(R.string.checker_stat_offline),
                            count = stats.offline,
                            color = StatusOfflineRed,
                            modifier = Modifier.weight(1f),
                            testTag = "stat_offline_count"
                        )
                        StatTile(
                            symbol = "🟡",
                            label = stringResource(R.string.checker_stat_checking),
                            count = stats.checking,
                            color = StatusCheckingYellow,
                            modifier = Modifier.weight(1f),
                            testTag = "stat_checking_count"
                        )
                        StatTile(
                            symbol = "⚪",
                            label = stringResource(R.string.checker_stat_indeterminate),
                            count = stats.indeterminate,
                            color = TextMuted,
                            modifier = Modifier.weight(1f),
                            testTag = "stat_indeterminate_count"
                        )
                    }
                }
            }
        }

        // 4. Live Progress / Verification Controller
        item {
            TVCard(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                testTag = "checker_control_card"
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    if (progressState.isRunning) {
                        // Verification is currently executing!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = PrimaryElectricCyan,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(
                                            R.string.checker_verifying_progress,
                                            numberFormat.format(progressState.verifiedChannels),
                                            numberFormat.format(progressState.totalChannels)
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (progressState.currentChannelName.isNotBlank()) {
                                        Text(
                                            text = stringResource(
                                                R.string.checker_verifying_current,
                                                progressState.currentChannelName
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PrimaryElectricCyan,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            val percentage = (progressState.progressFraction * 100f).coerceIn(0f, 100f)
                            Text(
                                text = String.format(Locale("pt", "PT"), "%.1f%%", percentage),
                                style = MaterialTheme.typography.titleLarge,
                                color = PrimaryElectricCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { progressState.progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .testTag("checker_progress_bar"),
                            color = PrimaryElectricCyan,
                            trackColor = SurfaceCardHover
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Live verified counts row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🟢 ${numberFormat.format(progressState.onlineCount)}   🔴 ${numberFormat.format(progressState.offlineCount)}   ⚪ ${numberFormat.format(progressState.indeterminateCount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )

                            val remaining = (progressState.totalChannels - progressState.verifiedChannels).coerceAtLeast(0)
                            Text(
                                text = "${stringResource(R.string.checker_stat_remaining)}: ${numberFormat.format(remaining)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stop Button
                        TVButton(
                            text = stringResource(R.string.checker_action_stop),
                            icon = Icons.Default.Stop,
                            onClick = { viewModel.stopVerification() },
                            isPrimary = false,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "btn_stop_stream_check"
                        )
                    } else {
                        // Verification is NOT running
                        if (progressState.isInterrupted) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StatusOfflineRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = StatusOfflineRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.checker_status_interrupted),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StatusOfflineRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        } else if (progressState.isCompleted && progressState.totalChannels > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StatusOnlineGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusOnlineGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.checker_status_completed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StatusOnlineGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        val hasVerifiedResults = (stats.online + stats.offline + stats.indeterminate) > 0
                        val buttonText = if (hasVerifiedResults) {
                            stringResource(R.string.checker_action_recheck)
                        } else {
                            stringResource(R.string.checker_action_start)
                        }
                        val buttonIcon = if (hasVerifiedResults) Icons.Default.Refresh else Icons.Default.PlayArrow

                        TVButton(
                            text = buttonText,
                            icon = buttonIcon,
                            onClick = {
                                if (selectedPlaylistId != null && stats.total > 0) {
                                    viewModel.startVerification()
                                }
                            },
                            isPrimary = true,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "btn_start_stream_check"
                        )
                    }
                }
            }
        }

        // 5. Concurrency & HY300 Architecture Notice
        item {
            TVCard(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                testTag = "checker_performance_card"
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Controlo Inteligente de Concorrência (HY300)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Para não sobrecarregar o hardware do projetor HY300, a verificação opera com fila assíncrona (máximo 3 conexões simultâneas), tempo limite (timeout) de 4 segundos e medição de latência milissegundo a milissegundo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 6. Filter and Sort Controls
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtrar e Ordenar Canais",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${numberFormat.format(totalFilteredCount)} canais",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter chips: Todos, 🟢 Online, 🔴 Offline, 🟡 A verificar, ⚪ Indeterminado
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterStatusChip(
                            label = stringResource(R.string.checker_filter_all),
                            count = stats.total,
                            isSelected = statusFilter == null,
                            onClick = { viewModel.setStatusFilter(null) },
                            testTag = "filter_chip_all"
                        )
                    }
                    item {
                        FilterStatusChip(
                            label = "🟢 ${stringResource(R.string.checker_stat_online)}",
                            count = stats.online,
                            isSelected = statusFilter == StreamStatus.ONLINE,
                            onClick = { viewModel.setStatusFilter(StreamStatus.ONLINE) },
                            testTag = "filter_chip_online"
                        )
                    }
                    item {
                        FilterStatusChip(
                            label = "🔴 ${stringResource(R.string.checker_stat_offline)}",
                            count = stats.offline,
                            isSelected = statusFilter == StreamStatus.OFFLINE,
                            onClick = { viewModel.setStatusFilter(StreamStatus.OFFLINE) },
                            testTag = "filter_chip_offline"
                        )
                    }
                    item {
                        FilterStatusChip(
                            label = "🟡 ${stringResource(R.string.checker_stat_checking)}",
                            count = stats.checking,
                            isSelected = statusFilter == StreamStatus.CHECKING,
                            onClick = { viewModel.setStatusFilter(StreamStatus.CHECKING) },
                            testTag = "filter_chip_checking"
                        )
                    }
                    item {
                        FilterStatusChip(
                            label = "⚪ ${stringResource(R.string.checker_stat_indeterminate)}",
                            count = stats.indeterminate,
                            isSelected = statusFilter == StreamStatus.UNKNOWN,
                            onClick = { viewModel.setStatusFilter(StreamStatus.UNKNOWN) },
                            testTag = "filter_chip_indeterminate"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sort options row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CheckerSort.values(), key = { it.name }) { sort ->
                        SortChip(
                            sort = sort,
                            isSelected = sortOption == sort,
                            onClick = { viewModel.setSortOption(sort) },
                            testTag = "sort_chip_${sort.name.lowercase()}"
                        )
                    }
                }
            }
        }

        // 7. Channels List
        if (channels.isEmpty()) {
            item {
                TVEmptyState(
                    title = "Nenhum canal encontrado",
                    description = if (statusFilter != null) "Nenhum canal corresponde ao filtro selecionado." else "Esta playlist não contém canais.",
                    icon = Icons.Default.Tv,
                    actionText = if (statusFilter != null) "Limpar filtro" else null,
                    onActionClick = { viewModel.setStatusFilter(null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(channels, key = { it.id }) { channel ->
                CheckerChannelRow(
                    channel = channel,
                    testTag = "checker_channel_row_${channel.id}"
                )
            }

            if (totalFilteredCount > channels.size) {
                item {
                    TVButton(
                        text = stringResource(R.string.load_more_channels),
                        onClick = { viewModel.loadMoreChannels() },
                        isPrimary = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        testTag = "btn_load_more_checker_channels"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    symbol: String,
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val numberFormat = remember { NumberFormat.getInstance(Locale("pt", "PT")) }
    Surface(
        modifier = modifier.testTag(testTag),
        color = SurfaceCardHover,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = symbol, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = numberFormat.format(count),
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TVPlaylistChip(
    playlist: Playlist,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor = when {
        isFocused -> BorderFocused
        isSelected -> PrimaryElectricCyan
        isHovered -> PrimarySapphire.copy(alpha = 0.5f)
        else -> BorderSubtle
    }

    val bgColor = when {
        isSelected -> PrimarySapphire.copy(alpha = 0.35f)
        isFocused -> SurfaceCardFocused
        isHovered -> SurfaceCardHover
        else -> SurfaceCard
    }

    Surface(
        modifier = Modifier
            .testTag(testTag)
            .border(if (isFocused || isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand),
        color = bgColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlaylistPlay,
                contentDescription = null,
                tint = if (isSelected) PrimaryElectricCyan else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) PrimaryElectricCyan else TextPrimary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = "${playlist.channelCount} canais",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun FilterStatusChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val numberFormat = remember { NumberFormat.getInstance(Locale("pt", "PT")) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor = when {
        isFocused -> BorderFocused
        isSelected -> PrimaryElectricCyan
        isHovered -> BorderSubtle
        else -> Color.Transparent
    }

    val bgColor = when {
        isSelected -> PrimaryElectricCyan.copy(alpha = 0.2f)
        isFocused -> SurfaceCardFocused
        isHovered -> SurfaceCardHover
        else -> SurfaceCard
    }

    Surface(
        modifier = Modifier
            .testTag(testTag)
            .border(if (isFocused || isSelected) 1.5.dp else 0.dp, borderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand),
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) PrimaryElectricCyan else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                color = SurfaceCardHover,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = numberFormat.format(count),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) PrimaryElectricCyan else TextMuted,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun SortChip(
    sort: CheckerSort,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = if (isSelected || isFocused) PrimaryElectricCyan else BorderSubtle
    val bgColor = if (isSelected) PrimaryElectricCyan.copy(alpha = 0.15f) else Color.Transparent

    Surface(
        modifier = Modifier
            .testTag(testTag)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand),
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = null,
                tint = if (isSelected) PrimaryElectricCyan else TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = sort.labelPt,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) PrimaryElectricCyan else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CheckerChannelRow(
    channel: Channel,
    testTag: String
) {
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale("pt", "PT")) }

    TVCard(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        testTag = testTag,
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Logo / Placeholder
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = SurfaceCardHover,
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(channel.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = channel.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = PrimaryElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Name & Category
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (channel.lastCheckedTimestamp != null && channel.lastCheckedTimestamp > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${timeFormat.format(Date(channel.lastCheckedTimestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Latency Badge (e.g. "245 ms")
            if (channel.latencyMs != null && channel.latencyMs > 0) {
                Surface(
                    color = SurfaceCardHover,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = PrimaryElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${channel.latencyMs} ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            // Stream Status Badge
            TVStatusBadge(status = channel.status)
        }
    }
}
