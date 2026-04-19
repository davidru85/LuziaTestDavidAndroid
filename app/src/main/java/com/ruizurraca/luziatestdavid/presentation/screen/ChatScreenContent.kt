package com.ruizurraca.luziatestdavid.presentation.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruizurraca.luziatestdavid.R
import com.ruizurraca.luziatestdavid.domain.model.Persona
import com.ruizurraca.luziatestdavid.domain.model.PersonaEntry
import com.ruizurraca.luziatestdavid.presentation.component.AssistantMessageBubble
import com.ruizurraca.luziatestdavid.presentation.component.ChatInputBar
import com.ruizurraca.luziatestdavid.presentation.component.ChatTopAppBar
import com.ruizurraca.luziatestdavid.presentation.component.RoleSelectorChips
import com.ruizurraca.luziatestdavid.presentation.component.UserMessageBubble
import com.ruizurraca.luziatestdavid.presentation.model.AssistantStreamState
import com.ruizurraca.luziatestdavid.presentation.model.ChatMessageUiModel
import com.ruizurraca.luziatestdavid.presentation.state.ChatUiState
import com.ruizurraca.luziatestdavid.presentation.state.ProcessingKind
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme

@Composable
fun ChatScreenContent(
    state: ChatUiState,
    selectedPersona: Persona,
    personaEntries: List<PersonaEntry>,
    isRecording: Boolean,
    snackbarHostState: SnackbarHostState,
    onDraftChange: (String) -> Unit,
    onMicTap: () -> Unit,
    onSendTap: () -> Unit,
    onPersonaSelected: (Persona) -> Unit,
    onRetryLastFailure: () -> Unit,
    onConfirmClearConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastFailedAssistantId = remember(state.messages) {
        state.messages.lastOrNull {
            it is ChatMessageUiModel.Assistant && it.streamState == AssistantStreamState.FAILED
        }?.id
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ChatTopAppBar(
                title = stringResource(R.string.app_title),
                isConversationEmpty = state.messages.isEmpty(),
                onConfirmClearConversation = onConfirmClearConversation
            )
        },
        bottomBar = {
            ChatInputBar(
                draft = state.draft,
                isRecording = isRecording,
                isEnabled = state.isInputEnabled(),
                streamingIndicatorLabel = state.streamingIndicatorLabel(),
                onDraftChange = onDraftChange,
                onMicTap = onMicTap,
                onSendTap = onSendTap
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            RoleSelectorChips(
                entries = personaEntries,
                selectedPersona = selectedPersona,
                onPersonaSelected = onPersonaSelected,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
            ) {
                items(items = state.messages, key = { it.id }) { message ->
                    when (message) {
                        is ChatMessageUiModel.User -> UserMessageBubble(model = message)
                        is ChatMessageUiModel.Assistant -> AssistantMessageBubble(
                            model = message,
                            onRetry = if (message.id == lastFailedAssistantId) onRetryLastFailure else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatUiState.streamingIndicatorLabel(): String? = when (this) {
    is ChatUiState.Idle -> null
    is ChatUiState.Listening -> stringResource(R.string.label_recording)
    is ChatUiState.Processing -> when (kind) {
        ProcessingKind.TRANSCRIBING -> stringResource(R.string.label_transcribing)
        ProcessingKind.AWAITING_REPLY -> stringResource(R.string.label_thinking)
    }
    is ChatUiState.Streaming -> null
}

private fun ChatUiState.isInputEnabled(): Boolean =
    this is ChatUiState.Idle || this is ChatUiState.Listening

@Preview(showBackground = true, name = "Light — Idle (empty)")
@Composable
private fun ChatScreenContentPreview_IdleEmpty() {
    LuziaTheme {
        ChatScreenContent(
            state = ChatUiState.Idle(),
            selectedPersona = Persona.STUDENT,
            personaEntries = previewPersonaEntries(),
            isRecording = false,
            snackbarHostState = remember { SnackbarHostState() },
            onDraftChange = {},
            onMicTap = {},
            onSendTap = {},
            onPersonaSelected = {},
            onRetryLastFailure = {},
            onConfirmClearConversation = {}
        )
    }
}

@Preview(showBackground = true, name = "Light — Conversation")
@Composable
private fun ChatScreenContentPreview_Conversation() {
    LuziaTheme {
        ChatScreenContent(
            state = ChatUiState.Idle(
                messages = listOf(
                    ChatMessageUiModel.User(
                        id = "u1",
                        timestamp = 1_000L,
                        content = "¿Cómo funciona la fotosíntesis?",
                        deliveryState = com.ruizurraca.luziatestdavid.presentation.model.UserDeliveryState.SENT
                    ),
                    ChatMessageUiModel.Assistant(
                        id = "a1",
                        timestamp = 2_000L,
                        content = "La fotosíntesis es…",
                        streamState = AssistantStreamState.RECEIVED
                    )
                )
            ),
            selectedPersona = Persona.STUDENT,
            personaEntries = previewPersonaEntries(),
            isRecording = false,
            snackbarHostState = remember { SnackbarHostState() },
            onDraftChange = {},
            onMicTap = {},
            onSendTap = {},
            onPersonaSelected = {},
            onRetryLastFailure = {},
            onConfirmClearConversation = {}
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark — Thinking"
)
@Composable
private fun ChatScreenContentPreview_DarkThinking() {
    LuziaTheme {
        ChatScreenContent(
            state = ChatUiState.Processing(
                messages = listOf(
                    ChatMessageUiModel.User(
                        id = "u1",
                        timestamp = 1_000L,
                        content = "pregunta",
                        deliveryState = com.ruizurraca.luziatestdavid.presentation.model.UserDeliveryState.SENT
                    )
                ),
                draft = "",
                kind = ProcessingKind.AWAITING_REPLY
            ),
            selectedPersona = Persona.STUDENT,
            personaEntries = previewPersonaEntries(),
            isRecording = false,
            snackbarHostState = remember { SnackbarHostState() },
            onDraftChange = {},
            onMicTap = {},
            onSendTap = {},
            onPersonaSelected = {},
            onRetryLastFailure = {},
            onConfirmClearConversation = {}
        )
    }
}

private fun previewPersonaEntries(): List<PersonaEntry> = listOf(
    PersonaEntry(Persona.STUDENT, "Student", "tutor prompt"),
    PersonaEntry(Persona.SCIENTIST, "Scientist", "scientist prompt"),
    PersonaEntry(Persona.ARTIST, "Artist", "artist prompt")
)
