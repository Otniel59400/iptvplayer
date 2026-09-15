package com.example.iptvplayer.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.iptvplayer.ui.components.TVButton
import com.example.iptvplayer.ui.components.TVCard
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderFocused
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryElectricCyan
import com.example.ui.theme.StatusLiveRed
import com.example.ui.theme.SurfaceCardHover
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PlayerPreviewScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showChannelOverlay by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("player_preview_root")
            .background(Color.Black)
    ) {
        // Video Viewport Area (Simulated 16:9 Screen)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = PrimaryElectricCyan.copy(alpha = 0.4f),
                modifier = Modifier.size(90.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Arquitetura do Leitor IPTV Fullscreen",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Vídeo em ecrã inteiro. Os controlos ocultam-se automaticamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Top Overlay: Channel Info + Live Edge Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TVButton(
                    text = stringResource(R.string.action_back),
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBackClick,
                    isPrimary = false,
                    testTag = "player_back_btn"
                )
            }

            // Live indicator
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, StatusLiveRed)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(StatusLiveRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.status_live),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Latência: ~1.8s",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // Bottom Bar: Channel List Overlay Button (Bottom-Left)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TVButton(
                text = "Lista de Canais",
                icon = Icons.Default.FormatListBulleted,
                onClick = { showChannelOverlay = !showChannelOverlay },
                isPrimary = true,
                testTag = "player_channel_list_toggle_btn"
            )

            Text(
                text = "O atraso de transmissão em direto depende da fonte de streaming e CDN.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        // Channel Overlay Drawer (When triggered)
        if (showChannelOverlay) {
            Surface(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxSize()
                    .align(Alignment.CenterStart)
                    .background(BackgroundDark.copy(alpha = 0.95f)),
                border = BorderStroke(1.dp, BorderSubtle),
                color = BackgroundDark
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Canais da Playlist",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        TVButton(
                            text = "Fechar",
                            onClick = { showChannelOverlay = false },
                            isPrimary = false,
                            testTag = "close_overlay_btn"
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Troca rápida de canal sem sair do ecrã completo do leitor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
