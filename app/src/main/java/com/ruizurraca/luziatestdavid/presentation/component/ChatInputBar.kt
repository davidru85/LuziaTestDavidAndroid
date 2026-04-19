package com.ruizurraca.luziatestdavid.presentation.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruizurraca.luziatestdavid.R
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
        // Phase 10.6.C: replaced `BottomAppBar` (fixed 80 dp container height)
        // with Surface + Row so the `OutlinedTextField` can grow with content.
        // `surfaceContainer` + 3 dp tonal elevation match the original
        // BottomAppBar visual. `Alignment.Bottom` keeps the mic / send button
        // anchored to the bottom of the field as it expands — standard
        // messenger UX.
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp
        ) {
            val messageInputDescription = stringResource(R.string.cd_message_input)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    enabled = isEnabled,
                    placeholder = {
                        // Cap the placeholder at a single line — the Spanish
                        // copy "Escribe un mensaje o toca el micro…" wraps on
                        // narrow phones, which would drive the empty field to
                        // 2-line height even before the user types. We want
                        // the bar to be 1 line tall until actual content
                        // forces growth.
                        Text(
                            text = stringResource(R.string.input_placeholder),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    minLines = 1,
                    maxLines = 4,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = messageInputDescription }
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
