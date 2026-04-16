package com.ruizurraca.luziatestdavid.infra

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Smoke test proving the Robolectric + Compose + JUnit Vintage wiring works end-to-end
 * on the JVM. If this fails, nothing in 5.4 onwards will run — treat as load-bearing.
 */
@RunWith(RobolectricTestRunner::class)
class RobolectricComposeSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun composeContentRendersUnderRobolectric() {
        composeTestRule.setContent {
            Text(text = "hello from robolectric")
        }

        composeTestRule
            .onNodeWithText("hello from robolectric")
            .assertIsDisplayed()
    }
}
