package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.ruizurraca.luziatestdavid.presentation.model.AssistantStreamState
import com.ruizurraca.luziatestdavid.presentation.model.ChatMessageUiModel
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme
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
        isRetryable: Boolean = false
    ) {
        composeTestRule.setContent {
            LuziaTheme {
                AssistantMessageBubble(model = model, isRetryable = isRetryable)
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

    // region Phase 10.6.A — retry affordance lives OUTSIDE the bubble

    @Test
    fun failed_retryable_doesNotRenderRetryButtonInsideBubble() {
        // The retry action moved out of the bubble in 10.6.A — the bubble must
        // NEVER host the retry control, regardless of the retryable flag.
        setBubble(
            model = assistant(streamState = AssistantStreamState.FAILED),
            isRetryable = true
        )

        composeTestRule.onNodeWithText("Retry reply").assertDoesNotExist()
    }

    @Test
    fun failed_nonRetryable_doesNotRenderRetryButtonInsideBubble() {
        setBubble(
            model = assistant(streamState = AssistantStreamState.FAILED),
            isRetryable = false
        )

        composeTestRule.onNodeWithText("Retry reply").assertDoesNotExist()
    }

    // endregion

    // region Phase 7.3.3.B — FAILED bubble friendly copy (retryable vs older)

    @Test
    fun failed_retryable_showsGoneBlankCopy() {
        // The LATEST failed assistant turn is retryable — show the "gone blank"
        // prompt that invites the user to tap retry (the button is now rendered
        // below the bubble by ChatScreenContent, but the bubble copy still
        // reflects the retryable intent).
        setBubble(
            model = assistant(streamState = AssistantStreamState.FAILED),
            isRetryable = true
        )

        composeTestRule.onNodeWithText("I've gone blank. Mind retrying?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sorry, empty message").assertDoesNotExist()
    }

    @Test
    fun failed_nonRetryable_showsEmptyMessageCopy() {
        // Older failed assistant turns aren't retryable — show the apologetic
        // copy only.
        setBubble(
            model = assistant(streamState = AssistantStreamState.FAILED),
            isRetryable = false
        )

        composeTestRule.onNodeWithText("Sorry, empty message").assertIsDisplayed()
        composeTestRule.onNodeWithText("I've gone blank. Mind retrying?").assertDoesNotExist()
    }

    // endregion

    // region Phase 7.3.1.B — LiveRegion.Polite for TalkBack announcements

    @Test
    fun loading_bubble_marksLiveRegionPolite_forTalkBack() {
        setBubble(assistant(streamState = AssistantStreamState.LOADING))

        composeTestRule
            .onAllNodes(isPoliteLiveRegion())
            .assertCountEquals(1)
    }

    @Test
    fun streaming_textBubble_marksLiveRegionPolite_forTalkBack() {
        setBubble(
            assistant(
                content = "La fotosíntesis es",
                streamState = AssistantStreamState.STREAMING
            )
        )

        composeTestRule
            .onAllNodes(isPoliteLiveRegion())
            .assertCountEquals(1)
    }

    @Test
    fun received_textBubble_doesNotMarkLiveRegion() {
        setBubble(
            assistant(
                content = "La fotosíntesis es el proceso por el cual las plantas convierten la luz solar en energía.",
                streamState = AssistantStreamState.RECEIVED
            )
        )

        composeTestRule
            .onAllNodes(isPoliteLiveRegion())
            .assertCountEquals(0)
    }

    @Test
    fun failed_bubble_doesNotMarkLiveRegion() {
        setBubble(assistant(streamState = AssistantStreamState.FAILED), isRetryable = true)

        composeTestRule
            .onAllNodes(isPoliteLiveRegion())
            .assertCountEquals(0)
    }

    private fun isPoliteLiveRegion(): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    // endregion
}
