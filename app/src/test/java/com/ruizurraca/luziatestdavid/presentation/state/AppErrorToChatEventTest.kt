package com.ruizurraca.luziatestdavid.presentation.state

import com.ruizurraca.luziatestdavid.domain.common.AppError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Contract tests for `AppError.toChatEvent()`: routes each domain error variant
 * to its tier-appropriate UI event per `TECHNICAL_SPEC.md §Error Handling`.
 *
 * Phase 7.2.A: the six formerly-singleton variants are now data classes that
 * may carry a backend-supplied `rawMessage`. Tier routing is unchanged, but
 * the `.message` surface now reflects the backend value when supplied.
 */
class AppErrorToChatEventTest {

    @Test
    fun `BadRequest maps to Tier1 carrying the AppError message`() {
        val error = AppError.BadRequest()
        val event = error.toChatEvent()

        assertTrue(event is ChatEvent.Tier1)
        assertEquals(error.message, (event as ChatEvent.Tier1).message)
    }

    @Test
    fun `BadRequest with backend-supplied message maps to Tier1 preserving that message`() {
        val backendMessage = "Audio file is empty or too short to transcribe."
        val error = AppError.BadRequest(rawMessage = backendMessage)

        val event = error.toChatEvent()

        assertTrue(event is ChatEvent.Tier1)
        assertEquals(backendMessage, (event as ChatEvent.Tier1).message)
    }

    @Test
    fun `FileTooLarge maps to Tier1`() {
        val error = AppError.FileTooLarge()
        val event = error.toChatEvent()

        assertTrue(event is ChatEvent.Tier1)
        assertEquals(error.message, (event as ChatEvent.Tier1).message)
    }

    @Test
    fun `Timeout maps to Tier1 as Snackbar fallback for paths without inline bubble context`() {
        val error = AppError.Timeout()
        val event = error.toChatEvent()

        assertTrue(event is ChatEvent.Tier1)
        assertEquals(error.message, (event as ChatEvent.Tier1).message)
    }

    @Test
    fun `Network maps to Tier1`() {
        val error = AppError.Network()
        val event = error.toChatEvent()

        assertTrue(event is ChatEvent.Tier1)
        assertEquals(error.message, (event as ChatEvent.Tier1).message)
    }

    @Test
    fun `ServiceUnavailable maps to Tier3 with AppError message`() {
        val error = AppError.ServiceUnavailable()
        val event = error.toChatEvent()

        assertTrue(event is ChatEvent.Tier3)
        val tier3 = event as ChatEvent.Tier3
        assertTrue(tier3.title.isNotBlank())
        assertEquals(error.message, tier3.message)
    }

    @Test
    fun `Internal maps to Tier3`() {
        val error = AppError.Internal()
        val event = error.toChatEvent()

        assertTrue(event is ChatEvent.Tier3)
        val tier3 = event as ChatEvent.Tier3
        assertTrue(tier3.title.isNotBlank())
        assertEquals(error.message, tier3.message)
    }

    @Test
    fun `Internal with backend-supplied message maps to Tier3 preserving that message`() {
        val backendMessage = "Transcription service failed. Please try again."
        val error = AppError.Internal(rawMessage = backendMessage)

        val event = error.toChatEvent()

        assertTrue(event is ChatEvent.Tier3)
        assertEquals(backendMessage, (event as ChatEvent.Tier3).message)
    }

    @Test
    fun `Unknown maps to Tier3 preserving its raw message`() {
        val unknown = AppError.Unknown(rawCode = "STRANGE_XYZ", rawMessage = "something weird")

        val event = unknown.toChatEvent()

        assertTrue(event is ChatEvent.Tier3)
        val tier3 = event as ChatEvent.Tier3
        assertTrue(tier3.title.isNotBlank())
        assertEquals("something weird", tier3.message)
    }

    @Test
    fun `ValidationError maps to Tier1 preserving the backend-supplied message`() {
        val backendMessage = "user messages must include role_prompt."
        val error = AppError.ValidationError(rawMessage = backendMessage)

        val event = error.toChatEvent()

        assertTrue(event is ChatEvent.Tier1) { "expected Tier1, got $event" }
        assertEquals(backendMessage, (event as ChatEvent.Tier1).message)
    }
}
