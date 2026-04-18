package com.ruizurraca.luziatestdavid.presentation.state

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Contract tests for [ChatEvent]: the VM → UI one-shot event channel classifying
 * errors per the 3-Tier strategy in `TECHNICAL_SPEC.md §Error Handling`.
 *
 *  - Tier-1 = Snackbar (validation / format errors, or fallback for paths that
 *    have no inline-bubble context).
 *  - Tier-3 = AlertDialog (critical / server errors requiring acknowledgement).
 *    Carries a semantic [Tier3Kind] (the composable resolves icon + title + copy
 *    via stringResource) plus the optional backend-verbatim `detailsMessage` for
 *    the dialog's collapsible Details section. No pre-resolved title string.
 *  - Tier-2 = inline bubble + retry; lives on `MessageStatus.FAILED`, NOT on
 *    the event channel.
 */
class ChatEventTest {

    @Test
    fun `Tier1 carries message`() {
        val event: ChatEvent = ChatEvent.Tier1("The request was invalid.")

        assertEquals("The request was invalid.", (event as ChatEvent.Tier1).message)
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
            ChatEvent.Tier1("snack"),
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
