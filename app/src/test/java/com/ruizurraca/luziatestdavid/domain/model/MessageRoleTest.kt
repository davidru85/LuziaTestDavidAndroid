package com.ruizurraca.luziatestdavid.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageRoleTest {

    @Test
    fun `MessageRole defines exactly USER and ASSISTANT`() {
        val roles = MessageRole.entries.toSet()

        assertTrue(MessageRole.USER in roles)
        assertTrue(MessageRole.ASSISTANT in roles)
        assertEquals(2, roles.size)
    }

    @Test
    fun `MessageRole does not define SYSTEM`() {
        val wireValues = MessageRole.entries.map { it.wire }.toSet()
        val names = MessageRole.entries.map { it.name }.toSet()

        assertFalse("system" in wireValues) {
            "SYSTEM role must be removed (per MEMORY.md Fork 1): persona rides on each user message."
        }
        assertFalse("SYSTEM" in names) {
            "MessageRole.SYSTEM must be removed from the enum."
        }
    }

    @Test
    fun `MessageRole wire values match backend contract`() {
        assertEquals("user", MessageRole.USER.wire)
        assertEquals("assistant", MessageRole.ASSISTANT.wire)
    }
}
