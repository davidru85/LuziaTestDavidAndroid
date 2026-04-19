package com.ruizurraca.luziatestdavid.presentation.state

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Contract tests for [ChatEvent]: the VM → UI one-shot event channel classifying
 * errors per the 3-Tier strategy in `TECHNICAL_SPEC.md §Error Handling`.
 *
 *  - Tier-1 = Snackbar. Carries a semantic [Tier1Kind] (the composable resolves
 *    user-facing copy via stringResource) plus optional `backendMessage` that
 *    overrides the resolved copy when the backend supplies something specific
 *    (option iii — backend prefers when present, translated fallback otherwise).
 *  - Tier-3 = AlertDialog (critical / server errors requiring acknowledgement).
 *    Carries a semantic [Tier3Kind] plus the optional backend-verbatim
 *    `detailsMessage` for the dialog's collapsible Details section.
 *  - Tier-2 = inline bubble + retry; lives on `MessageStatus.FAILED`, NOT on
 *    the event channel.
 */
class ChatEventTest {

    @Test
    fun `Tier1 carries a semantic kind and optional backend message`() {
        val event: ChatEvent = ChatEvent.Tier1(
            kind = Tier1Kind.ValidationError,
            backendMessage = "user messages must include role_prompt."
        )

        val tier1 = event as ChatEvent.Tier1
        assertEquals(Tier1Kind.ValidationError, tier1.kind)
        assertEquals("user messages must include role_prompt.", tier1.backendMessage)
    }

    @Test
    fun `Tier1 backendMessage is optional and defaults to null`() {
        val event: ChatEvent = ChatEvent.Tier1(kind = Tier1Kind.Network)

        val tier1 = event as ChatEvent.Tier1
        assertEquals(Tier1Kind.Network, tier1.kind)
        assertNull(tier1.backendMessage)
    }

    @Test
    fun `Tier1Kind covers every Tier-1 mapped AppError variant plus local variants and Unknown fallback`() {
        val expected = setOf(
            // Backend-facing
            Tier1Kind.BadRequest,
            Tier1Kind.FileTooLarge,
            Tier1Kind.Timeout,
            Tier1Kind.Network,
            Tier1Kind.ValidationError,
            // Local / client-originated
            Tier1Kind.RecorderAlreadyRunning,
            Tier1Kind.RecorderNotActive,
            Tier1Kind.RecorderNoOutputFile,
            Tier1Kind.RecorderStartFailed,
            Tier1Kind.RecorderStopFailed,
            Tier1Kind.EmptyAudioFile,
            Tier1Kind.EmptyConversationHistory,
            Tier1Kind.StreamingFailed,
            Tier1Kind.UnexpectedFailure,
            Tier1Kind.TtsUnavailable,
            // Legacy/unknown — message-only path without AppError attached
            Tier1Kind.Unknown
        )

        assertEquals(expected, Tier1Kind.entries.toSet())
    }

    @Test
    fun `Tier3 carries semantic kind and optional backend detailsMessage`() {
        val event: ChatEvent = ChatEvent.Tier3(
            kind = Tier3Kind.ServiceUnavailable,
            detailsMessage = "The service is temporarily unavailable."
        )

        val tier3 = event as ChatEvent.Tier3
        assertEquals(Tier3Kind.ServiceUnavailable, tier3.kind)
        assertEquals("The service is temporarily unavailable.", tier3.detailsMessage)
    }

    @Test
    fun `Tier3 detailsMessage is optional and defaults to null`() {
        val event: ChatEvent = ChatEvent.Tier3(kind = Tier3Kind.InternalError)

        assertEquals(Tier3Kind.InternalError, (event as ChatEvent.Tier3).kind)
        assertNull(event.detailsMessage)
    }

    @Test
    fun `Tier3Kind covers every Tier-3 mapped AppError variant`() {
        val expected = setOf(
            Tier3Kind.ServiceUnavailable,
            Tier3Kind.InternalError,
            Tier3Kind.Unexpected
        )

        assertEquals(expected, Tier3Kind.entries.toSet())
    }

    @Test
    fun `sealed hierarchy is exhaustive when matched`() {
        val events: List<ChatEvent> = listOf(
            ChatEvent.Tier1(kind = Tier1Kind.Network),
            ChatEvent.Tier3(kind = Tier3Kind.Unexpected, detailsMessage = "body")
        )

        val labels = events.map { event ->
            when (event) {
                is ChatEvent.Tier1 -> "snackbar"
                is ChatEvent.Tier3 -> "alert"
            }
        }

        assertEquals(listOf("snackbar", "alert"), labels)
    }
}
