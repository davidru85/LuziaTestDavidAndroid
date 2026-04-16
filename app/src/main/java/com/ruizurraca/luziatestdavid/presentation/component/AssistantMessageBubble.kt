package com.ruizurraca.luziatestdavid.presentation.component

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ruizurraca.luziatestdavid.presentation.model.AssistantStreamState
import com.ruizurraca.luziatestdavid.presentation.model.ChatMessageUiModel

@Composable
fun AssistantMessageBubble(
    model: ChatMessageUiModel.Assistant,
    modifier: Modifier = Modifier
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
            AssistantBubbleContent(model = model)
        }
    }
}

@Composable
private fun AssistantBubbleContent(model: ChatMessageUiModel.Assistant) {
    when (model.streamState) {
        AssistantStreamState.LOADING -> LoadingLines()
        AssistantStreamState.STREAMING,
        AssistantStreamState.RECEIVED -> AssistantText(model.content)
        AssistantStreamState.FAILED -> FailedIndicator()
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
private fun FailedIndicator() {
    Icon(
        imageVector = Icons.Filled.Warning,
        contentDescription = "Reply failed",
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .size(20.dp)
    )
}
