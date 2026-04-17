package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MorphingActionButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyDraft_notRecording_showsMicWithRecordDescription() {
        composeTestRule.setContent {
            LuziaTheme {
                MorphingActionButton(
                    hasText = false,
                    isRecording = false,
                    onMicTap = {},
                    onSendTap = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Record voice message").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Stop recording").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Send message").assertDoesNotExist()
    }

    @Test
    fun emptyDraft_recording_showsMicWithStopDescription() {
        composeTestRule.setContent {
            LuziaTheme {
                MorphingActionButton(
                    hasText = false,
                    isRecording = true,
                    onMicTap = {},
                    onSendTap = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Stop recording").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Record voice message").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Send message").assertDoesNotExist()
    }

    @Test
    fun nonEmptyDraft_showsSendButton() {
        composeTestRule.setContent {
            LuziaTheme {
                MorphingActionButton(
                    hasText = true,
                    isRecording = false,
                    onMicTap = {},
                    onSendTap = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Send message").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Record voice message").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Stop recording").assertDoesNotExist()
    }

    @Test
    fun micTap_invokesOnMicTap_notOnSendTap() {
        var micTaps = 0
        var sendTaps = 0

        composeTestRule.setContent {
            LuziaTheme {
                MorphingActionButton(
                    hasText = false,
                    isRecording = false,
                    onMicTap = { micTaps++ },
                    onSendTap = { sendTaps++ }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Record voice message").performClick()

        assertEquals(1, micTaps)
        assertEquals(0, sendTaps)
    }

    @Test
    fun sendTap_invokesOnSendTap_notOnMicTap() {
        var micTaps = 0
        var sendTaps = 0

        composeTestRule.setContent {
            LuziaTheme {
                MorphingActionButton(
                    hasText = true,
                    isRecording = false,
                    onMicTap = { micTaps++ },
                    onSendTap = { sendTaps++ }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Send message").performClick()

        assertEquals(0, micTaps)
        assertEquals(1, sendTaps)
    }

    @Test
    fun disabled_buttonReportsNotEnabled() {
        composeTestRule.setContent {
            LuziaTheme {
                MorphingActionButton(
                    hasText = false,
                    isRecording = false,
                    enabled = false,
                    onMicTap = {},
                    onSendTap = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Record voice message").assertIsNotEnabled()
    }
}
