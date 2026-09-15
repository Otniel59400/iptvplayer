package com.example.iptvplayer.ui.playlists

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.iptvplayer.domain.model.Playlist
import com.example.iptvplayer.domain.model.PlaylistType
import com.example.iptvplayer.ui.components.TVButton
import com.example.iptvplayer.ui.components.TVCard
import com.example.iptvplayer.ui.components.TVEmptyState
import com.example.iptvplayer.ui.components.TVSectionTitle
import com.example.ui.theme.BorderFocused
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryElectricCyan
import com.example.ui.theme.PrimarySapphire
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardFocused
import com.example.ui.theme.SurfaceCardHover
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistViewModel,
    initialOpenAddDialog: Boolean = false,
    onOpenChannels: ((Playlist) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val playlists by viewModel.playlists.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val importStatusMessage by viewModel.importStatusMessage.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Dialog States
    var showAddChoiceDialog by remember { mutableStateOf(initialOpenAddDialog) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showFileDialog by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var renameTargetPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var deleteTargetPlaylist by remember { mutableStateOf<Playlist?>(null) }

    LaunchedEffect(initialOpenAddDialog) {
        if (initialOpenAddDialog) {
            showAddChoiceDialog = true
        }
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            var name = "Nova Playlist"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(idx) ?: name
                    }
                }
            } catch (_: Exception) {
                name = "Lista Local"
            }
            selectedFileName = name.removeSuffix(".m3u").removeSuffix(".m3u8")
            showFileDialog = true
        }
    }

    // Handle feedback & errors
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("playlists_screen_root")
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 380.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    TVSectionTitle(
                        title = stringResource(R.string.nav_playlists),
                        badgeText = "${playlists.size} listas"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TVButton(
                            text = stringResource(R.string.action_import_m3u_file),
                            icon = Icons.Default.FileOpen,
                            onClick = {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            },
                            isPrimary = true,
                            testTag = "btn_import_file"
                        )

                        TVButton(
                            text = stringResource(R.string.action_import_m3u_url),
                            icon = Icons.Default.Link,
                            onClick = { showUrlDialog = true },
                            isPrimary = false,
                            testTag = "btn_import_url"
                        )
                    }
                }
            }

            // Empty state if zero playlists
            if (playlists.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    TVEmptyState(
                        title = stringResource(R.string.empty_playlists),
                        description = stringResource(R.string.empty_playlists_desc),
                        icon = Icons.Default.PlaylistPlay,
                        actionText = stringResource(R.string.action_import_m3u_file),
                        onActionClick = {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        }
                    )
                }
            } else {
                itemsIndexed(playlists, key = { _, item -> item.id }) { index, playlist ->
                    PlaylistTVCard(
                        playlist = playlist,
                        isFirst = index == 0,
                        isLast = index == playlists.lastIndex,
                        onOpenChannels = onOpenChannels?.let { cb -> { cb(playlist) } },
                        onMoveUp = { viewModel.reorderPlaylist(playlist.id, moveUp = true) },
                        onMoveDown = { viewModel.reorderPlaylist(playlist.id, moveUp = false) },
                        onRefresh = { viewModel.refreshRemotePlaylist(playlist.id) },
                        onRename = { renameTargetPlaylist = playlist },
                        onDuplicate = { viewModel.duplicatePlaylist(playlist.id) },
                        onDelete = { deleteTargetPlaylist = playlist }
                    )
                }
            }
        }

        // Snackbar host for feedback
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )

        // Loading / Import Progress Overlay
        if (isImporting) {
            ImportProgressOverlay(
                statusMessage = importStatusMessage,
                count = importProgress
            )
        }
    }

    // Dialog: Add Choice (Local File or Remote URL)
    if (showAddChoiceDialog) {
        AddChoicePlaylistDialog(
            onDismiss = { showAddChoiceDialog = false },
            onChooseFile = {
                showAddChoiceDialog = false
                filePickerLauncher.launch(arrayOf("*/*"))
            },
            onChooseUrl = {
                showAddChoiceDialog = false
                showUrlDialog = true
            }
        )
    }

    // Dialog: Add by URL
    if (showUrlDialog) {
        AddUrlPlaylistDialog(
            onDismiss = { showUrlDialog = false },
            onConfirm = { name, url ->
                showUrlDialog = false
                viewModel.importRemotePlaylist(name, url)
            }
        )
    }

    // Dialog: Add Local File Confirmation
    if (showFileDialog && selectedFileUri != null) {
        AddFilePlaylistDialog(
            initialName = selectedFileName,
            onDismiss = {
                showFileDialog = false
                selectedFileUri = null
            },
            onConfirm = { customName ->
                val uri = selectedFileUri
                showFileDialog = false
                selectedFileUri = null
                if (uri != null) {
                    viewModel.importLocalPlaylist(
                        name = customName,
                        uri = uri,
                        contentResolver = context.contentResolver
                    )
                }
            }
        )
    }

    // Dialog: Rename Playlist
    renameTargetPlaylist?.let { playlist ->
        RenamePlaylistDialog(
            initialName = playlist.name,
            onDismiss = { renameTargetPlaylist = null },
            onConfirm = { newName ->
                renameTargetPlaylist = null
                viewModel.renamePlaylist(playlist.id, newName)
            }
        )
    }

    // Dialog: Confirm Delete
    deleteTargetPlaylist?.let { playlist ->
        DeleteConfirmDialog(
            playlistName = playlist.name,
            onDismiss = { deleteTargetPlaylist = null },
            onConfirm = {
                deleteTargetPlaylist = null
                viewModel.deletePlaylist(playlist.id)
            }
        )
    }
}

