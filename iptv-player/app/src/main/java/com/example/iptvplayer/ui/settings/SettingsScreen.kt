package com.example.iptvplayer.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.iptvplayer.ui.components.TVCard
import com.example.iptvplayer.ui.components.TVSectionTitle
import com.example.ui.theme.PrimaryElectricCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_root")
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TVSectionTitle(
                title = stringResource(R.string.nav_settings),
                badgeText = "Sistema"
            )
        }

        // Setting 1: Screen & Projection
        item {
            SettingCard(
                icon = Icons.Default.DisplaySettings,
                title = stringResource(R.string.settings_display_title),
                description = stringResource(R.string.settings_display_desc),
                statusText = "16:9 Ativo"
            )
        }

        // Setting 2: Performance
        item {
            SettingCard(
                icon = Icons.Default.Memory,
                title = stringResource(R.string.settings_performance_title),
                description = stringResource(R.string.settings_performance_desc),
                statusText = "Modo Económico"
            )
        }

        // Setting 3: Remote & Mouse
        item {
            SettingCard(
                icon = Icons.Default.Mouse,
                title = stringResource(R.string.settings_remote_title),
                description = stringResource(R.string.settings_remote_desc),
                statusText = "D-Pad + Rato"
            )
        }

        // Setting 4: Language
        item {
            SettingCard(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_language_title),
                description = stringResource(R.string.settings_language_desc),
                statusText = "PT (AO)"
            )
        }

        // Setting 5: About
        item {
            SettingCard(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_about_title),
                description = stringResource(R.string.settings_version),
                statusText = "v1.0.0"
            )
        }
    }
}

@Composable
private fun SettingCard(
    icon: ImageVector,
    title: String,
    description: String,
    statusText: String
) {
    TVCard(
        onClick = { /* Setting toggle */ },
        modifier = Modifier.fillMaxWidth(),
        testTag = "setting_card_${title.take(10)}"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryElectricCyan,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryElectricCyan,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
