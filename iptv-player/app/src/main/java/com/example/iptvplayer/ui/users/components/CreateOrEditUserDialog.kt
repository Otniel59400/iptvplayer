package com.example.iptvplayer.ui.users.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.iptvplayer.domain.model.User

private val AVATAR_COLORS = listOf(
    0xFF00E5FF, // Cyan
    0xFF2979FF, // Blue
    0xFFFF6D00, // Orange
    0xFF00E676, // Green
    0xFF7C4DFF, // Purple
    0xFFFFD600, // Amber
    0xFFFF1744  // Magenta
)

@Composable
fun CreateOrEditUserDialog(
    userToEdit: User? = null,
    onSave: (username: String, displayName: String?, pin: String?, avatarColor: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(userToEdit?.username ?: "") }
    var displayName by remember { mutableStateOf(userToEdit?.displayName ?: "") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(userToEdit?.avatarColor ?: AVATAR_COLORS.first()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val isEditing = userToEdit != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(560.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .testTag("create_edit_user_dialog"),
            color = Color(0xFF141926),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(selectedColor), CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Person else Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isEditing) stringResource(R.string.action_edit_user) else stringResource(R.string.action_create_user),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = if (isEditing) "Altere os dados do perfil ou defina um novo PIN" else "Preencha os dados do novo perfil",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF90A4AE))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar color palette selector
                Text(
                    text = "Cor do avatar",
                    style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFB0BEC5)),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AVATAR_COLORS.forEach { colorVal ->
                        val isSelected = selectedColor == colorVal
                        val interactionSource = remember { MutableInteractionSource() }
                        val isFocused by interactionSource.collectIsFocusedAsState()
                        val isHovered by interactionSource.collectIsHoveredAsState()

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .scale(if (isFocused || isSelected) 1.15f else 1f)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (isSelected) 3.dp else if (isFocused) 2.dp else 1.dp,
                                    color = if (isSelected || isFocused) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { selectedColor = colorVal }
                                )
                                .pointerHoverIcon(PointerIcon.Hand)
                                .testTag("color_picker_$colorVal"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Form Fields
                // Username field (disabled if editing)
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        validationError = null
                    },
                    enabled = !isEditing,
                    label = { Text(stringResource(R.string.field_name)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color.Gray,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF37474F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_username")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Display Name field
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.field_display_name)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF37474F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_display_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // PIN field
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter { char -> char.isDigit() }.take(8)
                        validationError = null
                    },
                    label = {
                        Text(
                            if (isEditing) stringResource(R.string.field_new_pin)
                            else stringResource(R.string.field_pin)
                        )
                    },
                    placeholder = { Text("Ex: 1234 (4 a 8 dígitos)", color = Color.Gray) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
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
                        .testTag("input_pin")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Confirm PIN field (only if PIN was entered)
                if (pin.isNotEmpty()) {
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            confirmPin = it.filter { char -> char.isDigit() }.take(8)
                            validationError = null
                        },
                        label = { Text(stringResource(R.string.field_pin_confirm)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
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
                            .testTag("input_pin_confirm")
                    )
                }

                // Error text banner
                if (validationError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFD32F2F).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFD32F2F), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("user_dialog_error"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = validationError!!,
                            color = Color(0xFFFF8A80),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .testTag("button_cancel_user_dialog"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCFD8DC)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            val cleanUser = username.trim()
                            if (cleanUser.isEmpty()) {
                                validationError = "O nome não pode estar vazio"
                                return@Button
                            }
                            if (pin.isNotEmpty()) {
                                if (pin.length < 4 || pin.length > 8) {
                                    validationError = "PIN inválido. Introduza entre 4 e 8 dígitos"
                                    return@Button
                                }
                                if (pin != confirmPin) {
                                    validationError = "Os PINs introduzidos não coincidem"
                                    return@Button
                                }
                            }
                            onSave(
                                cleanUser,
                                displayName.trim().ifBlank { null },
                                pin.ifBlank { null },
                                selectedColor
                            )
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .testTag("button_save_user_dialog"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF0A0E1A)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isEditing) stringResource(R.string.action_save) else stringResource(R.string.action_create_user),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
