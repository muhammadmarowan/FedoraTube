package io.github.aedev.flow.ui.screens.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R
import io.github.aedev.flow.data.model.Channel

private val RowHorizontalPadding = 12.dp
private val AvatarSpacing = 12.dp
private val AvatarRowTopPadding = 10.dp

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

        LazyRow(
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
            item(key = "view_all") {
                AllSubscriptionsAvatarItem(onClick = onViewAllClick)
            }
        }
    }
}
