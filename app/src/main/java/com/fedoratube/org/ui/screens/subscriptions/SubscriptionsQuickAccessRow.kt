package com.fedoratube.org.ui.screens.subscriptions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fedoratube.org.R
import com.fedoratube.org.data.model.Channel

private val RowHorizontalPadding = 12.dp
private val AvatarSpacing = 12.dp
private val AvatarRowTopPadding = 4.dp

@Composable
internal fun SubscriptionsQuickAccessRow(
    channels: List<Channel>,
    selectedChannelId: String? = null,
    onChannelClick: (Channel) -> Unit,
    onChannelLongClick: ((Channel) -> Unit)? = null,
    onViewAllClick: () -> Unit,
    onMusicClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (channels.any { it.isMusic }) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RowHorizontalPadding),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SubscriptionMusicPill(
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    iconSize = 14.dp,
                    textStyle = MaterialTheme.typography.labelMedium,
                    onClick = onMusicClick,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding =
                    PaddingValues(
                        start = RowHorizontalPadding,
                        end = 8.dp,
                        top = AvatarRowTopPadding,
                    ),
                horizontalArrangement = Arrangement.spacedBy(AvatarSpacing),
            ) {
                items(channels, key = { it.id }) { channel ->
                    ChannelAvatarItem(
                        channel = channel,
                        selected = channel.id == selectedChannelId,
                        onClick = { onChannelClick(channel) },
                        onLongClick = onChannelLongClick?.let { { it(channel) } },
                    )
                }
            }

            // Authentic YouTube pinned "All" text button on the right
            Box(
                modifier =
                    Modifier
                        .padding(top = AvatarRowTopPadding + 14.dp, end = 12.dp, start = 4.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 24.dp),
                            onClick = onViewAllClick,
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.view_all_button_label),
                    style =
                        MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.5.sp,
                            color = Color(0xFF3EA6FF),
                        ),
                )
            }
        }
    }
}
