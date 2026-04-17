package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ruizurraca.luziatestdavid.presentation.model.AssistantStreamState
import com.ruizurraca.luziatestdavid.presentation.model.ChatMessageUiModel
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AssistantMessageBubbleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun assistant(
        content: String = "",
        streamState: AssistantStreamState = AssistantStreamState.RECEIVED,
        id: String = "a-1"
    ) = ChatMessageUiModel.Assistant(
        id = id,
        timestamp = 0L,
        content = content,
        streamState = streamState
    )

    private fun setBubble(
        model: ChatMessageUiModel.Assistant,
        onRetry: (() -> Unit)? = null
    ) {
        composeTestRule.setContent {
            LuziaTheme {
                AssistantMessageBubble(model = model, onRetry = onRetry)
            }
        }
    }

    @Test
    fun loading_showsShimmer_hidesContent() {
        setBubble(
            assistant(
                content = "should not appear",
                streamState = AssistantStreamState.LOADING
            )
        )

        composeTestRule.onNodeWithContentDescription("Loading response").assertIsDisplayed()
        composeTestRule.onNodeWithText("should not appear").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Reply failed").assertDoesNotExist()
    }

    @Test
    fun streaming_showsPartialContent_hidesShimmer() {
        setBubble(
            assistant(
                content = "Hola, ¿",
                streamState = AssistantStreamState.STREAMING
            )
        )

        composeTestRule.onNodeWithText("Hola, ¿").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Loading response").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Reply failed").assertDoesNotExist()
    }

    @Test
    fun received_showsFullContent_hidesShimmerAndError() {
        setBubble(
            assistant(
                content = "Hola, ¿qué tal?",
                streamState = AssistantStreamState.RECEIVED
            )
        )

        composeTestRule.onNodeWithText("Hola, ¿qué tal?").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Loading response").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Reply failed").assertDoesNotExist()
    }

    @Test
    fun failed_showsErrorIcon_hidesShimmer() {
        setBubble(
            assistant(
                content = "Hola, ¿",
                streamState = AssistantStreamState.FAILED
            )
        )

        composeTestRule.onNodeWithContentDescription("Reply failed").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Loading response").assertDoesNotExist()
    }

    // ----- Retry button (Phase 5.5.G, MEMORY.md Fork 2) -----------------------

    @Test
    fun failed_withOnRetry_showsRetryButton() {
        setBubble(
            model = assistant(streamState = AssistantStreamState.FAILED),
            onRetry = {}
        )

        composeTestRule.onNodeWithContentDescription("Retry reply").assertIsDisplayed()
    }

    @Test
    fun failed_withoutOnRetry_hidesRetryButton() {
        setBubble(
            model = assistant(streamState = AssistantStreamState.FAILED),
            onRetry = null
        )

        composeTestRule.onNodeWithContentDescription("Retry reply").assertDoesNotExist()
    }

    @Test
    fun received_withOnRetry_hidesRetryButton() {
        setBubble(
            model = assistant(
                content = "Hola",
                streamState = AssistantStreamState.RECEIVED
            ),
            onRetry = {}
        )

        composeTestRule.onNodeWithContentDescription("Retry reply").assertDoesNotExist()
    }

    @Test
    fun loading_withOnRetry_hidesRetryButton() {
        setBubble(
            model = assistant(streamState = AssistantStreamState.LOADING),
            onRetry = {}
        )

        composeTestRule.onNodeWithContentDescription("Retry reply").assertDoesNotExist()
    }

    @Test
    fun streaming_withOnRetry_hidesRetryButton() {
        setBubble(
            model = assistant(
                content = "partial",
                streamState = AssistantStreamState.STREAMING
            ),
            onRetry = {}
        )

        composeTestRule.onNodeWithContentDescription("Retry reply").assertDoesNotExist()
    }

    @Test
    fun clickingRetry_invokesCallback_once() {
        var retries = 0
        setBubble(
            model = assistant(streamState = AssistantStreamState.FAILED),
            onRetry = { retries++ }
        )

        composeTestRule.onNodeWithContentDescription("Retry reply").performClick()

        assertEquals(1, retries)
    }
}
