package com.example.iptvplayer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.iptvplayer.domain.model.StreamStatus
import com.example.ui.theme.BorderFocused
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryElectricCyan
import com.example.ui.theme.PrimarySapphire
import com.example.ui.theme.StatusCheckingYellow
import com.example.ui.theme.StatusLiveRed
import com.example.ui.theme.StatusOfflineRed
import com.example.ui.theme.StatusOnlineGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardFocused
import com.example.ui.theme.SurfaceCardHover
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * TV Focus Card with seamless remote control (D-pad) focus & physical mouse hover support.
 * Designed specifically for 16:9 projection viewing.
 */
@Composable
fun TVCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "tv_card",
    contentPadding: PaddingValues = PaddingValues(16.dp),
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isFocused -> 1.04f
            isHovered -> 1.02f
            else -> 1.0f
        },
        animationSpec = spring(stiffness = 400f),
        label = "card_scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFocused -> SurfaceCardFocused
            isHovered -> SurfaceCardHover
            else -> SurfaceCard
        },
        label = "card_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> BorderFocused
            isHovered -> BorderFocused.copy(alpha = 0.6f)
            else -> BorderSubtle
        },
        label = "card_border"
    )

    Surface(
        modifier = modifier
            .testTag(testTag)
            .scale(scale)
            .pointerHoverIcon(PointerIcon.Hand)
            .border(
                width = if (isFocused) 2.5.dp else if (isHovered) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        color = backgroundColor,
        shape = shape
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

/**
 * TV Button for action prompts with distinct focus and hover indicators.
 */
@Composable
fun TVButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    testTag: String = "tv_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else if (isHovered) 1.02f else 1.0f,
        label = "btn_scale"
    )

    val bgColor = when {
        isFocused -> PrimaryElectricCyan
        isHovered -> if (isPrimary) PrimarySapphire else SurfaceCardHover
        isPrimary -> PrimarySapphire.copy(alpha = 0.85f)
        else -> SurfaceCard
    }

    val contentColor = when {
        isFocused -> Color.Black
        else -> TextPrimary
    }

    val shape = RoundedCornerShape(10.dp)

    Surface(
        modifier = modifier
            .testTag(testTag)
            .defaultMinSize(minHeight = 48.dp)
            .scale(scale)
            .pointerHoverIcon(PointerIcon.Hand)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else BorderSubtle,
                shape = shape
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        color = bgColor,
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Sidebar Navigation Item for the 16:9 TV interface.
 */
@Composable
fun TVNavRailItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "tv_nav_item"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        label = "nav_scale"
    )

    val shape = RoundedCornerShape(12.dp)

    val backgroundColor = when {
        isFocused -> PrimaryElectricCyan.copy(alpha = 0.25f)
        isSelected -> PrimarySapphire.copy(alpha = 0.35f)
        isHovered -> SurfaceCardHover
        else -> Color.Transparent
    }

    val borderColor = when {
        isFocused -> PrimaryElectricCyan
        isSelected -> PrimarySapphire
        isHovered -> BorderSubtle
        else -> Color.Transparent
    }

    val iconColor = when {
        isFocused -> PrimaryElectricCyan
        isSelected -> PrimaryElectricCyan
        isHovered -> TextPrimary
        else -> TextSecondary
    }

    val textColor = when {
        isFocused -> TextPrimary
        isSelected -> TextPrimary
        isHovered -> TextPrimary
        else -> TextSecondary
    }

    Surface(
        modifier = modifier
            .testTag(testTag)
            .scale(scale)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                color = borderColor,
                shape = shape
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        color = backgroundColor,
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selected Pill Indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .background(PrimaryElectricCyan, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

/**
 * Section Title for content carousels and lists.
 */
@Composable
fun TVSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    Row(
        modifier = modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        if (badgeText != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                color = SurfaceCardHover,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Text(
                    text = badgeText,
                    color = PrimaryElectricCyan,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/**
 * TV Empty State Display.
 */
@Composable
fun TVEmptyState(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.Tv,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TVCard(
        onClick = { onActionClick?.invoke() },
        modifier = modifier.fillMaxWidth(),
        testTag = "tv_empty_state_card"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = SurfaceCardHover,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = PrimaryElectricCyan,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                TVButton(
                    text = actionText,
                    onClick = onActionClick,
                    isPrimary = true,
                    testTag = "empty_state_action_button"
                )
            }
        }
    }
}

/**
 * Stream Status Badge (Online, Offline, Checking, Unknown, Live).
 */
@Composable
fun TVStatusBadge(
    status: StreamStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, labelRes) = when (status) {
        StreamStatus.ONLINE -> Triple(StatusOnlineGreen.copy(alpha = 0.2f), StatusOnlineGreen, R.string.status_online)
        StreamStatus.OFFLINE -> Triple(StatusOfflineRed.copy(alpha = 0.2f), StatusOfflineRed, R.string.status_offline)
        StreamStatus.CHECKING -> Triple(StatusCheckingYellow.copy(alpha = 0.2f), StatusCheckingYellow, R.string.status_checking)
        StreamStatus.UNKNOWN -> Triple(SurfaceCardHover, TextMuted, R.string.status_unknown)
    }

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(textColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(labelRes),
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
