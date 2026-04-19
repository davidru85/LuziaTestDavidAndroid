package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ruizurraca.luziatestdavid.domain.model.Persona
import com.ruizurraca.luziatestdavid.domain.model.PersonaEntry
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Compose UI tests for `RoleSelectorChips` per DESIGN_SYSTEM.md §Role Selector.
 *
 * Contract:
 *  - One [androidx.compose.material3.FilterChip] per entry, labelled by `displayName`.
 *  - Exactly one chip is selected (SingleSelect), matching `selectedPersona`.
 *  - Tapping a chip invokes `onPersonaSelected` with that persona.
 *  - Each chip exposes `Role.RadioButton` for accessibility (DESIGN_SYSTEM §A11y).
 */
@RunWith(RobolectricTestRunner::class)
class RoleSelectorChipsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val entries = listOf(
        PersonaEntry(Persona.STUDENT, "Student", "tutor prompt"),
        PersonaEntry(Persona.SCIENTIST, "Scientist", "scientist prompt"),
        PersonaEntry(Persona.ARTIST, "Artist", "artist prompt")
    )

    @Test
    fun rendersOneChipPerEntry() {
        composeTestRule.setContent {
            LuziaTheme {
                RoleSelectorChips(
                    entries = entries,
                    selectedPersona = Persona.STUDENT,
                    onPersonaSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Student").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scientist").assertIsDisplayed()
        composeTestRule.onNodeWithText("Artist").assertIsDisplayed()
    }

    @Test
    fun selectedPersonaChip_isMarkedSelected_othersNot() {
        composeTestRule.setContent {
            LuziaTheme {
                RoleSelectorChips(
                    entries = entries,
                    selectedPersona = Persona.ARTIST,
                    onPersonaSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Artist").assertIsSelected()
        composeTestRule.onNodeWithText("Student").assertIsNotSelected()
        composeTestRule.onNodeWithText("Scientist").assertIsNotSelected()
    }

    @Test
    fun tappingAChip_invokesOnPersonaSelected_withThatPersona() {
        var selected: Persona? = null

        composeTestRule.setContent {
            LuziaTheme {
                RoleSelectorChips(
                    entries = entries,
                    selectedPersona = Persona.STUDENT,
                    onPersonaSelected = { selected = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Scientist").performClick()

        assertEquals(Persona.SCIENTIST, selected)
    }

    @Test
    fun tappingAlreadySelectedChip_stillInvokesCallback() {
        var invocations = 0
        var lastSelected: Persona? = null

        composeTestRule.setContent {
            LuziaTheme {
                RoleSelectorChips(
                    entries = entries,
                    selectedPersona = Persona.STUDENT,
                    onPersonaSelected = {
                        invocations++
                        lastSelected = it
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("Student").performClick()

        assertEquals(1, invocations)
        assertEquals(Persona.STUDENT, lastSelected)
    }

    @Test
    fun tappingMultipleChips_invokesCallbackEachTime() {
        val received = mutableListOf<Persona>()

        composeTestRule.setContent {
            LuziaTheme {
                RoleSelectorChips(
                    entries = entries,
                    selectedPersona = Persona.STUDENT,
                    onPersonaSelected = { received.add(it) }
                )
            }
        }

        composeTestRule.onNodeWithText("Scientist").performClick()
        composeTestRule.onNodeWithText("Artist").performClick()

        assertEquals(listOf(Persona.SCIENTIST, Persona.ARTIST), received)
    }

    @Test
    fun eachChip_exposesRadioButtonRoleForAccessibility() {
        composeTestRule.setContent {
            LuziaTheme {
                RoleSelectorChips(
                    entries = entries,
                    selectedPersona = Persona.STUDENT,
                    onPersonaSelected = {}
                )
            }
        }

        val isRadioButton = SemanticsMatcher.expectValue(
            SemanticsProperties.Role,
            Role.RadioButton
        )
        composeTestRule.onNodeWithText("Student").assert(isRadioButton)
        composeTestRule.onNodeWithText("Scientist").assert(isRadioButton)
        composeTestRule.onNodeWithText("Artist").assert(isRadioButton)
    }

    // region Phase 7.3.3.E — persona icons persistent across chips

    @Test
    fun eachChip_displaysItsPersonaSpecificLeadingIcon() {
        composeTestRule.setContent {
            LuziaTheme {
                RoleSelectorChips(
                    entries = entries,
                    selectedPersona = Persona.STUDENT,
                    onPersonaSelected = {}
                )
            }
        }

        // Icons are persistent (always visible, not just on the selected chip) so each
        // persona has a stable visual identity. testTag is the stable handle —
        // ImageVector equality via semantics isn't directly exposed. useUnmergedTree
        // because FilterChip merges the leadingIcon's semantics into the chip node.
        composeTestRule.onNodeWithTag("persona-icon-STUDENT", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("persona-icon-SCIENTIST", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("persona-icon-ARTIST", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun personaIcons_remainVisibleOnUnselectedChips() {
        composeTestRule.setContent {
            LuziaTheme {
                RoleSelectorChips(
                    entries = entries,
                    selectedPersona = Persona.ARTIST,
                    onPersonaSelected = {}
                )
            }
        }

        // Non-selected personas still display their icon.
        composeTestRule.onNodeWithTag("persona-icon-STUDENT", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("persona-icon-SCIENTIST", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    // endregion

    @Test
    fun emptyEntries_rendersNoChips() {
        var callbacks: Persona? = null

        composeTestRule.setContent {
            LuziaTheme {
                RoleSelectorChips(
                    entries = emptyList(),
                    selectedPersona = Persona.STUDENT,
                    onPersonaSelected = { callbacks = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Student").assertDoesNotExist()
        composeTestRule.onNodeWithText("Scientist").assertDoesNotExist()
        composeTestRule.onNodeWithText("Artist").assertDoesNotExist()
        assertNull(callbacks)
    }
}
