package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Stateless TTS toggle rendered beneath the last received assistant bubble
 * (Phase 10.6.D). Idle state shows a "Read aloud" affordance; speaking state
 * swaps to a "Stop reading aloud" affordance. Tap toggles playback via the
 * hoisted `onClick`. Hardcoded English content descriptions match Robolectric's
 * default locale; per-locale pinning lives in `A11yStringMigration*Test`.
 */
@RunWith(RobolectricTestRunner::class)
class TtsPlayButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun idleState_showsPlayIconWithReadAloudDescription() {
        composeTestRule.setContent {
            LuziaTheme {
                TtsPlayButton(isSpeaking = false, onClick = {})
            }
        }

        composeTestRule.onNodeWithContentDescription("Read aloud").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Stop reading aloud")
            .assertDoesNotExist()
    }

    @Test
    fun speakingState_showsStopIconWithStopReadingAloudDescription() {
        composeTestRule.setContent {
            LuziaTheme {
                TtsPlayButton(isSpeaking = true, onClick = {})
            }
        }

        composeTestRule.onNodeWithContentDescription("Stop reading aloud")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Read aloud").assertDoesNotExist()
    }

    @Test
    fun click_invokesOnClick() {
        var clicks = 0

        composeTestRule.setContent {
            LuziaTheme {
                TtsPlayButton(isSpeaking = false, onClick = { clicks++ })
            }
        }

        composeTestRule.onNodeWithContentDescription("Read aloud").performClick()

        assertEquals(1, clicks)
    }
}
