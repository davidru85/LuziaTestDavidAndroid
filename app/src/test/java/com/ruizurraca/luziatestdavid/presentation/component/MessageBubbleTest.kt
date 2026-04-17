package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.ruizurraca.luziatestdavid.presentation.model.AssistantStreamState
import com.ruizurraca.luziatestdavid.presentation.model.ChatMessageUiModel
import com.ruizurraca.luziatestdavid.presentation.model.UserDeliveryState
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessageBubbleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setDispatcher(model: ChatMessageUiModel) {
        composeTestRule.setContent {
            LuziaTheme {
                MessageBubble(model = model)
            }
        }
    }

    @Test
    fun userModel_routesToUserBubble() {
        setDispatcher(
            ChatMessageUiModel.User(
                id = "u-1",
                timestamp = 0L,
                content = "hola",
                deliveryState = UserDeliveryState.SENT
            )
        )

        // SENT icon is unique to the user bubble
        composeTestRule.onNodeWithContentDescription("Message sent").assertIsDisplayed()
        // Assistant-bubble signatures must NOT appear
        composeTestRule.onNodeWithContentDescription("Loading response").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Reply failed").assertDoesNotExist()
    }

    @Test
    fun assistantModel_routesToAssistantBubble() {
        setDispatcher(
            ChatMessageUiModel.Assistant(
                id = "a-1",
                timestamp = 0L,
                content = "",
                streamState = AssistantStreamState.LOADING
            )
        )

        // Shimmer semantic is unique to the assistant bubble LOADING state
        composeTestRule.onNodeWithContentDescription("Loading response").assertIsDisplayed()
        // User-bubble signatures must NOT appear
        composeTestRule.onNodeWithContentDescription("Message sent").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Sending message").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Failed to send").assertDoesNotExist()
    }
}
