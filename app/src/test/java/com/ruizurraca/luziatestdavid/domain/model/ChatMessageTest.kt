package com.ruizurraca.luziatestdavid.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
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
    fun `ChatMessage defaults personaPrompt to null`() {
        val message = ChatMessage(
            id = "msg-1",
            role = MessageRole.USER,
            content = "Hello",
            timestamp = 1L
        )

        assertNull(message.personaPrompt)
    }

    @Test
    fun `ChatMessage captures personaPrompt when provided`() {
        val prompt = "You are a patient, educational tutor. " +
            "Explain concepts step by step and encourage learning."
        val message = ChatMessage(
            id = "msg-1",
            role = MessageRole.USER,
            content = "How does photosynthesis work?",
            timestamp = 1L,
            personaPrompt = prompt
        )

        assertEquals(prompt, message.personaPrompt)
    }

    @Test
    fun `ChatMessage copy produces independent value with overridden status`() {
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
    fun `ChatMessage copy preserves personaPrompt unless overridden`() {
        val tutorPrompt = "You are a patient, educational tutor."
        val artistPrompt = "You are a creative artist."

        val original = ChatMessage(
            id = "msg-1",
            role = MessageRole.USER,
            content = "Explain gravity",
            timestamp = 1L,
            personaPrompt = tutorPrompt
        )

        val preserved = original.copy(content = "Explain gravity in detail")
        assertEquals(tutorPrompt, preserved.personaPrompt)

        val reassigned = original.copy(personaPrompt = artistPrompt)
        assertEquals(artistPrompt, reassigned.personaPrompt)
        assertNotEquals(original, reassigned)
    }

    @Test
    fun `ChatMessage equality is structural including personaPrompt`() {
        val prompt = "You are a rigorous scientist."
        val a = ChatMessage("id", MessageRole.USER, "prompt", 10L, personaPrompt = prompt)
        val b = ChatMessage("id", MessageRole.USER, "prompt", 10L, personaPrompt = prompt)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())

        val c = ChatMessage("id", MessageRole.USER, "prompt", 10L, personaPrompt = null)
        assertNotEquals(a, c)
    }
}
