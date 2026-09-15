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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.iptvplayer.domain.model.User

@Composable
fun PinEntryDialog(
    user: User,
    pinInput: String,
    pinError: String?,
    onDigitClick: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onPinTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .testTag("pin_entry_dialog"),
            color = Color(0xFF141926),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // User Avatar and Lock Header
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(user.avatarColor), CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.enter_pin_title),
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.enter_pin_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Text(
                    text = stringResource(R.string.enter_pin_for_user, user.displayTitle),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFB0BEC5)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Masked PIN dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    val maxDots = 6
                    for (i in 0 until maxDots) {
                        val isFilled = i < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(
                                    if (isFilled) Color(0xFF00E5FF) else Color(0xFF263238),
                                    CircleShape
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isFilled) Color(0xFF00E5FF) else Color(0xFF546E7A),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Error message banner
                if (pinError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFD32F2F).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFD32F2F), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("pin_error_banner"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pinError,
                            color = Color(0xFFFF8A80),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Invisible / fallback keyboard input
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = onPinTextChange,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF37474F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(vertical = 4.dp)
                        .testTag("pin_text_field"),
                    placeholder = { Text("Ou digite o PIN aqui...", color = Color.Gray, fontSize = 13.sp) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Numeric Keypad optimized for D-pad and Mouse
                val keypadRows = listOf(
                    listOf('1', '2', '3'),
                    listOf('4', '5', '6'),
                    listOf('7', '8', '9'),
                    listOf('C', '0', '<')
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    keypadRows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { key ->
                                KeypadButton(
                                    symbol = key,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        when (key) {
                                            'C' -> onClear()
                                            '<' -> onBackspace()
                                            else -> onDigitClick(key)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons: "Entrar" and "Voltar"
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
                            .testTag("pin_cancel_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFCFD8DC)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_back),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = pinInput.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .testTag("pin_submit_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF0A0E1A),
                            disabledContainerColor = Color(0xFF37474F),
                            disabledContentColor = Color(0xFF78909C)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_login),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    symbol: Char,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bg = when {
        isFocused -> Color(0xFF00E5FF)
        isHovered -> Color(0xFF263238)
        else -> Color(0xFF1B2333)
    }
    val contentColor = when {
        isFocused -> Color.Black
        else -> Color.White
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .scale(if (isFocused) 1.05f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color(0xFF37474F),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .testTag("keypad_$symbol"),
        contentAlignment = Alignment.Center
    ) {
        when (symbol) {
            '<' -> Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Apagar",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            'C' -> Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Limpar",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            else -> Text(
                text = symbol.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = 18.sp
                )
            )
        }
    }
}
