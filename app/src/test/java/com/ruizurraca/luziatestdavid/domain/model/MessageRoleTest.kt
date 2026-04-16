package com.ruizurraca.luziatestdavid.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageRoleTest {

    @Test
    fun `MessageRole defines USER ASSISTANT and SYSTEM`() {
        val roles = MessageRole.entries.toSet()

        assertTrue(MessageRole.USER in roles)
        assertTrue(MessageRole.ASSISTANT in roles)
        assertTrue(MessageRole.SYSTEM in roles)
        assertEquals(3, roles.size)
    }

    @Test
    fun `MessageRole wire values match backend contract`() {
        assertEquals("user", MessageRole.USER.wire)
        assertEquals("assistant", MessageRole.ASSISTANT.wire)
        assertEquals("system", MessageRole.SYSTEM.wire)
    }
}
