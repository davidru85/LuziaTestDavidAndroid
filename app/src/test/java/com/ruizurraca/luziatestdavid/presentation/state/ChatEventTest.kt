package com.ruizurraca.luziatestdavid.presentation.state

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * RED-phase contract tests for [ChatEvent]: the VM → UI one-shot event channel
 * classifying errors per the 3-Tier strategy in `TECHNICAL_SPEC.md §Error Handling`.
 *
 *  - Tier-1 = Snackbar (validation / format errors, or fallback for paths that
 *    have no inline-bubble context).
 *  - Tier-3 = AlertDialog (critical / server errors requiring acknowledgement).
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
    fun `Tier3 carries title and message`() {
        val event: ChatEvent = ChatEvent.Tier3(
            title = "Service error",
            message = "The service is temporarily unavailable."
        )

        val tier3 = event as ChatEvent.Tier3
        assertEquals("Service error", tier3.title)
        assertEquals("The service is temporarily unavailable.", tier3.message)
    }

    @Test
    fun `sealed hierarchy is exhaustive when matched`() {
        val events: List<ChatEvent> = listOf(
            ChatEvent.Tier1("snack"),
            ChatEvent.Tier3("Alert", "body")
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
