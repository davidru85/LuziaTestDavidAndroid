package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Compose UI tests for `ChatInputBar` per DESIGN_SYSTEM.md §Input Bar.
 *
 * The bar composes three leaves:
 *  - `StreamingIndicator` (above, when `streamingIndicatorLabel != null`)
 *  - `OutlinedTextField` (placeholder "Type a message or tap the mic…")
 *  - `MorphingActionButton` (Mic ↔ Send based on `draft.isNotBlank()`)
 *
 * Contract:
 *  - Empty draft + not recording -> Mic affordance ("Record voice message").
 *  - Non-empty draft -> Send affordance ("Send message").
 *  - Recording state -> "Stop recording" affordance.
 *  - Streaming indicator label rendered iff non-null.
 *  - `isEnabled = false` disables both the text field and the action button.
 */
@RunWith(RobolectricTestRunner::class)
class ChatInputBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setBar(
        draft: String = "",
        isRecording: Boolean = false,
        isEnabled: Boolean = true,
        streamingIndicatorLabel: String? = null,
        onDraftChange: (String) -> Unit = {},
        onMicTap: () -> Unit = {},
        onSendTap: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            LuziaTheme {
                ChatInputBar(
                    draft = draft,
                    isRecording = isRecording,
                    isEnabled = isEnabled,
                    streamingIndicatorLabel = streamingIndicatorLabel,
                    onDraftChange = onDraftChange,
                    onMicTap = onMicTap,
                    onSendTap = onSendTap
                )
            }
        }
    }

    // ----- Placeholder + text input ------------------------------------------

    @Test
    fun emptyDraft_showsPlaceholder() {
        setBar(draft = "")

        composeTestRule.onNodeWithText("Type a message or tap the mic…").assertIsDisplayed()
    }

    @Test
    fun typing_invokesOnDraftChange_withTypedText() {
        var captured: String? = null
        setBar(
            draft = "",
            onDraftChange = { captured = it }
        )

        composeTestRule.onNode(hasSetTextAction()).performTextInput("hola")

        assertEquals("hola", captured)
    }

    // ----- Streaming indicator -----------------------------------------------

    @Test
    fun streamingIndicatorLabel_null_hidesIndicator() {
        setBar(streamingIndicatorLabel = null)

        composeTestRule.onNodeWithText("Thinking…").assertDoesNotExist()
        composeTestRule.onNodeWithText("Recording…").assertDoesNotExist()
    }

    @Test
    fun streamingIndicatorLabel_present_showsLabel() {
        setBar(streamingIndicatorLabel = "Thinking…")

        composeTestRule.onNodeWithText("Thinking…").assertIsDisplayed()
    }

    // ----- Morphing action button delegation ---------------------------------

    @Test
    fun emptyDraft_notRecording_showsMicAffordance() {
        setBar(draft = "", isRecording = false)

        composeTestRule.onNodeWithContentDescription("Record voice message").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Send message").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Stop recording").assertDoesNotExist()
    }

    @Test
    fun emptyDraft_recording_showsStopAffordance() {
        setBar(draft = "", isRecording = true)

        composeTestRule.onNodeWithContentDescription("Stop recording").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Record voice message").assertDoesNotExist()
    }

    @Test
    fun nonEmptyDraft_showsSendAffordance() {
        setBar(draft = "hola")

        composeTestRule.onNodeWithContentDescription("Send message").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Record voice message").assertDoesNotExist()
    }

    @Test
    fun micTap_invokesOnMicTap() {
        var micTaps = 0
        setBar(draft = "", onMicTap = { micTaps++ })

        composeTestRule.onNodeWithContentDescription("Record voice message").performClick()

        assertEquals(1, micTaps)
    }

    @Test
    fun sendTap_invokesOnSendTap() {
        var sendTaps = 0
        setBar(draft = "hola", onSendTap = { sendTaps++ })

        composeTestRule.onNodeWithContentDescription("Send message").performClick()

        assertEquals(1, sendTaps)
    }

    // ----- Enabled / disabled -------------------------------------------------

    @Test
    fun disabled_textFieldReportsDisabled() {
        setBar(isEnabled = false)

        composeTestRule.onNode(hasSetTextAction()).assertIsNotEnabled()
    }

    @Test
    fun disabled_actionButtonReportsDisabled() {
        setBar(draft = "", isEnabled = false)

        composeTestRule.onNodeWithContentDescription("Record voice message").assertIsNotEnabled()
    }
}
