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
            ChatMessage("s", MessageRole.SYSTEM, "You are helpful.", 0L),
            ChatMessage("u", MessageRole.USER, "Hi", 1L),
            ChatMessage("a", MessageRole.ASSISTANT, "Hello!", 2L)
        )

        val dto = mapper.toRequestDto(messages)

        assertEquals(
            listOf("system", "user", "assistant"),
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
}
