package com.ruizurraca.luziatestdavid.data.remote.mapper

import com.ruizurraca.luziatestdavid.data.remote.dto.ChatMessageDto
import com.ruizurraca.luziatestdavid.data.remote.dto.ChatRequestDto
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import com.ruizurraca.luziatestdavid.domain.model.MessageStatus
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * RED-phase contract tests for [ChatMapper].
 *
 * The mapper converts a `List<ChatMessage>` (domain) into a [ChatRequestDto] suitable
 * for the `POST /chat` endpoint body per TECHNICAL_SPEC §API.2:
 *
 *   { "messages": [ { "role": "...", "content": "..." } ] }
 *
 * Domain-only fields (id, timestamp, status) MUST NOT appear in the outbound DTO.
 */
class ChatMapperTest {

    private val mapper = ChatMapper()

    @Test
    fun `toRequestDto maps a single USER message`() {
        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "Hello", 1_000L)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(
            ChatRequestDto(listOf(ChatMessageDto(role = "user", content = "Hello"))),
            dto
        )
    }

    @Test
    fun `toRequestDto maps all MessageRole values to their wire strings`() {
        val messages = listOf(
            ChatMessage("u", MessageRole.USER, "Hi", 1L),
            ChatMessage("a", MessageRole.ASSISTANT, "Hello!", 2L)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(
            listOf("user", "assistant"),
            dto.messages.map { it.role }
        )
    }

    @Test
    fun `toRequestDto preserves message order`() {
        val messages = listOf(
            ChatMessage("a", MessageRole.USER, "first", 1L),
            ChatMessage("b", MessageRole.ASSISTANT, "second", 2L),
            ChatMessage("c", MessageRole.USER, "third", 3L),
            ChatMessage("d", MessageRole.ASSISTANT, "fourth", 4L)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(
            listOf("first", "second", "third", "fourth"),
            dto.messages.map { it.content }
        )
    }

    @Test
    fun `toRequestDto preserves content verbatim including whitespace and unicode`() {
        val tricky = "  leading spaces\n\ttab + newline — em-dash 😀 привет "
        val messages = listOf(
            ChatMessage("u", MessageRole.USER, tricky, 1L)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(tricky, dto.messages.single().content)
    }

    @Test
    fun `toRequestDto emits empty messages list for empty input`() {
        val dto = mapper.toRequestDto(emptyList())

        assertEquals(ChatRequestDto(emptyList()), dto)
    }

    @Test
    fun `toRequestDto ignores domain-only fields (id, timestamp, status)`() {
        val messages = listOf(
            ChatMessage(
                id = "irrelevant-uuid",
                role = MessageRole.USER,
                content = "hi",
                timestamp = 9_999_999L,
                status = MessageStatus.PENDING
            )
        )

        val json = Json.encodeToString(
            ChatRequestDto.serializer(),
            mapper.toRequestDto(messages)
        )

        assertEquals("""{"messages":[{"role":"user","content":"hi"}]}""", json)
    }

    // ----- Per-message persona capture (Phase 5.5.B, MEMORY.md Fork 1) ---------

    @Test
    fun `toRequestDto emits persona prompt as role for user messages`() {
        val prompt = "You are a patient, educational tutor. " +
            "Explain concepts step by step and encourage learning."
        val messages = listOf(
            ChatMessage(
                id = "u1",
                role = MessageRole.USER,
                content = "How does photosynthesis work?",
                timestamp = 1L,
                personaPrompt = prompt
            )
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(prompt, dto.messages.single().role)
        assertEquals("How does photosynthesis work?", dto.messages.single().content)
    }

    @Test
    fun `toRequestDto emits assistant wire string for assistant messages regardless of personaPrompt`() {
        val messages = listOf(
            ChatMessage(
                id = "a1",
                role = MessageRole.ASSISTANT,
                content = "La fotosíntesis es…",
                timestamp = 2L,
                personaPrompt = "ignored for assistants"
            )
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals("assistant", dto.messages.single().role)
    }

    @Test
    fun `toRequestDto falls back to user wire string when personaPrompt is null`() {
        val messages = listOf(
            ChatMessage(
                id = "u1",
                role = MessageRole.USER,
                content = "Hello",
                timestamp = 1L,
                personaPrompt = null
            )
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals("user", dto.messages.single().role)
    }

    @Test
    fun `toRequestDto captures different personas per user message across a conversation`() {
        val tutor = "You are a patient, educational tutor. " +
            "Explain concepts step by step and encourage learning."
        val artist = "You are a creative artist. " +
            "Think imaginatively, brainstorm ideas, and inspire creativity."

        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "¿Cómo funciona la fotosíntesis?", 1L, personaPrompt = tutor),
            ChatMessage("a1", MessageRole.ASSISTANT, "La fotosíntesis es…", 2L),
            ChatMessage("u2", MessageRole.USER, "Ahora escríbelo como un poema", 3L, personaPrompt = artist)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(
            listOf(tutor, "assistant", artist),
            dto.messages.map { it.role }
        )
    }

    @Test
    fun `toRequestDto matches the TECHNICAL_SPEC example JSON shape`() {
        val tutor = "You are a patient, educational tutor. " +
            "Explain concepts step by step and encourage learning."
        val artist = "You are a creative artist. " +
            "Think imaginatively, brainstorm ideas, and inspire creativity."

        val messages = listOf(
            ChatMessage("u1", MessageRole.USER, "¿Cómo funciona la fotosíntesis?", 1L, personaPrompt = tutor),
            ChatMessage("a1", MessageRole.ASSISTANT, "La fotosíntesis es…", 2L),
            ChatMessage("u2", MessageRole.USER, "Ahora escríbelo como un poema", 3L, personaPrompt = artist)
        )

        val dto = mapper.toRequestDto(messages)

        val expected = ChatRequestDto(
            listOf(
                ChatMessageDto(role = tutor, content = "¿Cómo funciona la fotosíntesis?"),
                ChatMessageDto(role = "assistant", content = "La fotosíntesis es…"),
                ChatMessageDto(role = artist, content = "Ahora escríbelo como un poema")
            )
        )
        assertEquals(expected, dto)
    }
}
