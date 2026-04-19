package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ruizurraca.luziatestdavid.R
import com.ruizurraca.luziatestdavid.presentation.model.ChatMessageUiModel
import com.ruizurraca.luziatestdavid.presentation.model.UserDeliveryState

@Composable
fun UserMessageBubble(
    model: ChatMessageUiModel.User,
    modifier: Modifier = Modifier
) {
    val bubbleAlpha = if (model.deliveryState == UserDeliveryState.SENDING) 0.7f else 1.0f

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = ShapeDefaults.ExtraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .alpha(bubbleAlpha)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = model.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f, fill = false)
                )
                DeliveryStateIndicator(model.deliveryState)
            }
        }
    }
}

@Composable
private fun DeliveryStateIndicator(state: UserDeliveryState) {
    val iconSize = 14.dp
    val tint = MaterialTheme.colorScheme.onPrimaryContainer
    when (state) {
        UserDeliveryState.SENDING -> {
            val sendingDescription = stringResource(R.string.cd_sending_message)
            CircularProgressIndicator(
                modifier = Modifier
                    .size(iconSize)
                    .semantics { contentDescription = sendingDescription },
                strokeWidth = 1.5.dp,
                color = tint.copy(alpha = 0.7f)
            )
        }
        UserDeliveryState.SENT -> Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = stringResource(R.string.cd_message_sent),
            modifier = Modifier.size(iconSize),
            tint = tint.copy(alpha = 0.7f)
        )
        UserDeliveryState.FAILED -> Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = stringResource(R.string.cd_failed_to_send),
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.error
        )
    }
}
