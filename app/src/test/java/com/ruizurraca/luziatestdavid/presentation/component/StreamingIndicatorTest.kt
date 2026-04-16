package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StreamingIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun notVisible_labelNotRendered() {
        composeTestRule.setContent {
            LuziaTheme {
                StreamingIndicator(visible = false, label = "Thinking…")
            }
        }

        composeTestRule.onNodeWithText("Thinking…").assertDoesNotExist()
    }

    @Test
    fun visible_rendersLabel() {
        composeTestRule.setContent {
            LuziaTheme {
                StreamingIndicator(visible = true, label = "Recording…")
            }
        }

        composeTestRule.onNodeWithText("Recording…").assertIsDisplayed()
    }

    @Test
    fun visible_differentLabelsRenderIndependently() {
        composeTestRule.setContent {
            LuziaTheme {
                StreamingIndicator(visible = true, label = "Receiving…")
            }
        }

        composeTestRule.onNodeWithText("Receiving…").assertIsDisplayed()
        // Other phase labels must not leak
        composeTestRule.onNodeWithText("Thinking…").assertDoesNotExist()
        composeTestRule.onNodeWithText("Recording…").assertDoesNotExist()
    }
}
