package com.ruizurraca.luziatestdavid.data.catalog

import com.ruizurraca.luziatestdavid.domain.model.Persona
import com.ruizurraca.luziatestdavid.domain.model.PersonaEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DefaultPersonaCatalog]. Pure JVM — no Android Context mocking.
 *
 * The Hilt provider in `di/CatalogModule` wires this impl with the two
 * `strings.xml` arrays; verifying that resource wiring is a separate concern.
 */
class DefaultPersonaCatalogTest {

    private val tutor = "You are a patient, educational tutor. " +
        "Explain concepts step by step and encourage learning."
    private val scientist = "You are a rigorous scientist. " +
        "Provide evidence-based, analytical, and precise answers."
    private val artist = "You are a creative artist. " +
        "Think imaginatively, brainstorm ideas, and inspire creativity."

    private val displayNames = listOf("Student", "Scientist", "Artist")
    private val prompts = listOf(tutor, scientist, artist)

    @Test
    fun `entries returns one PersonaEntry per Persona in order`() {
        val catalog = DefaultPersonaCatalog(displayNames, prompts)

        val entries = catalog.entries()

        assertEquals(
            listOf(Persona.STUDENT, Persona.SCIENTIST, Persona.ARTIST),
            entries.map { it.persona }
        )
    }

    @Test
    fun `entries bind displayName and prompt by matching index`() {
        val catalog = DefaultPersonaCatalog(displayNames, prompts)

        val entries = catalog.entries()

        assertEquals(
            listOf(
                PersonaEntry(Persona.STUDENT, "Student", tutor),
                PersonaEntry(Persona.SCIENTIST, "Scientist", scientist),
                PersonaEntry(Persona.ARTIST, "Artist", artist)
            ),
            entries
        )
    }

    @Test
    fun `constructor rejects displayNames of the wrong size`() {
        val tooFewNames = listOf("Student", "Scientist")

        assertThrows(IllegalArgumentException::class.java) {
            DefaultPersonaCatalog(tooFewNames, prompts)
        }
    }

    @Test
    fun `constructor rejects prompts of the wrong size`() {
        val tooManyPrompts = prompts + "extra"

        assertThrows(IllegalArgumentException::class.java) {
            DefaultPersonaCatalog(displayNames, tooManyPrompts)
        }
    }
}
