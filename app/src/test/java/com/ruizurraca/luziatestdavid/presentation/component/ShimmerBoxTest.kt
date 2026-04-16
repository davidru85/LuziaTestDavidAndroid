package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShimmerBoxTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersWithProvidedSizeModifier() {
        composeTestRule.setContent {
            LuziaTheme {
                ShimmerBox(
                    modifier = Modifier
                        .size(width = 140.dp, height = 20.dp)
                        .testTag("shimmer")
                )
            }
        }

        composeTestRule
            .onNodeWithTag("shimmer")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(140.dp)
            .assertHeightIsAtLeast(20.dp)
    }
}
