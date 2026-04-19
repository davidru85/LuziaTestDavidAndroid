package com.ruizurraca.luziatestdavid.presentation.state

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Contract tests for [ChatEvent]: the VM → UI one-shot event channel classifying
 * errors per the strategy in `TECHNICAL_SPEC.md §Error Handling`.
 *
 *  - [ChatEvent.TransientSnackbar] — non-blocking severity. Carries a semantic
 *    [TransientSnackbarKind] (the composable resolves user-facing copy via
 *    stringResource) plus optional `backendMessage` that overrides the resolved
 *    copy when the backend supplies something specific (option iii — backend
 *    prefers when present, translated fallback otherwise).
 *  - [ChatEvent.BlockingErrorDialog] — critical / server errors requiring
 *    acknowledgement. Carries a semantic [BlockingErrorDialogKind] plus the
 *    optional backend-verbatim `detailsMessage` for the dialog's collapsible
 *    Details section.
 *  - Inline-bubble retry affordance for connectivity errors rides on
 *    `MessageStatus.FAILED`, NOT on this event channel.
 */
class ChatEventTest {

    @Test
    fun `TransientSnackbar carries a semantic kind and optional backend message`() {
        val event: ChatEvent = ChatEvent.TransientSnackbar(
            kind = TransientSnackbarKind.ValidationError,
            backendMessage = "user messages must include role_prompt."
        )

        val snackbar = event as ChatEvent.TransientSnackbar
        assertEquals(TransientSnackbarKind.ValidationError, snackbar.kind)
        assertEquals("user messages must include role_prompt.", snackbar.backendMessage)
    }

    @Test
    fun `TransientSnackbar backendMessage is optional and defaults to null`() {
        val event: ChatEvent = ChatEvent.TransientSnackbar(kind = TransientSnackbarKind.Network)

        val snackbar = event as ChatEvent.TransientSnackbar
        assertEquals(TransientSnackbarKind.Network, snackbar.kind)
        assertNull(snackbar.backendMessage)
    }

    @Test
    fun `TransientSnackbarKind covers every AppError variant plus local variants and Unknown fallback`() {
        val expected = setOf(
            // Backend-facing
            TransientSnackbarKind.BadRequest,
            TransientSnackbarKind.FileTooLarge,
            TransientSnackbarKind.Timeout,
            TransientSnackbarKind.Network,
            TransientSnackbarKind.ValidationError,
            // Local / client-originated
            TransientSnackbarKind.RecorderAlreadyRunning,
            TransientSnackbarKind.RecorderNotActive,
            TransientSnackbarKind.RecorderNoOutputFile,
            TransientSnackbarKind.RecorderStartFailed,
            TransientSnackbarKind.RecorderStopFailed,
            TransientSnackbarKind.EmptyAudioFile,
            TransientSnackbarKind.EmptyConversationHistory,
            TransientSnackbarKind.StreamingFailed,
            TransientSnackbarKind.UnexpectedFailure,
            TransientSnackbarKind.TtsUnavailable,
            // Legacy/unknown — message-only path without AppError attached
            TransientSnackbarKind.Unknown
        )

        assertEquals(expected, TransientSnackbarKind.entries.toSet())
    }

    @Test
    fun `BlockingErrorDialog carries semantic kind and optional backend detailsMessage`() {
        val event: ChatEvent = ChatEvent.BlockingErrorDialog(
            kind = BlockingErrorDialogKind.ServiceUnavailable,
            detailsMessage = "The service is temporarily unavailable."
        )

        val blockingError = event as ChatEvent.BlockingErrorDialog
        assertEquals(BlockingErrorDialogKind.ServiceUnavailable, blockingError.kind)
        assertEquals("The service is temporarily unavailable.", blockingError.detailsMessage)
    }

    @Test
    fun `BlockingErrorDialog detailsMessage is optional and defaults to null`() {
        val event: ChatEvent = ChatEvent.BlockingErrorDialog(kind = BlockingErrorDialogKind.InternalError)

        assertEquals(BlockingErrorDialogKind.InternalError, (event as ChatEvent.BlockingErrorDialog).kind)
        assertNull(event.detailsMessage)
    }

    @Test
    fun `BlockingErrorDialogKind covers every BlockingErrorDialog-mapped AppError variant`() {
        val expected = setOf(
            BlockingErrorDialogKind.ServiceUnavailable,
            BlockingErrorDialogKind.InternalError,
            BlockingErrorDialogKind.Unexpected
        )

        assertEquals(expected, BlockingErrorDialogKind.entries.toSet())
    }

    @Test
    fun `sealed hierarchy is exhaustive when matched`() {
        val events: List<ChatEvent> = listOf(
            ChatEvent.TransientSnackbar(kind = TransientSnackbarKind.Network),
            ChatEvent.BlockingErrorDialog(kind = BlockingErrorDialogKind.Unexpected, detailsMessage = "body")
        )

        val labels = events.map { event ->
            when (event) {
                is ChatEvent.TransientSnackbar -> "snackbar"
                is ChatEvent.BlockingErrorDialog -> "alert"
            }
        }

        assertEquals(listOf("snackbar", "alert"), labels)
    }
}
