package com.ruizurraca.luziatestdavid.presentation.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ruizurraca.luziatestdavid.domain.model.Persona
import com.ruizurraca.luziatestdavid.domain.model.PersonaEntry
import com.ruizurraca.luziatestdavid.presentation.model.AssistantStreamState
import com.ruizurraca.luziatestdavid.presentation.model.ChatMessageUiModel
import com.ruizurraca.luziatestdavid.presentation.model.UserDeliveryState
import com.ruizurraca.luziatestdavid.presentation.state.ChatUiState
import com.ruizurraca.luziatestdavid.presentation.state.ProcessingKind
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Integration tests for `ChatScreenContent`, the stateless Scaffold that composes
 * all leaves (TopAppBar, RoleSelector, message list, InputBar) and routes state/
 * callbacks.
 *
 * Contract:
 *  - Messages from `state.messages` render as bubbles.
 *  - Phase-derived streaming indicator label: Listening -> "Recording…",
 *    Processing(TRANSCRIBING) -> "Transcribing…", Processing(AWAITING_REPLY) ->
 *    "Thinking…", Idle/Streaming -> hidden.
 *  - Persona chip tap invokes `onPersonaSelected` with that persona.
 *  - Retry button on the last FAILED assistant invokes `onRetryLastFailure`.
 *  - Send tap / clear-confirm delegate to their respective callbacks.
 *  - Empty conversation -> DeleteSweep is disabled.
 */
@RunWith(RobolectricTestRunner::class)
class ChatScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val personaEntries = listOf(
        PersonaEntry(Persona.STUDENT, "Student", "tutor prompt"),
        PersonaEntry(Persona.SCIENTIST, "Scientist", "scientist prompt"),
        PersonaEntry(Persona.ARTIST, "Artist", "artist prompt")
    )

    private fun userMsg(
        id: String = "u1",
        content: String = "hola",
        deliveryState: UserDeliveryState = UserDeliveryState.SENT
    ) = ChatMessageUiModel.User(
        id = id,
        timestamp = 1_000L,
        content = content,
        deliveryState = deliveryState
    )

    private fun assistantMsg(
        id: String = "a1",
        content: String = "¡hola!",
        streamState: AssistantStreamState = AssistantStreamState.RECEIVED
    ) = ChatMessageUiModel.Assistant(
        id = id,
        timestamp = 2_000L,
        content = content,
        streamState = streamState
    )

    private fun setContent(
        state: ChatUiState = ChatUiState.Idle(),
        selectedPersona: Persona = Persona.STUDENT,
        isRecording: Boolean = false,
        onDraftChange: (String) -> Unit = {},
        onMicTap: () -> Unit = {},
        onSendTap: () -> Unit = {},
        onPersonaSelected: (Persona) -> Unit = {},
        onRetryLastFailure: () -> Unit = {},
        onConfirmClearConversation: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            LuziaTheme {
                ChatScreenContent(
                    state = state,
                    selectedPersona = selectedPersona,
                    personaEntries = personaEntries,
                    isRecording = isRecording,
                    snackbarHostState = SnackbarHostState(),
                    onDraftChange = onDraftChange,
                    onMicTap = onMicTap,
                    onSendTap = onSendTap,
                    onPersonaSelected = onPersonaSelected,
                    onRetryLastFailure = onRetryLastFailure,
                    onConfirmClearConversation = onConfirmClearConversation
                )
            }
        }
    }

    // ----- Message rendering --------------------------------------------------

    @Test
    fun messages_renderUserAndAssistantBubbles() {
        setContent(
            state = ChatUiState.Idle(
                messages = listOf(
                    userMsg(id = "u1", content = "pregunta 1"),
                    assistantMsg(id = "a1", content = "respuesta 1")
                )
            )
        )

        composeTestRule.onNodeWithText("pregunta 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("respuesta 1").assertIsDisplayed()
    }

    // ----- Phase-derived streaming indicator ---------------------------------

    @Test
    fun processing_AwaitingReply_showsThinkingLabel() {
        setContent(
            state = ChatUiState.Processing(
                messages = emptyList(),
                draft = "",
                kind = ProcessingKind.AWAITING_REPLY
            )
        )

        composeTestRule.onNodeWithText("Thinking…").assertIsDisplayed()
    }

    @Test
    fun processing_Transcribing_showsTranscribingLabel() {
        setContent(
            state = ChatUiState.Processing(
                messages = emptyList(),
                draft = "",
                kind = ProcessingKind.TRANSCRIBING
            )
        )

        composeTestRule.onNodeWithText("Transcribing…").assertIsDisplayed()
    }

    @Test
    fun listening_showsRecordingLabel() {
        setContent(
            state = ChatUiState.Listening(messages = emptyList(), draft = ""),
            isRecording = true
        )

        composeTestRule.onNodeWithText("Recording…").assertIsDisplayed()
    }

    @Test
    fun idle_hidesStreamingLabels() {
        setContent(state = ChatUiState.Idle())

        composeTestRule.onNodeWithText("Thinking…").assertDoesNotExist()
        composeTestRule.onNodeWithText("Recording…").assertDoesNotExist()
        composeTestRule.onNodeWithText("Transcribing…").assertDoesNotExist()
    }

    // ----- Persona selector integration --------------------------------------

    @Test
    fun tappingPersonaChip_invokesOnPersonaSelected() {
        var selected: Persona? = null
        setContent(onPersonaSelected = { selected = it })

        composeTestRule.onNodeWithText("Artist").performClick()

        assertEquals(Persona.ARTIST, selected)
    }

    // ----- Retry integration --------------------------------------------------

    @Test
    fun retryOnLastFailedAssistant_invokesOnRetryLastFailure() {
        var retries = 0
        setContent(
            state = ChatUiState.Idle(
                messages = listOf(
                    userMsg(id = "u1", content = "pregunta"),
                    assistantMsg(id = "a1", streamState = AssistantStreamState.FAILED)
                )
            ),
            onRetryLastFailure = { retries++ }
        )

        composeTestRule.onNodeWithContentDescription("Retry reply").performClick()

        assertEquals(1, retries)
    }

    // ----- Delete-sweep + confirm flow ----------------------------------------

    @Test
    fun emptyConversation_deleteSweepDisabled() {
        setContent(state = ChatUiState.Idle(messages = emptyList()))

        composeTestRule.onNodeWithContentDescription("Clear conversation").assertIsNotEnabled()
    }

    @Test
    fun nonEmptyConversation_confirmClear_invokesCallback() {
        var cleared = 0
        setContent(
            state = ChatUiState.Idle(messages = listOf(userMsg())),
            onConfirmClearConversation = { cleared++ }
        )

        composeTestRule.onNodeWithContentDescription("Clear conversation").performClick()
        composeTestRule.onNodeWithText("Clear").performClick()

        assertEquals(1, cleared)
    }

    // ----- Send tap integration -----------------------------------------------

    @Test
    fun sendTapWithNonEmptyDraft_invokesOnSendTap() {
        var sends = 0
        setContent(
            state = ChatUiState.Idle(draft = "hola"),
            onSendTap = { sends++ }
        )

        composeTestRule.onNodeWithContentDescription("Send message").performClick()

        assertEquals(1, sends)
    }
}
