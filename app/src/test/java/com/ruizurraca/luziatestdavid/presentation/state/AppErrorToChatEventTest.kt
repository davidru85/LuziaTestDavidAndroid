package com.ruizurraca.luziatestdavid.presentation.state

import com.ruizurraca.luziatestdavid.domain.common.AppError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Contract tests for `AppError.toChatEvent()`: routes each domain error variant
 * to its tier-appropriate UI event per `TECHNICAL_SPEC.md §Error Handling`.
 *
 * Phase 7.3.3.H.2: transient-snackbar events no longer carry a pre-resolved
 * `message` String. They carry a semantic [TransientSnackbarKind] (the
 * composable resolves copy via stringResource) plus an optional
 * `backendMessage` that preempts the translated copy whenever the backend
 * supplied something specific (option iii). Blocking-error-dialog events
 * continue the same pattern with [BlockingErrorDialogKind] + `detailsMessage`.
 */
class AppErrorToChatEventTest {

    // region TransientSnackbar — backend-facing variants (kind + optional backendMessage)

    @Test
    fun `BadRequest default maps to TransientSnackbar with BadRequest kind and null backendMessage`() {
        val event = AppError.BadRequest().toChatEvent()

        val snackbar = event as ChatEvent.TransientSnackbar
        assertEquals(TransientSnackbarKind.BadRequest, snackbar.kind)
        assertNull(snackbar.backendMessage)
    }

    @Test
    fun `BadRequest with backend-supplied message routes that message through as backendMessage`() {
        val backendMessage = "Audio file is empty or too short to transcribe."
        val event = AppError.BadRequest(rawMessage = backendMessage).toChatEvent()

        val snackbar = event as ChatEvent.TransientSnackbar
        assertEquals(TransientSnackbarKind.BadRequest, snackbar.kind)
        assertEquals(backendMessage, snackbar.backendMessage)
    }

    @Test
    fun `FileTooLarge maps to TransientSnackbar with FileTooLarge kind`() {
        val event = AppError.FileTooLarge().toChatEvent()

        assertTrue(event is ChatEvent.TransientSnackbar)
        assertEquals(TransientSnackbarKind.FileTooLarge, (event as ChatEvent.TransientSnackbar).kind)
    }

    @Test
    fun `Timeout maps to TransientSnackbar with Timeout kind`() {
        val event = AppError.Timeout().toChatEvent()

        assertTrue(event is ChatEvent.TransientSnackbar)
        assertEquals(TransientSnackbarKind.Timeout, (event as ChatEvent.TransientSnackbar).kind)
    }

    @Test
    fun `Network maps to TransientSnackbar with Network kind`() {
        val event = AppError.Network().toChatEvent()

        assertTrue(event is ChatEvent.TransientSnackbar)
        assertEquals(TransientSnackbarKind.Network, (event as ChatEvent.TransientSnackbar).kind)
    }

    @Test
    fun `ValidationError maps to TransientSnackbar preserving backend message`() {
        val backendMessage = "user messages must include role_prompt."
        val event = AppError.ValidationError(rawMessage = backendMessage).toChatEvent()

        val snackbar = event as ChatEvent.TransientSnackbar
        assertEquals(TransientSnackbarKind.ValidationError, snackbar.kind)
        assertEquals(backendMessage, snackbar.backendMessage)
    }

    // endregion

    // region TransientSnackbar — local variants (never carry a backendMessage)

    @Test
    fun `RecorderAlreadyRunning maps to TransientSnackbar with RecorderAlreadyRunning kind and no backendMessage`() {
        val event = AppError.RecorderAlreadyRunning.toChatEvent()

        val snackbar = event as ChatEvent.TransientSnackbar
        assertEquals(TransientSnackbarKind.RecorderAlreadyRunning, snackbar.kind)
        assertNull(snackbar.backendMessage)
    }

    @Test
    fun `RecorderNotActive maps to TransientSnackbar with RecorderNotActive kind`() {
        val event = AppError.RecorderNotActive.toChatEvent()
        assertEquals(TransientSnackbarKind.RecorderNotActive, (event as ChatEvent.TransientSnackbar).kind)
    }

    @Test
    fun `RecorderNoOutputFile maps to TransientSnackbar with RecorderNoOutputFile kind`() {
        val event = AppError.RecorderNoOutputFile.toChatEvent()
        assertEquals(TransientSnackbarKind.RecorderNoOutputFile, (event as ChatEvent.TransientSnackbar).kind)
    }

