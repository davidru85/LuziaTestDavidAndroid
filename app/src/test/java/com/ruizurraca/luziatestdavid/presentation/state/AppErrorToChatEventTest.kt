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
 * Phase 7.3.3.H.2: Tier-1 events no longer carry a pre-resolved `message`
 * String. They carry a semantic [Tier1Kind] (the composable resolves copy via
 * stringResource) plus an optional `backendMessage` that preempts the translated
 * copy whenever the backend supplied something specific (option iii). Tier-3
 * events continue the same pattern with [Tier3Kind] + `detailsMessage`.
 */
class AppErrorToChatEventTest {

    // region Tier-1 backend-facing variants — kind + optional backendMessage

    @Test
    fun `BadRequest default maps to Tier1 with BadRequest kind and null backendMessage`() {
        val event = AppError.BadRequest().toChatEvent()

        val tier1 = event as ChatEvent.Tier1
        assertEquals(Tier1Kind.BadRequest, tier1.kind)
        assertNull(tier1.backendMessage)
    }

    @Test
    fun `BadRequest with backend-supplied message routes that message through as backendMessage`() {
        val backendMessage = "Audio file is empty or too short to transcribe."
        val event = AppError.BadRequest(rawMessage = backendMessage).toChatEvent()

        val tier1 = event as ChatEvent.Tier1
        assertEquals(Tier1Kind.BadRequest, tier1.kind)
        assertEquals(backendMessage, tier1.backendMessage)
    }

    @Test
    fun `FileTooLarge maps to Tier1 with FileTooLarge kind`() {
        val event = AppError.FileTooLarge().toChatEvent()

        assertTrue(event is ChatEvent.Tier1)
        assertEquals(Tier1Kind.FileTooLarge, (event as ChatEvent.Tier1).kind)
    }

    @Test
    fun `Timeout maps to Tier1 with Timeout kind`() {
        val event = AppError.Timeout().toChatEvent()

        assertTrue(event is ChatEvent.Tier1)
        assertEquals(Tier1Kind.Timeout, (event as ChatEvent.Tier1).kind)
    }

    @Test
    fun `Network maps to Tier1 with Network kind`() {
        val event = AppError.Network().toChatEvent()

        assertTrue(event is ChatEvent.Tier1)
        assertEquals(Tier1Kind.Network, (event as ChatEvent.Tier1).kind)
    }

    @Test
    fun `ValidationError maps to Tier1 preserving backend message`() {
        val backendMessage = "user messages must include role_prompt."
        val event = AppError.ValidationError(rawMessage = backendMessage).toChatEvent()

        val tier1 = event as ChatEvent.Tier1
        assertEquals(Tier1Kind.ValidationError, tier1.kind)
        assertEquals(backendMessage, tier1.backendMessage)
    }

    // endregion

    // region Tier-1 local variants — never carry a backendMessage

    @Test
    fun `RecorderAlreadyRunning maps to Tier1 RecorderAlreadyRunning kind with no backendMessage`() {
        val event = AppError.RecorderAlreadyRunning.toChatEvent()

        val tier1 = event as ChatEvent.Tier1
        assertEquals(Tier1Kind.RecorderAlreadyRunning, tier1.kind)
        assertNull(tier1.backendMessage)
    }

    @Test
    fun `RecorderNotActive maps to Tier1 RecorderNotActive kind`() {
        val event = AppError.RecorderNotActive.toChatEvent()
        assertEquals(Tier1Kind.RecorderNotActive, (event as ChatEvent.Tier1).kind)
    }

    @Test
    fun `RecorderNoOutputFile maps to Tier1 RecorderNoOutputFile kind`() {
        val event = AppError.RecorderNoOutputFile.toChatEvent()
        assertEquals(Tier1Kind.RecorderNoOutputFile, (event as ChatEvent.Tier1).kind)
    }

    @Test
    fun `RecorderStartFailed maps to Tier1 RecorderStartFailed kind`() {
        val event = AppError.RecorderStartFailed.toChatEvent()
        assertEquals(Tier1Kind.RecorderStartFailed, (event as ChatEvent.Tier1).kind)
    }

    @Test
    fun `RecorderStopFailed maps to Tier1 RecorderStopFailed kind`() {
        val event = AppError.RecorderStopFailed.toChatEvent()
        assertEquals(Tier1Kind.RecorderStopFailed, (event as ChatEvent.Tier1).kind)
    }

    @Test
    fun `EmptyAudioFile maps to Tier1 EmptyAudioFile kind`() {
        val event = AppError.EmptyAudioFile.toChatEvent()
        assertEquals(Tier1Kind.EmptyAudioFile, (event as ChatEvent.Tier1).kind)
    }

    @Test
    fun `EmptyConversationHistory maps to Tier1 EmptyConversationHistory kind`() {
        val event = AppError.EmptyConversationHistory.toChatEvent()
        assertEquals(Tier1Kind.EmptyConversationHistory, (event as ChatEvent.Tier1).kind)
    }

    @Test
    fun `StreamingFailed maps to Tier1 StreamingFailed kind`() {
        val event = AppError.StreamingFailed.toChatEvent()
        assertEquals(Tier1Kind.StreamingFailed, (event as ChatEvent.Tier1).kind)
    }

    @Test
    fun `UnexpectedFailure maps to Tier1 UnexpectedFailure kind`() {
        val event = AppError.UnexpectedFailure.toChatEvent()
        assertEquals(Tier1Kind.UnexpectedFailure, (event as ChatEvent.Tier1).kind)
    }

    // endregion

    // region Tier-3 — kind + detailsMessage (unchanged from 7.3.3.C)

    @Test
    fun `ServiceUnavailable maps to Tier3 with ServiceUnavailable kind and backend detailsMessage`() {
        val error = AppError.ServiceUnavailable()
        val event = error.toChatEvent()

        val tier3 = event as ChatEvent.Tier3
        assertEquals(Tier3Kind.ServiceUnavailable, tier3.kind)
        assertEquals(error.message, tier3.detailsMessage)
    }

    @Test
    fun `Internal maps to Tier3 with InternalError kind`() {
        val event = AppError.Internal().toChatEvent()

        assertTrue(event is ChatEvent.Tier3)
        assertEquals(Tier3Kind.InternalError, (event as ChatEvent.Tier3).kind)
    }

    @Test
    fun `Internal with backend-supplied message maps to Tier3 preserving that message as detailsMessage`() {
        val backendMessage = "Transcription service failed. Please try again."
        val error = AppError.Internal(rawMessage = backendMessage)

        val event = error.toChatEvent()

        val tier3 = event as ChatEvent.Tier3
        assertEquals(Tier3Kind.InternalError, tier3.kind)
        assertEquals(backendMessage, tier3.detailsMessage)
    }

    @Test
    fun `Unknown maps to Tier3 with Unexpected kind and preserved raw message as detailsMessage`() {
        val unknown = AppError.Unknown(rawCode = "STRANGE_XYZ", rawMessage = "something weird")

        val event = unknown.toChatEvent()

        val tier3 = event as ChatEvent.Tier3
        assertEquals(Tier3Kind.Unexpected, tier3.kind)
        assertEquals("something weird", tier3.detailsMessage)
    }

    // endregion
}
