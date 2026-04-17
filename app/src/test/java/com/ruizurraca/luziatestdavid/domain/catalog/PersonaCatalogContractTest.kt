package com.ruizurraca.luziatestdavid.domain.catalog

import com.ruizurraca.luziatestdavid.domain.model.Persona
import com.ruizurraca.luziatestdavid.domain.model.PersonaEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Compile-time contract test: this file will not compile unless [PersonaCatalog]
 * exposes `fun entries(): List<PersonaEntry>` and [PersonaEntry] carries
 * `(persona: Persona, displayName: String, prompt: String)`.
 *
 * Modelled after [com.ruizurraca.luziatestdavid.domain.repository.ChatRepositoryContractTest].
 */
class PersonaCatalogContractTest {

    private val fake = object : PersonaCatalog {
        override fun entries(): List<PersonaEntry> = listOf(
            PersonaEntry(Persona.STUDENT, "Student", "be a tutor"),
            PersonaEntry(Persona.SCIENTIST, "Scientist", "be rigorous"),
            PersonaEntry(Persona.ARTIST, "Artist", "be creative")
        )
    }

    @Test
    fun `entries returns a list of PersonaEntry`() {
        val result: List<PersonaEntry> = fake.entries()
        assertEquals(3, result.size)
    }

    @Test
    fun `entries preserve Persona order`() {
        assertEquals(
            listOf(Persona.STUDENT, Persona.SCIENTIST, Persona.ARTIST),
            fake.entries().map { it.persona }
        )
    }

    @Test
    fun `PersonaEntry exposes persona displayName and prompt`() {
        val entry = PersonaEntry(
            persona = Persona.STUDENT,
            displayName = "Student",
            prompt = "You are a tutor."
        )

        assertEquals(Persona.STUDENT, entry.persona)
        assertEquals("Student", entry.displayName)
        assertEquals("You are a tutor.", entry.prompt)
    }
}