    @Test
    fun `RecorderStartFailed maps to TransientSnackbar with RecorderStartFailed kind`() {
        val event = AppError.RecorderStartFailed.toChatEvent()
        assertEquals(TransientSnackbarKind.RecorderStartFailed, (event as ChatEvent.TransientSnackbar).kind)
    }

    @Test
    fun `RecorderStopFailed maps to TransientSnackbar with RecorderStopFailed kind`() {
        val event = AppError.RecorderStopFailed.toChatEvent()
        assertEquals(TransientSnackbarKind.RecorderStopFailed, (event as ChatEvent.TransientSnackbar).kind)
    }

    @Test
    fun `EmptyAudioFile maps to TransientSnackbar with EmptyAudioFile kind`() {
        val event = AppError.EmptyAudioFile.toChatEvent()
        assertEquals(TransientSnackbarKind.EmptyAudioFile, (event as ChatEvent.TransientSnackbar).kind)
    }

    @Test
    fun `EmptyConversationHistory maps to TransientSnackbar with EmptyConversationHistory kind`() {
        val event = AppError.EmptyConversationHistory.toChatEvent()
        assertEquals(TransientSnackbarKind.EmptyConversationHistory, (event as ChatEvent.TransientSnackbar).kind)
    }

    @Test
    fun `StreamingFailed maps to TransientSnackbar with StreamingFailed kind`() {
        val event = AppError.StreamingFailed.toChatEvent()
        assertEquals(TransientSnackbarKind.StreamingFailed, (event as ChatEvent.TransientSnackbar).kind)
    }

    @Test
    fun `UnexpectedFailure maps to TransientSnackbar with UnexpectedFailure kind`() {
        val event = AppError.UnexpectedFailure.toChatEvent()
        assertEquals(TransientSnackbarKind.UnexpectedFailure, (event as ChatEvent.TransientSnackbar).kind)
    }

    @Test
    fun `TtsUnavailable maps to TransientSnackbar with TtsUnavailable kind and no backendMessage`() {
        val event = AppError.TtsUnavailable.toChatEvent()

        val snackbar = event as ChatEvent.TransientSnackbar
        assertEquals(TransientSnackbarKind.TtsUnavailable, snackbar.kind)
        assertNull(snackbar.backendMessage)
    }

    // endregion

    // region BlockingErrorDialog — kind + detailsMessage (unchanged from 7.3.3.C)

    @Test
    fun `ServiceUnavailable maps to BlockingErrorDialog with ServiceUnavailable kind and backend detailsMessage`() {
        val error = AppError.ServiceUnavailable()
        val event = error.toChatEvent()

        val blockingError = event as ChatEvent.BlockingErrorDialog
        assertEquals(BlockingErrorDialogKind.ServiceUnavailable, blockingError.kind)
        assertEquals(error.message, blockingError.detailsMessage)
    }

    @Test
    fun `Internal maps to BlockingErrorDialog with InternalError kind`() {
        val event = AppError.Internal().toChatEvent()

        assertTrue(event is ChatEvent.BlockingErrorDialog)
        assertEquals(BlockingErrorDialogKind.InternalError, (event as ChatEvent.BlockingErrorDialog).kind)
    }

    @Test
    fun `Internal with backend-supplied message maps to BlockingErrorDialog preserving that message as detailsMessage`() {
        val backendMessage = "Transcription service failed. Please try again."
        val error = AppError.Internal(rawMessage = backendMessage)

        val event = error.toChatEvent()

        val blockingError = event as ChatEvent.BlockingErrorDialog
        assertEquals(BlockingErrorDialogKind.InternalError, blockingError.kind)
        assertEquals(backendMessage, blockingError.detailsMessage)
    }

    @Test
    fun `Unknown maps to BlockingErrorDialog with Unexpected kind and preserved raw message as detailsMessage`() {
        val unknown = AppError.Unknown(rawCode = "STRANGE_XYZ", rawMessage = "something weird")

        val event = unknown.toChatEvent()

        val blockingError = event as ChatEvent.BlockingErrorDialog
        assertEquals(BlockingErrorDialogKind.Unexpected, blockingError.kind)
        assertEquals("something weird", blockingError.detailsMessage)
    }

    // endregion
}
