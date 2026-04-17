package com.ruizurraca.luziatestdavid.presentation.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme

@Composable
fun ChatInputBar(
    draft: String,
    isRecording: Boolean,
    isEnabled: Boolean,
    streamingIndicatorLabel: String?,
    onDraftChange: (String) -> Unit,
    onMicTap: () -> Unit,
    onSendTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        StreamingIndicator(
            visible = streamingIndicatorLabel != null,
            label = streamingIndicatorLabel.orEmpty()
        )
        BottomAppBar {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                enabled = isEnabled,
                placeholder = { Text("Type a message or tap the mic…") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .semantics { contentDescription = "Message input" }
            )
            MorphingActionButton(
                hasText = draft.isNotBlank(),
                isRecording = isRecording,
                enabled = isEnabled,
                onMicTap = onMicTap,
                onSendTap = onSendTap
            )
        }
    }
}

@Preview(showBackground = true, name = "Light — Empty")
@Composable
private fun ChatInputBarPreview_Empty() {
    LuziaTheme {
        ChatInputBar(
            draft = "",
            isRecording = false,
            isEnabled = true,
            streamingIndicatorLabel = null,
            onDraftChange = {},
            onMicTap = {},
            onSendTap = {}
        )
    }
}

@Preview(showBackground = true, name = "Light — Typing")
@Composable
private fun ChatInputBarPreview_Typing() {
    LuziaTheme {
        ChatInputBar(
            draft = "¿Cómo funciona la fotosíntesis?",
            isRecording = false,
            isEnabled = true,
            streamingIndicatorLabel = null,
            onDraftChange = {},
            onMicTap = {},
            onSendTap = {}
        )
    }
}

@Preview(showBackground = true, name = "Light — Recording")
@Composable
private fun ChatInputBarPreview_Recording() {
    LuziaTheme {
        ChatInputBar(
            draft = "",
            isRecording = true,
            isEnabled = true,
            streamingIndicatorLabel = "Recording…",
            onDraftChange = {},
            onMicTap = {},
            onSendTap = {}
        )
    }
}

@Preview(showBackground = true, name = "Light — Thinking")
@Composable
private fun ChatInputBarPreview_Thinking() {
    LuziaTheme {
        ChatInputBar(
            draft = "",
            isRecording = false,
            isEnabled = false,
            streamingIndicatorLabel = "Thinking…",
            onDraftChange = {},
            onMicTap = {},
            onSendTap = {}
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark — Empty"
)
@Composable
private fun ChatInputBarPreview_DarkEmpty() {
    LuziaTheme {
        ChatInputBar(
            draft = "",
            isRecording = false,
            isEnabled = true,
            streamingIndicatorLabel = null,
            onDraftChange = {},
            onMicTap = {},
            onSendTap = {}
        )
    }
}
