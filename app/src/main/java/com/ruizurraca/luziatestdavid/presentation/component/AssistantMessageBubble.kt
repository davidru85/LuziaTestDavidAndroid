package com.ruizurraca.luziatestdavid.presentation.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruizurraca.luziatestdavid.R
import com.ruizurraca.luziatestdavid.presentation.model.AssistantStreamState
import com.ruizurraca.luziatestdavid.presentation.model.ChatMessageUiModel
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme

@Composable
fun AssistantMessageBubble(
    model: ChatMessageUiModel.Assistant,
    modifier: Modifier = Modifier,
    isRetryable: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = ShapeDefaults.ExtraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            AssistantBubbleContent(model = model, isRetryable = isRetryable)
        }
    }
}

@Composable
private fun AssistantBubbleContent(
    model: ChatMessageUiModel.Assistant,
    isRetryable: Boolean
) {
    when (model.streamState) {
        AssistantStreamState.LOADING -> LoadingLines()
        AssistantStreamState.STREAMING -> AssistantText(
            content = model.content,
            announceChanges = true
        )
        AssistantStreamState.RECEIVED -> AssistantText(
            content = model.content,
            announceChanges = false
        )
        AssistantStreamState.FAILED -> FailedIndicator(isRetryable = isRetryable)
    }
}

@Composable
private fun LoadingLines() {
    val loadingDescription = stringResource(R.string.cd_loading_response)
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            // LOADING + STREAMING are live regions so TalkBack announces "Loading
            // response" when the shimmer appears and the tokens as they stream in.
            // RECEIVED and FAILED are not, to avoid re-announcing history on scroll.
            .semantics {
                contentDescription = loadingDescription
                liveRegion = LiveRegionMode.Polite
            }
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp))
        ShimmerBox(modifier = Modifier.width(160.dp).height(12.dp))
    }
}

@Composable
private fun AssistantText(content: String, announceChanges: Boolean) {
    val baseModifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    val modifier = if (announceChanges) {
        baseModifier.semantics { liveRegion = LiveRegionMode.Polite }
    } else {
        baseModifier
    }
    Text(
        text = content,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun FailedIndicator(isRetryable: Boolean) {
    // Copy varies with retryability: the retryable (latest) failure invites
    // the user to tap retry; older failures show the apologetic variant.
    // Phase 10.6.A moved the retry *button* out of the bubble into a
    // dedicated control rendered beneath the bubble by the caller.
    val messageRes = if (isRetryable) {
        R.string.bubble_failed_latest_message
    } else {
        R.string.bubble_failed_older_message
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = stringResource(R.string.cd_reply_failed),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .size(20.dp)
        )
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

private fun previewAssistant(
    content: String = "",
    streamState: AssistantStreamState
): ChatMessageUiModel.Assistant = ChatMessageUiModel.Assistant(
    id = "preview",
    timestamp = 0L,
    content = content,
    streamState = streamState
)

@Preview(showBackground = true, name = "Light — LOADING")
@Composable
private fun AssistantMessageBubblePreview_Loading() {
    LuziaTheme {
        AssistantMessageBubble(
            model = previewAssistant(streamState = AssistantStreamState.LOADING)
        )
    }
}

@Preview(showBackground = true, name = "Light — RECEIVED")
@Composable
private fun AssistantMessageBubblePreview_Received() {
    LuziaTheme {
        AssistantMessageBubble(
            model = previewAssistant(
                content = "La fotosíntesis es el proceso por el cual las plantas convierten la luz solar en energía.",
                streamState = AssistantStreamState.RECEIVED
            )
        )
    }
}

@Preview(showBackground = true, name = "Light — FAILED non-retryable")
@Composable
private fun AssistantMessageBubblePreview_FailedNonRetryable() {
    LuziaTheme {
        AssistantMessageBubble(
            model = previewAssistant(streamState = AssistantStreamState.FAILED),
            isRetryable = false
        )
    }
}

@Preview(showBackground = true, name = "Light — FAILED retryable")
@Composable
private fun AssistantMessageBubblePreview_FailedRetryable() {
    LuziaTheme {
        AssistantMessageBubble(
            model = previewAssistant(streamState = AssistantStreamState.FAILED),
            isRetryable = true
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark — FAILED retryable"
)
@Composable
private fun AssistantMessageBubblePreview_DarkFailedRetryable() {
    LuziaTheme {
        AssistantMessageBubble(
            model = previewAssistant(streamState = AssistantStreamState.FAILED),
            isRetryable = true
        )
    }
}
