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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruizurraca.luziatestdavid.presentation.model.AssistantStreamState
import com.ruizurraca.luziatestdavid.presentation.model.ChatMessageUiModel
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme

@Composable
fun AssistantMessageBubble(
    model: ChatMessageUiModel.Assistant,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
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
            AssistantBubbleContent(model = model, onRetry = onRetry)
        }
    }
}

@Composable
private fun AssistantBubbleContent(
    model: ChatMessageUiModel.Assistant,
    onRetry: (() -> Unit)?
) {
    when (model.streamState) {
        AssistantStreamState.LOADING -> LoadingLines()
        AssistantStreamState.STREAMING,
        AssistantStreamState.RECEIVED -> AssistantText(model.content)
        AssistantStreamState.FAILED -> FailedIndicator(onRetry = onRetry)
    }
}

@Composable
private fun LoadingLines() {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = "Loading response" }
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp))
        ShimmerBox(modifier = Modifier.width(160.dp).height(12.dp))
    }
}

@Composable
private fun AssistantText(content: String) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun FailedIndicator(onRetry: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = "Reply failed",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .size(20.dp)
        )
        if (onRetry != null) {
            IconButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Retry reply",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
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

@Preview(showBackground = true, name = "Light — FAILED without retry")
@Composable
private fun AssistantMessageBubblePreview_FailedNoRetry() {
    LuziaTheme {
        AssistantMessageBubble(
            model = previewAssistant(streamState = AssistantStreamState.FAILED),
            onRetry = null
        )
    }
}

@Preview(showBackground = true, name = "Light — FAILED with retry")
@Composable
private fun AssistantMessageBubblePreview_FailedWithRetry() {
    LuziaTheme {
        AssistantMessageBubble(
            model = previewAssistant(streamState = AssistantStreamState.FAILED),
            onRetry = {}
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark — FAILED with retry"
)
@Composable
private fun AssistantMessageBubblePreview_DarkFailedWithRetry() {
    LuziaTheme {
        AssistantMessageBubble(
            model = previewAssistant(streamState = AssistantStreamState.FAILED),
            onRetry = {}
        )
    }
}
