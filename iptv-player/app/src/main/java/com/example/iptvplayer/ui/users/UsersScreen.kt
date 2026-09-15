package com.example.iptvplayer.ui.users

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.iptvplayer.domain.model.User
import com.example.iptvplayer.ui.components.TVCard
import com.example.iptvplayer.ui.components.TVSectionTitle
import com.example.iptvplayer.ui.users.components.CreateOrEditUserDialog
import com.example.iptvplayer.ui.users.components.PinEntryDialog
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryElectricCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UsersScreen(
    viewModel: UserViewModel,
    onNavigateToProfileSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val users by viewModel.users.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val selectedUserForPin by viewModel.selectedUserForPin.collectAsState()
    val pinInput by viewModel.pinInput.collectAsState()
    val pinError by viewModel.pinError.collectAsState()
    val actionFeedback by viewModel.actionFeedback.collectAsState()

    var userToEdit by remember { mutableStateOf<User?>(null) }
    var userToDelete by remember { mutableStateOf<User?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(actionFeedback) {
        actionFeedback?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("users_screen_root")
                .padding(horizontal = 28.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TVSectionTitle(
                        title = stringResource(R.string.nav_users),
                        badgeText = "${users.size} Perfis"
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Switch User Button
                        OutlinedButton(
                            onClick = onNavigateToProfileSelect,
                            modifier = Modifier
                                .height(40.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .testTag("button_switch_user"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PrimaryElectricCyan
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwitchAccount,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_switch_user),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        // Add User Button
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .height(40.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .testTag("button_add_user"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryElectricCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_add_user),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Description Header
            item {
                Text(
                    text = stringResource(R.string.user_manage_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }

            // List of Users
            items(users, key = { it.id }) { user ->
                val isActive = activeUser?.id == user.id

                TVCard(
                    onClick = {
                        if (!isActive) {
                            viewModel.selectUserToLogin(user) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Perfil alterado para ${user.displayTitle}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "user_item_${user.username}"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Avatar and User Details
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(user.avatarColor))
                                    .border(
                                        width = if (isActive) 2.5.dp else 1.dp,
                                        color = if (isActive) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = user.displayTitle.firstOrNull()?.uppercase() ?: "U"
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black
                                    )
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = user.displayTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )

                                    if (isActive) {
                                        Surface(
                                            color = PrimaryElectricCyan.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryElectricCyan)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.active_profile_label),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = PrimaryElectricCyan,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (user.isPinProtected) {
                                        Surface(
                                            color = Color(0xFF263238),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFD600),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = "PIN",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFFFFD600),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                val lastLoginStr = if (user.lastLoginAt != null) {
                                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(user.lastLoginAt))
                                } else {
                                    "Nunca"
                                }

                                Text(
                                    text = "@${user.username} • " + stringResource(R.string.last_login_prefix, lastLoginStr),
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                        }

                        // Actions for this user
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Edit Profile / Change PIN
                            OutlinedButton(
                                onClick = { userToEdit = user },
                                modifier = Modifier
                                    .height(38.dp)
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .testTag("button_edit_user_${user.username}"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.action_edit),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            // Delete Profile (only if more than 1 user exists)
                            if (users.size > 1) {
                                OutlinedButton(
                                    onClick = { userToDelete = user },
                                    modifier = Modifier
                                        .height(38.dp)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .testTag("button_delete_user_${user.username}"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFFF8A80)
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.action_delete),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Architecture Information Card
            item {
                TVCard(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "users_architecture_card"
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Arquitetura Multi-Perfil Isolada",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "As definições, canais favoritos, histórico e permissões são completamente persistidos e segregados por identificador de utilizador no banco de dados SQLite/Room local.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // PIN Dialog for switching profile
        if (selectedUserForPin != null) {
            PinEntryDialog(
                user = selectedUserForPin!!,
                pinInput = pinInput,
                pinError = pinError,
                onDigitClick = { viewModel.onPinDigitEntered(it) },
                onBackspace = { viewModel.onPinBackspace() },
                onClear = { viewModel.onPinClear() },
                onPinTextChange = { viewModel.setPinInput(it) },
                onSubmit = {
                    viewModel.submitPin { loggedInUser ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Sessão iniciada como ${loggedInUser.displayTitle}")
                        }
                    }
                },
                onDismiss = { viewModel.dismissPinDialog() }
            )
        }

        // Create Dialog
        if (showAddDialog) {
            CreateOrEditUserDialog(
                userToEdit = null,
                onSave = { username, displayName, pin, avatarColor ->
                    viewModel.createUser(
                        username = username,
                        displayName = displayName,
                        pin = pin,
                        avatarColor = avatarColor,
                        onSuccess = { newUser ->
                            showAddDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Perfil ${newUser.displayTitle} criado!")
                            }
                        },
                        onError = { err ->
                            scope.launch { snackbarHostState.showSnackbar(err) }
                        }
                    )
                },
                onDismiss = { showAddDialog = false }
            )
        }

        // Edit Dialog
        if (userToEdit != null) {
            CreateOrEditUserDialog(
                userToEdit = userToEdit,
                onSave = { _, displayName, pin, avatarColor ->
                    viewModel.updateUser(
                        userId = userToEdit!!.id,
                        displayName = displayName,
                        newPin = pin,
                        avatarColor = avatarColor,
                        onSuccess = {
                            userToEdit = null
                            scope.launch {
                                snackbarHostState.showSnackbar("Perfil atualizado!")
                            }
                        },
                        onError = { err ->
                            scope.launch { snackbarHostState.showSnackbar(err) }
                        }
                    )
                },
                onDismiss = { userToEdit = null }
            )
        }

        // Delete Confirmation Dialog
        if (userToDelete != null) {
            AlertDialog(
                onDismissRequest = { userToDelete = null },
                title = {
                    Text(
                        text = stringResource(R.string.confirm_delete_user_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.confirm_delete_user_message),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.confirm_delete_user_warning),
                            color = Color(0xFFFF8A80),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = userToDelete!!.id
                            userToDelete = null
                            viewModel.deleteUser(
                                userId = id,
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Utilizador eliminado com sucesso")
                                    }
                                },
                                onError = { err ->
                                    scope.launch { snackbarHostState.showSnackbar(err) }
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("confirm_delete_user_button")
                    ) {
                        Text(stringResource(R.string.action_delete), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { userToDelete = null },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB0BEC5)),
                        modifier = Modifier.testTag("cancel_delete_user_button")
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
                containerColor = Color(0xFF141926)
            )
        }
    }
}
