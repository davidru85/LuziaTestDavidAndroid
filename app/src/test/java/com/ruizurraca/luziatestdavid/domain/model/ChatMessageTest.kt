package com.ruizurraca.luziatestdavid.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class ChatMessageTest {

    @Test
    fun `ChatMessage exposes id role content and timestamp`() {
        val message = ChatMessage(
            id = "msg-1",
            role = MessageRole.USER,
            content = "Hello",
            timestamp = 1_700_000_000L
        )

        assertEquals("msg-1", message.id)
        assertEquals(MessageRole.USER, message.role)
        assertEquals("Hello", message.content)
        assertEquals(1_700_000_000L, message.timestamp)
    }

    @Test
    fun `ChatMessage defaults status to DELIVERED`() {
        val message = ChatMessage(
            id = "msg-1",
            role = MessageRole.ASSISTANT,
            content = "Hi there",
            timestamp = 1L
        )

        assertEquals(MessageStatus.DELIVERED, message.status)
    }

    @Test
    fun `ChatMessage copy produces independent value with overridden field`() {
        val original = ChatMessage(
            id = "msg-1",
            role = MessageRole.USER,
            content = "draft",
            timestamp = 1L,
            status = MessageStatus.PENDING
        )

        val sent = original.copy(status = MessageStatus.DELIVERED)

        assertEquals(MessageStatus.PENDING, original.status)
        assertEquals(MessageStatus.DELIVERED, sent.status)
        assertNotEquals(original, sent)
    }

    @Test
    fun `ChatMessage equality is structural`() {
        val a = ChatMessage("id", MessageRole.SYSTEM, "sys prompt", 10L)
        val b = ChatMessage("id", MessageRole.SYSTEM, "sys prompt", 10L)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
