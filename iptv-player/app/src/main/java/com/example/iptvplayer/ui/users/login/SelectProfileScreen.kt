package com.example.iptvplayer.ui.users.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.iptvplayer.domain.model.User
import com.example.iptvplayer.ui.users.UserViewModel
import com.example.iptvplayer.ui.users.components.CreateOrEditUserDialog
import com.example.iptvplayer.ui.users.components.PinEntryDialog
import kotlinx.coroutines.launch

@Composable
fun SelectProfileScreen(
    viewModel: UserViewModel,
    canGoBack: Boolean,
    onNavigateBack: () -> Unit,
    onLoginSuccess: (User) -> Unit
) {
    val users by viewModel.users.collectAsState()
    val selectedUserForPin by viewModel.selectedUserForPin.collectAsState()
    val pinInput by viewModel.pinInput.collectAsState()
    val pinError by viewModel.pinError.collectAsState()
    val feedbackMessage by viewModel.actionFeedback.collectAsState()

    var isCreatingFirstUser by remember { mutableStateOf(false) }
    var firstUserFormError by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    // BackHandler: if in first user creation form, remote Back returns to initial empty state
    BackHandler(enabled = users.isEmpty() && isCreatingFirstUser) {
        isCreatingFirstUser = false
        firstUserFormError = null
    }

    // Main 16:9 Projector Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E1A),
                        Color(0xFF0D1424),
                        Color(0xFF070A12)
                    )
                )
            )
            .testTag("select_profile_screen")
    ) {
        if (users.isEmpty()) {
            // ZERO USERS STATE: Dedicated first-user onboarding flow
            if (!isCreatingFirstUser) {
                ZeroUsersWelcomeView(
                    onCreateUserClick = {
                        firstUserFormError = null
                        isCreatingFirstUser = true
                    }
                )
            } else {
                FirstUserCreationFormView(
                    onBack = {
                        isCreatingFirstUser = false
                        firstUserFormError = null
                    },
                    validationError = firstUserFormError,
                    onClearError = { firstUserFormError = null },
                    onSubmitUser = { username, pin ->
                        viewModel.createUser(
                            username = username,
                            displayName = null,
                            pin = pin,
                            avatarColor = 0xFF00E5FF,
                            onSuccess = { newUser ->
                                isCreatingFirstUser = false
                                firstUserFormError = null
                                scope.launch {
                                    snackbarHostState.showSnackbar("Utilizador criado com sucesso.")
                                }
                                onLoginSuccess(newUser)
                            },
                            onError = { err ->
                                firstUserFormError = err
                            }
                        )
                    }
                )
            }
        } else {
            // MULTI-USER STATE: Existing profile selection screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar with branding & back action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF00E5FF), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }

                    if (canGoBack) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .height(40.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .testTag("button_back_to_home"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB0BEC5)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_back),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.4f))

                // Main Title & Subtitle
                Text(
                    text = stringResource(R.string.profile_select_title),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 34.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.profile_select_subtitle),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFF90A4AE),
                        fontSize = 16.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profiles_row")
                ) {
                    items(users, key = { it.id }) { user ->
                        ProfileCard(
                            user = user,
                            onClick = {
                                viewModel.selectUserToLogin(user) { loggedUser ->
                                    onLoginSuccess(loggedUser)
                                }
                            }
                        )
                    }

                    // Add Profile Card
                    item {
                        AddProfileCard(
                            onClick = { showCreateDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.6f))
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // PIN Entry Dialog
        if (selectedUserForPin != null) {
            PinEntryDialog(
                user = selectedUserForPin!!,
                pinInput = pinInput,
                pinError = pinError,
                onDigitClick = { digit -> viewModel.onPinDigitEntered(digit) },
                onBackspace = { viewModel.onPinBackspace() },
                onClear = { viewModel.onPinClear() },
                onPinTextChange = { text -> viewModel.setPinInput(text) },
                onSubmit = {
                    viewModel.submitPin { loggedInUser ->
                        onLoginSuccess(loggedInUser)
                    }
                },
                onDismiss = { viewModel.dismissPinDialog() }
            )
        }

        // Create User Dialog (for existing users wanting to add another profile)
        if (showCreateDialog) {
            CreateOrEditUserDialog(
                userToEdit = null,
                onSave = { username, displayName, pin, avatarColor ->
                    viewModel.createUser(
                        username = username,
                        displayName = displayName,
                        pin = pin,
                        avatarColor = avatarColor,
                        onSuccess = { newUser ->
                            showCreateDialog = false
                            onLoginSuccess(newUser)
                        },
                        onError = { err ->
                            scope.launch { snackbarHostState.showSnackbar(err) }
                        }
                    )
                },
                onDismiss = { showCreateDialog = false }
            )
        }
    }
}

/**
 * Clean initial empty-state screen when zero users exist in the database.
 * Structure:
 * IPTV Player
 * “Bem-vindo ao IPTV Player”
 * “Crie o primeiro utilizador para começar.”
 * [ Criar utilizador ]
 */
