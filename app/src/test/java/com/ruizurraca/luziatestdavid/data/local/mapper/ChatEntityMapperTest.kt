package com.ruizurraca.luziatestdavid.data.local.mapper

import com.ruizurraca.luziatestdavid.data.local.entity.ChatMessageEntity
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import com.ruizurraca.luziatestdavid.domain.model.MessageStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Round-trip tests for [ChatEntityMapper] focused on the per-message persona capture
 * introduced in Phase 5.5.B (see MEMORY.md Fork 1).
 *
 * Contract:
 *   - [ChatMessageEntity] MUST persist [ChatMessage.personaPrompt] verbatim, including null.
 *   - domain -> entity -> domain round-trip MUST be lossless.
 *   - The field is orthogonal to `role`: an assistant message with a non-null
 *     personaPrompt (shouldn't happen in practice but is representable) must still
 *     round-trip without mutation.
 */
class ChatEntityMapperTest {

    @Test
    fun `toEntity preserves personaPrompt for user messages`() {
        val prompt = "You are a patient, educational tutor. " +
            "Explain concepts step by step and encourage learning."
        val message = ChatMessage(
            id = "u1",
            role = MessageRole.USER,
            content = "How does photosynthesis work?",
            timestamp = 1_000L,
            status = MessageStatus.DELIVERED,
            personaPrompt = prompt
        )

        val entity = message.toEntity()

        assertEquals(prompt, entity.personaPrompt)
    }

    @Test
    fun `toEntity preserves null personaPrompt for assistant messages`() {
        val message = ChatMessage(
            id = "a1",
            role = MessageRole.ASSISTANT,
            content = "La fotosíntesis es…",
            timestamp = 2_000L,
            status = MessageStatus.DELIVERED,
            personaPrompt = null
        )

        val entity = message.toEntity()

        assertNull(entity.personaPrompt)
    }

    @Test
    fun `toDomain preserves personaPrompt from entity`() {
        val prompt = "You are a creative artist."
        val entity = ChatMessageEntity(
            id = "u2",
            role = "user",
            content = "Write this as a poem",
            timestamp = 3_000L,
            status = "DELIVERED",
            personaPrompt = prompt
        )

        val message = entity.toDomain()

        assertEquals(prompt, message.personaPrompt)
    }

    @Test
    fun `toDomain preserves null personaPrompt`() {
        val entity = ChatMessageEntity(
            id = "a2",
            role = "assistant",
            content = "Let me try…",
            timestamp = 4_000L,
            status = "DELIVERED",
            personaPrompt = null
        )

        val message = entity.toDomain()

        assertNull(message.personaPrompt)
    }

    @Test
    fun `domain to entity to domain round-trip is lossless for user with persona`() {
        val original = ChatMessage(
            id = "u3",
            role = MessageRole.USER,
            content = "Explain gravity",
            timestamp = 5_000L,
            status = MessageStatus.PENDING,
            personaPrompt = "You are a rigorous scientist."
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `domain to entity to domain round-trip is lossless for assistant without persona`() {
        val original = ChatMessage(
            id = "a3",
            role = MessageRole.ASSISTANT,
            content = "Gravity is…",
            timestamp = 6_000L,
            status = MessageStatus.FAILED,
            personaPrompt = null
        )

        val roundTripped = original.toEntity().toDomain()

        assertEquals(original, roundTripped)
    }
}