/**
 * TV-optimized Playlist Card with large typography, badges, and remote-friendly action bar.
 */
@Composable
fun PlaylistTVCard(
    playlist: Playlist,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRefresh: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenChannels: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor = when {
        isFocused -> BorderFocused
        isHovered -> BorderFocused.copy(alpha = 0.6f)
        else -> BorderSubtle
    }

    val backgroundColor = when {
        isFocused -> SurfaceCardFocused
        isHovered -> SurfaceCardHover
        else -> SurfaceCard
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("playlist_card_${playlist.id}")
            .pointerHoverIcon(PointerIcon.Hand)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .focusable(interactionSource = interactionSource),
        color = backgroundColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Type Icon + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (playlist.type == PlaylistType.URL) {
                        Color(0xFF7C4DFF).copy(alpha = 0.2f)
                    } else {
                        PrimaryElectricCyan.copy(alpha = 0.2f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (playlist.type == PlaylistType.URL) Color(0xFF7C4DFF) else PrimaryElectricCyan
                    ),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (playlist.type == PlaylistType.URL) Icons.Default.Link else Icons.Default.FileOpen,
                            contentDescription = null,
                            tint = if (playlist.type == PlaylistType.URL) Color(0xFFB388FF) else PrimaryElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (playlist.type == PlaylistType.URL) {
                            playlist.url ?: playlist.source
                        } else {
                            playlist.source.ifBlank { "Ficheiro local importado" }
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Badges Row: Channel Count, Type, Last Sync
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Channels badge
                PlaylistBadge(
                    text = stringResource(R.string.playlist_channels_count, playlist.channelCount),
                    icon = Icons.Default.Tv,
                    color = PrimaryElectricCyan
                )

                // Type badge
                PlaylistBadge(
                    text = if (playlist.type == PlaylistType.URL) {
                        stringResource(R.string.playlist_type_url)
                    } else {
                        stringResource(R.string.playlist_type_local)
                    },
                    icon = if (playlist.type == PlaylistType.URL) Icons.Default.Link else Icons.Default.FileOpen,
                    color = if (playlist.type == PlaylistType.URL) Color(0xFFB388FF) else Color(0xFF81C784)
                )

                // Time ago
                val timeLabel = formatTimeAgo(playlist.lastSync ?: playlist.createdAt)
                PlaylistBadge(
                    text = timeLabel,
                    icon = Icons.Default.Sync,
                    color = Color(0xFF90A4AE)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row (Touch & D-pad friendly)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onOpenChannels != null) {
                    TVButton(
                        text = stringResource(R.string.action_open_playlist),
                        icon = Icons.Default.PlayArrow,
                        onClick = onOpenChannels,
                        isPrimary = true,
                        testTag = "btn_open_channels_${playlist.id}"
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // Move Up
                    CardActionButton(
                    icon = Icons.Default.ArrowUpward,
                    contentDescription = stringResource(R.string.action_move_up),
                    enabled = !isFirst,
                    onClick = onMoveUp,
                    testTag = "btn_move_up_${playlist.id}"
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Move Down
                CardActionButton(
                    icon = Icons.Default.ArrowDownward,
                    contentDescription = stringResource(R.string.action_move_down),
                    enabled = !isLast,
                    onClick = onMoveDown,
                    testTag = "btn_move_down_${playlist.id}"
                )

                // Refresh (only for remote URL playlists)
                if (playlist.type == PlaylistType.URL) {
                    Spacer(modifier = Modifier.width(6.dp))
                    CardActionButton(
                        icon = Icons.Default.Refresh,
                        contentDescription = "Atualizar canais",
                        enabled = true,
                        onClick = onRefresh,
                        testTag = "btn_refresh_${playlist.id}"
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Duplicate
                CardActionButton(
                    icon = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.action_duplicate),
                    enabled = true,
                    onClick = onDuplicate,
                    testTag = "btn_duplicate_${playlist.id}"
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Rename
                CardActionButton(
                    icon = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.action_rename),
                    enabled = true,
                    onClick = onRename,
                    testTag = "btn_rename_${playlist.id}"
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Delete
                CardActionButton(
                    icon = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.dialog_delete_playlist_title),
                    enabled = true,
                    isDestructive = true,
                    onClick = onDelete,
                    testTag = "btn_delete_${playlist.id}"
                )
            }
        }
    }
}
}

@Composable
fun CardActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    testTag: String = "card_action"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val tint = when {
        !enabled -> Color(0xFF455A64)
        isDestructive -> ErrorRed
        isFocused -> PrimaryElectricCyan
        isHovered -> Color.White
        else -> Color(0xFFB0BEC5)
    }

    val bg = when {
        !enabled -> Color.Transparent
        isFocused -> PrimaryElectricCyan.copy(alpha = 0.2f)
        isHovered -> SurfaceCardHover
        else -> Color(0xFF1E2638)
    }

    Surface(
        modifier = modifier
            .testTag(testTag)
            .size(36.dp)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .border(
                1.dp,
                if (isFocused) BorderFocused else BorderSubtle,
                RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .focusable(enabled = enabled, interactionSource = interactionSource),
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PlaylistBadge(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = color
                )
            )
        }
    }
}

/**
 * Dialog to choose how to add a playlist (File or URL).
 */
@Composable
fun AddChoicePlaylistDialog(
    onDismiss: () -> Unit,
    onChooseFile: () -> Unit,
    onChooseUrl: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF131824),
            border = BorderStroke(1.dp, Color(0xFF263238)),
            modifier = Modifier
                .width(480.dp)
                .padding(16.dp)
                .testTag("dialog_add_choice")
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Adicionar Playlist",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Escolha como pretende importar a sua lista de canais M3U / M3U8 para o projetor:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(20.dp))

                TVButton(
                    text = stringResource(R.string.action_import_m3u_file),
                    icon = Icons.Default.FileOpen,
                    onClick = onChooseFile,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_choice_import_file"
                )

                Spacer(modifier = Modifier.height(12.dp))

                TVButton(
                    text = stringResource(R.string.action_import_m3u_url),
                    icon = Icons.Default.Link,
                    onClick = onChooseUrl,
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_choice_import_url"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TVButton(
                        text = stringResource(R.string.dialog_action_cancel),
                        onClick = onDismiss,
                        isPrimary = false,
                        testTag = "btn_choice_cancel"
                    )
                }
            }
        }
    }
}

