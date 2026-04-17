package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Compose UI tests for `ChatTopAppBar` per DESIGN_SYSTEM.md §Top App Bar.
 *
 * Contract:
 *  - Title rendered with `titleLarge` typography.
 *  - `Icons.Outlined.DeleteSweep` icon button with `contentDescription = "Clear conversation"`.
 *  - Button disabled when `isConversationEmpty = true`.
 *  - Tapping the (enabled) button shows an `AlertDialog` with Confirm + Cancel actions.
 *  - Confirm tap invokes `onConfirmClearConversation` and dismisses the dialog.
 *  - Cancel tap dismisses without invoking the callback.
 *  - Disabled button ignores taps (no dialog shown).
 */
@RunWith(RobolectricTestRunner::class)
class ChatTopAppBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setBar(
        title: String = "Luzia",
        isConversationEmpty: Boolean = false,
        onConfirmClearConversation: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            LuziaTheme {
                ChatTopAppBar(
                    title = title,
                    isConversationEmpty = isConversationEmpty,
                    onConfirmClearConversation = onConfirmClearConversation
                )
            }
        }
    }

    // ----- Title + button visibility -----------------------------------------

    @Test
    fun title_isDisplayed() {
        setBar(title = "Luzia")

        composeTestRule.onNodeWithText("Luzia").assertIsDisplayed()
    }

    @Test
    fun deleteSweepIconButton_isDisplayed() {
        setBar()

        composeTestRule.onNodeWithContentDescription("Clear conversation").assertIsDisplayed()
    }

    // ----- Enabled / disabled -------------------------------------------------

    @Test
    fun emptyConversation_deleteButtonDisabled() {
        setBar(isConversationEmpty = true)

        composeTestRule.onNodeWithContentDescription("Clear conversation").assertIsNotEnabled()
    }

    @Test
    fun nonEmptyConversation_deleteButtonEnabled() {
        setBar(isConversationEmpty = false)

        composeTestRule.onNodeWithContentDescription("Clear conversation").assertIsEnabled()
    }

    // ----- Confirm dialog flow ------------------------------------------------

    @Test
    fun confirmDialog_hiddenByDefault() {
        setBar(isConversationEmpty = false)

        composeTestRule.onNodeWithText("Clear").assertDoesNotExist()
        composeTestRule.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test
    fun tappingDeleteSweep_showsConfirmDialog() {
        setBar(isConversationEmpty = false)

        composeTestRule.onNodeWithContentDescription("Clear conversation").performClick()

        composeTestRule.onNodeWithText("Clear").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun confirmButton_invokesCallback_andDismissesDialog() {
        var confirmations = 0
        setBar(
            isConversationEmpty = false,
            onConfirmClearConversation = { confirmations++ }
        )

        composeTestRule.onNodeWithContentDescription("Clear conversation").performClick()
        composeTestRule.onNodeWithText("Clear").performClick()

        assertEquals(1, confirmations)
        composeTestRule.onNodeWithText("Clear").assertDoesNotExist()
        composeTestRule.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test
    fun cancelButton_dismissesDialog_withoutCallback() {
        var confirmations = 0
        setBar(
            isConversationEmpty = false,
            onConfirmClearConversation = { confirmations++ }
        )

        composeTestRule.onNodeWithContentDescription("Clear conversation").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        assertEquals(0, confirmations)
        composeTestRule.onNodeWithText("Clear").assertDoesNotExist()
        composeTestRule.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test
    fun disabledButton_tap_doesNotShowDialog() {
        setBar(isConversationEmpty = true)

        composeTestRule.onNodeWithContentDescription("Clear conversation").performClick()

        composeTestRule.onNodeWithText("Clear").assertDoesNotExist()
        composeTestRule.onNodeWithText("Cancel").assertDoesNotExist()
    }
}