@Composable
private fun ZeroUsersWelcomeView(
    onCreateUserClick: () -> Unit
) {
    val createBtnFocusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = isFocused || isHovered

    LaunchedEffect(Unit) {
        try {
            createBtnFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Branding: Icon & Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFF00E5FF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = Color(0xFF0A0E1A),
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = 1.2.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title: “Bem-vindo ao IPTV Player”
        Text(
            text = stringResource(R.string.welcome_iptv_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 38.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Subtitle: “Crie o primeiro utilizador para começar.”
        Text(
            text = stringResource(R.string.welcome_first_user_subtitle),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color(0xFF94A3B8),
                fontSize = 18.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(44.dp))

        // Primary Button: “Criar utilizador”
        Button(
            onClick = onCreateUserClick,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E5FF),
                contentColor = Color(0xFF0A0E1A)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .widthIn(min = 280.dp)
                .height(58.dp)
                .scale(if (isHighlighted) 1.08f else 1.0f)
                .focusRequester(createBtnFocusRequester)
                .border(
                    width = if (isFocused) 3.5.dp else if (isHovered) 2.dp else 0.dp,
                    color = if (isFocused) Color.White else Color(0xFF00E5FF),
                    shape = RoundedCornerShape(14.dp)
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .testTag("button_create_user_primary")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.action_create_user),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }
    }
}

/**
 * Dedicated first user creation form.
 * Fields:
 * - Nome do utilizador
 * - PIN ou palavra-passe
 * - Confirmar PIN ou palavra-passe
 *
 * Logical D-pad focus order:
 * Nome -> PIN -> Confirmar PIN -> Criar utilizador -> Voltar
 */
@Composable
private fun FirstUserCreationFormView(
    onBack: () -> Unit,
    onSubmitUser: (username: String, pin: String) -> Unit,
    validationError: String?,
    onClearError: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val nameFocusRequester = remember { FocusRequester() }
    val pinFocusRequester = remember { FocusRequester() }
    val confirmPinFocusRequester = remember { FocusRequester() }
    val submitFocusRequester = remember { FocusRequester() }
    val backFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            nameFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val submitInteraction = remember { MutableInteractionSource() }
    val isSubmitFocused by submitInteraction.collectIsFocusedAsState()
    val isSubmitHovered by submitInteraction.collectIsHoveredAsState()
    val isSubmitHighlighted = isSubmitFocused || isSubmitHovered

    val backInteraction = remember { MutableInteractionSource() }
    val isBackFocused by backInteraction.collectIsFocusedAsState()
    val isBackHovered by backInteraction.collectIsHoveredAsState()
    val isBackHighlighted = isBackFocused || isBackHovered

    val displayError = localError ?: validationError

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF00E5FF), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = Color(0xFF0A0E1A),
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 16:9 Projector Form Card
        Surface(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(18.dp))
                .border(1.5.dp, Color(0xFF27354A), RoundedCornerShape(18.dp))
                .testTag("first_user_creation_form"),
            color = Color(0xFF131824),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.action_create_user),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 24.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Configure o perfil principal para começar a utilizar o IPTV Player",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Field 1: Nome do utilizador
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        localError = null
                        onClearError()
                    },
                    label = { Text(stringResource(R.string.field_first_user_name)) },
                    placeholder = { Text("Ex: Utilizador Principal", color = Color(0xFF64748B)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { pinFocusRequester.requestFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF37474F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocusRequester)
                        .focusProperties {
                            next = pinFocusRequester
                            down = pinFocusRequester
                        }
                        .testTag("input_first_user_name")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Field 2: PIN ou palavra-passe
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it
                        localError = null
                        onClearError()
                    },
                    label = { Text(stringResource(R.string.field_first_user_pin)) },
                    placeholder = { Text("4 a 8 dígitos ou caracteres", color = Color(0xFF64748B)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { confirmPinFocusRequester.requestFocus() }),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF37474F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(pinFocusRequester)
                        .focusProperties {
                            up = nameFocusRequester
                            next = confirmPinFocusRequester
                            down = confirmPinFocusRequester
                        }
                        .testTag("input_first_user_pin")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Field 3: Confirmar PIN ou palavra-passe
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        confirmPin = it
                        localError = null
                        onClearError()
                    },
                    label = { Text(stringResource(R.string.field_first_user_pin_confirm)) },
                    placeholder = { Text("Repita o PIN ou palavra-passe", color = Color(0xFF64748B)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        submitFocusRequester.requestFocus()
                    }),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF37474F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(confirmPinFocusRequester)
                        .focusProperties {
                            up = pinFocusRequester
                            next = submitFocusRequester
                            down = submitFocusRequester
                        }
                        .testTag("input_first_user_pin_confirm")
                )

                // Error message banner
                if (!displayError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFD32F2F).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFD32F2F), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("first_user_error_banner"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayError,
                            color = Color(0xFFFF8A80),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Button: “Criar utilizador”
                Button(
                    onClick = {
                        val cleanUsername = username.trim()
                        if (cleanUsername.isEmpty()) {
                            localError = "O nome do utilizador não pode estar vazio"
                            nameFocusRequester.requestFocus()
                            return@Button
                        }
                        if (pin.isEmpty()) {
                            localError = "Introduza um PIN ou palavra-passe"
                            pinFocusRequester.requestFocus()
                            return@Button
                        }
                        if (pin.length < 4 || pin.length > 8) {
                            localError = "PIN ou palavra-passe inválido. Introduza entre 4 e 8 dígitos"
                            pinFocusRequester.requestFocus()
                            return@Button
                        }
                        if (pin != confirmPin) {
                            localError = "O PIN ou palavra-passe não coincide"
                            confirmPinFocusRequester.requestFocus()
                            return@Button
                        }

                        onSubmitUser(cleanUsername, pin)
                    },
                    interactionSource = submitInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color(0xFF0A0E1A)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .scale(if (isSubmitHighlighted) 1.04f else 1.0f)
                        .focusRequester(submitFocusRequester)
                        .focusProperties {
                            up = confirmPinFocusRequester
                            next = backFocusRequester
                            down = backFocusRequester
                        }
                        .border(
                            width = if (isSubmitFocused) 3.dp else if (isSubmitHovered) 2.dp else 0.dp,
                            color = if (isSubmitFocused) Color.White else Color(0xFF00E5FF),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .pointerHoverIcon(PointerIcon.Hand)
                        .testTag("button_submit_first_user")
                ) {
                    Text(
                        text = stringResource(R.string.action_create_user),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action: “Voltar”
                OutlinedButton(
                    onClick = onBack,
                    interactionSource = backInteraction,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB0BEC5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .scale(if (isBackHighlighted) 1.03f else 1.0f)
                        .focusRequester(backFocusRequester)
                        .focusProperties {
                            up = submitFocusRequester
                        }
                        .border(
                            width = if (isBackFocused) 2.5.dp else if (isBackHovered) 1.5.dp else 1.dp,
                            color = if (isBackFocused) Color(0xFF00E5FF) else if (isBackHovered) Color.White else Color(0xFF37474F),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .pointerHoverIcon(PointerIcon.Hand)
                        .testTag("button_back_first_user")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_back),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    user: User,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val isHighlighted = isFocused || isHovered

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(160.dp)
            .scale(if (isHighlighted) 1.08f else 1.0f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isHighlighted) Color(0xFF1B2333) else Color(0xFF131824)
            )
            .border(
                width = if (isFocused) 3.5.dp else if (isHovered) 2.dp else 1.dp,
                color = if (isFocused) Color(0xFF00E5FF) else if (isHovered) Color(0xFF00B0FF) else Color(0xFF263238),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(18.dp)
            .testTag("profile_card_${user.username}")
    ) {
        // Avatar Circle
        Box(
            modifier = Modifier
                .size(82.dp)
                .clip(CircleShape)
                .background(Color(user.avatarColor))
                .border(
                    width = if (isHighlighted) 2.5.dp else 1.5.dp,
                    color = if (isHighlighted) Color.White else Color.White.copy(alpha = 0.4f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            val initial = user.displayTitle.firstOrNull()?.uppercase() ?: "U"
            Text(
                text = initial,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    fontSize = 36.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Profile Display Name
        Text(
            text = user.displayTitle,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) Color(0xFF00E5FF) else Color.White,
                fontSize = 17.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // PIN protected indicator pill
        if (user.isPinProtected) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(Color(0xFF263238), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.pin_protected_label),
                    tint = Color(0xFFFFD600),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "PIN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFFFD600),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        } else {
            Text(
                text = "Sem PIN",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF78909C),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun AddProfileCard(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = isFocused || isHovered

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(160.dp)
            .scale(if (isHighlighted) 1.08f else 1.0f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isHighlighted) Color(0xFF1B2333) else Color(0xFF101522))
            .border(
                width = if (isFocused) 3.5.dp else if (isHovered) 2.dp else 1.dp,
                color = if (isFocused) Color(0xFF00E5FF) else if (isHovered) Color(0xFF00B0FF) else Color(0xFF37474F),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(18.dp)
            .testTag("add_profile_card")
    ) {
        Box(
            modifier = Modifier
                .size(82.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E283A))
                .border(
                    width = 2.dp,
                    color = if (isHighlighted) Color(0xFF00E5FF) else Color(0xFF546E7A),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.action_add_user),
                tint = if (isHighlighted) Color(0xFF00E5FF) else Color(0xFFB0BEC5),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.action_add_user),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) Color(0xFF00E5FF) else Color(0xFFB0BEC5),
                fontSize = 16.sp
            ),
            maxLines = 1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Novo perfil",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF78909C),
                fontSize = 11.sp
            )
        )
    }
}
