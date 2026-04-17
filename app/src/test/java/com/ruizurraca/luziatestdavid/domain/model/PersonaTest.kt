package com.ruizurraca.luziatestdavid.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Contract for the Persona enum per DESIGN_SYSTEM.md §Role Selector and
 * MEMORY.md Fork 1.
 *
 * Order matters: it mirrors the parallel string arrays `role_names` and
 * `role_prompts` in strings.xml (indexes correspond 1:1).
 */
class PersonaTest {

    @Test
    fun `Persona defines exactly three entries`() {
        assertEquals(3, Persona.entries.size)
    }

    @Test
    fun `Persona ordinals are STUDENT SCIENTIST ARTIST in order`() {
        assertEquals(listOf(Persona.STUDENT, Persona.SCIENTIST, Persona.ARTIST), Persona.entries)
    }

    @Test
    fun `Persona STUDENT is the default at ordinal zero`() {
        assertEquals(0, Persona.STUDENT.ordinal)
    }
}
