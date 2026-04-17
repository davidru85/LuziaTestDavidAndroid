package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.ruizurraca.luziatestdavid.presentation.model.ChatMessageUiModel
import com.ruizurraca.luziatestdavid.presentation.model.UserDeliveryState
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserMessageBubbleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun user(
        content: String = "Hola",
        deliveryState: UserDeliveryState = UserDeliveryState.SENT,
        id: String = "u-1"
    ) = ChatMessageUiModel.User(
        id = id,
        timestamp = 0L,
        content = content,
        deliveryState = deliveryState
    )

    private fun setBubble(model: ChatMessageUiModel.User) {
        composeTestRule.setContent {
            LuziaTheme {
                UserMessageBubble(model = model)
            }
        }
    }

    @Test
    fun rendersMessageContent() {
        setBubble(user(content = "Hola qué tal"))

        composeTestRule.onNodeWithText("Hola qué tal").assertIsDisplayed()
    }

    @Test
    fun sending_showsProgressIndicator_hidesCheckAndError() {
        setBubble(user(deliveryState = UserDeliveryState.SENDING))

        composeTestRule.onNodeWithContentDescription("Sending message").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Message sent").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Failed to send").assertDoesNotExist()
    }

    @Test
    fun sent_showsCheckIcon_hidesProgressAndError() {
        setBubble(user(deliveryState = UserDeliveryState.SENT))

        composeTestRule.onNodeWithContentDescription("Message sent").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Sending message").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Failed to send").assertDoesNotExist()
    }

    @Test
    fun failed_showsErrorIcon_hidesCheckAndProgress() {
        setBubble(user(deliveryState = UserDeliveryState.FAILED))

        composeTestRule.onNodeWithContentDescription("Failed to send").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Message sent").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Sending message").assertDoesNotExist()
    }
}