/**
 * Dialog to import M3U via Remote URL.
 */
@Composable
fun AddUrlPlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF131824),
            border = BorderStroke(1.dp, Color(0xFF263238)),
            modifier = Modifier
                .width(480.dp)
                .padding(16.dp)
                .testTag("dialog_add_url")
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_add_url_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.dialog_add_playlist_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF90A4AE))
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_playlist_name)) },
                    placeholder = { Text("Ex.: Minha Lista IPTV") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_playlist_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryElectricCyan,
                        unfocusedBorderColor = Color(0xFF37474F),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // URL field
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = null
                    },
                    label = { Text(stringResource(R.string.field_playlist_url)) },
                    placeholder = { Text(stringResource(R.string.field_playlist_url_hint)) },
                    isError = urlError != null,
                    supportingText = urlError?.let { { Text(it, color = ErrorRed) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_playlist_url"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryElectricCyan,
                        unfocusedBorderColor = Color(0xFF37474F),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB0BEC5)),
                        modifier = Modifier.testTag("dialog_url_cancel")
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val cleanUrl = url.trim()
                            if (cleanUrl.isBlank() || (!cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true))) {
                                urlError = "Introduza um URL válido (http:// ou https://)"
                            } else {
                                onConfirm(name.ifBlank { "Playlist Remota" }, cleanUrl)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryElectricCyan,
                            contentColor = Color(0xFF0A0E1A)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("dialog_url_confirm")
                    ) {
                        Text(stringResource(R.string.action_add_playlist), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog to confirm importing a selected local file.
 */
@Composable
fun AddFilePlaylistDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF131824),
            border = BorderStroke(1.dp, Color(0xFF263238)),
            modifier = Modifier
                .width(460.dp)
                .padding(16.dp)
                .testTag("dialog_add_file")
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_add_file_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Ficheiro M3U/M3U8 selecionado. Confirme ou altere o nome da lista:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF90A4AE))
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_playlist_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_file_playlist_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryElectricCyan,
                        unfocusedBorderColor = Color(0xFF37474F),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB0BEC5))
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { onConfirm(name.ifBlank { initialName }) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryElectricCyan,
                            contentColor = Color(0xFF0A0E1A)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("dialog_file_confirm")
                    ) {
                        Text(stringResource(R.string.action_import_m3u_file), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog to rename a playlist.
 */
@Composable
fun RenamePlaylistDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF131824),
            border = BorderStroke(1.dp, Color(0xFF263238)),
            modifier = Modifier
                .width(440.dp)
                .padding(16.dp)
                .testTag("dialog_rename")
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_rename_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_playlist_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_rename_playlist"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryElectricCyan,
                        unfocusedBorderColor = Color(0xFF37474F),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB0BEC5))
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (name.isNotBlank()) onConfirm(name.trim())
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryElectricCyan,
                            contentColor = Color(0xFF0A0E1A)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("dialog_rename_confirm")
                    ) {
                        Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Confirmation dialog to delete a playlist.
 */
@Composable
fun DeleteConfirmDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_delete_playlist_title),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_delete_playlist_message, playlistName),
                color = Color(0xFFCFD8DC)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("dialog_delete_confirm")
            ) {
                Text(stringResource(R.string.action_delete), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB0BEC5))
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        containerColor = Color(0xFF131824),
        modifier = Modifier.testTag("dialog_delete_playlist")
    )
}

/**
 * Background parsing progress overlay.
 */
@Composable
fun ImportProgressOverlay(
    statusMessage: String,
    count: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD0A0E1A))
            .clickable(enabled = false) {}, // Intercept clicks
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF131824),
            border = BorderStroke(1.5.dp, PrimaryElectricCyan),
            modifier = Modifier
                .width(420.dp)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = PrimaryElectricCyan,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(54.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.playlist_import_progress_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = PrimaryElectricCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.playlist_importing_desc),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF90A4AE)
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diffMs = System.currentTimeMillis() - timestamp
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        minutes < 1 -> "Atualizada agora"
        minutes < 60 -> "Atualizada há $minutes min"
        hours < 24 -> "Atualizada há $hours h"
        else -> "Atualizada há $days d"
    }
}
