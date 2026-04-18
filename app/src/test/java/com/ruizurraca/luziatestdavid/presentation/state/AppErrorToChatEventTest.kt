package com.ruizurraca.luziatestdavid.presentation.state

import com.ruizurraca.luziatestdavid.domain.common.AppError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * RED-phase contract tests for `AppError.toChatEvent()`: routes each domain
 * error variant to its tier-appropriate UI event per `TECHNICAL_SPEC.md §Error
 * Handling`.
 */
class AppErrorToChatEventTest {

    @Test
    fun `BadRequest maps to Tier1 carrying the AppError message`() {
        val event = AppError.BadRequest.toChatEvent()

        assertTrue(event is ChatEvent.Tier1)
        assertEquals(AppError.BadRequest.message, (event as ChatEvent.Tier1).message)
    }

    @Test
    fun `FileTooLarge maps to Tier1`() {
        val event = AppError.FileTooLarge.toChatEvent()

        assertTrue(event is ChatEvent.Tier1)
        assertEquals(AppError.FileTooLarge.message, (event as ChatEvent.Tier1).message)
    }

    @Test
    fun `Timeout maps to Tier1 as Snackbar fallback for paths without inline bubble context`() {
        val event = AppError.Timeout.toChatEvent()

        assertTrue(event is ChatEvent.Tier1)
        assertEquals(AppError.Timeout.message, (event as ChatEvent.Tier1).message)
    }

    @Test
    fun `Network maps to Tier1`() {
        val event = AppError.Network.toChatEvent()

        assertTrue(event is ChatEvent.Tier1)
        assertEquals(AppError.Network.message, (event as ChatEvent.Tier1).message)
    }

    @Test
    fun `ServiceUnavailable maps to Tier3 with AppError message`() {
        val event = AppError.ServiceUnavailable.toChatEvent()

        assertTrue(event is ChatEvent.Tier3)
        val tier3 = event as ChatEvent.Tier3
        assertTrue(tier3.title.isNotBlank())
        assertEquals(AppError.ServiceUnavailable.message, tier3.message)
    }

    @Test
    fun `Internal maps to Tier3`() {
        val event = AppError.Internal.toChatEvent()

        assertTrue(event is ChatEvent.Tier3)
        val tier3 = event as ChatEvent.Tier3
        assertTrue(tier3.title.isNotBlank())
        assertEquals(AppError.Internal.message, tier3.message)
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
