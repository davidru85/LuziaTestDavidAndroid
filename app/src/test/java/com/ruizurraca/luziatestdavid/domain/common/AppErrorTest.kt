package com.ruizurraca.luziatestdavid.domain.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppErrorTest {

    @Test
    fun `BadRequest default instance carries BAD_REQUEST code and human message`() {
        val error: AppError = AppError.BadRequest()

        assertEquals("BAD_REQUEST", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `FileTooLarge default instance carries FILE_TOO_LARGE code`() {
        val error: AppError = AppError.FileTooLarge()

        assertEquals("FILE_TOO_LARGE", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `Timeout default instance carries TIMEOUT code`() {
        val error: AppError = AppError.Timeout()

        assertEquals("TIMEOUT", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `Network default instance carries NETWORK_ERROR code`() {
        val error: AppError = AppError.Network()

        assertEquals("NETWORK_ERROR", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `ServiceUnavailable default instance carries SERVICE_UNAVAILABLE code`() {
        val error: AppError = AppError.ServiceUnavailable()

        assertEquals("SERVICE_UNAVAILABLE", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `Internal default instance carries INTERNAL_ERROR code`() {
        val error: AppError = AppError.Internal()

        assertEquals("INTERNAL_ERROR", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `Unknown preserves raw code and raw message`() {
        val error: AppError = AppError.Unknown(rawCode = "WEIRD_THING", rawMessage = "Something odd")

        assertEquals("WEIRD_THING", error.code)
        assertEquals("Something odd", error.message)
    }

    @Test
    fun `fromCode maps BAD_REQUEST to default BadRequest instance when no message is supplied`() {
        assertEquals(AppError.BadRequest(), AppError.fromCode("BAD_REQUEST"))
    }

    @Test
    fun `fromCode maps FILE_TOO_LARGE to default FileTooLarge instance when no message is supplied`() {
        assertEquals(AppError.FileTooLarge(), AppError.fromCode("FILE_TOO_LARGE"))
    }

    @Test
    fun `fromCode maps TIMEOUT to default Timeout instance when no message is supplied`() {
        assertEquals(AppError.Timeout(), AppError.fromCode("TIMEOUT"))
    }

    @Test
    fun `fromCode maps NETWORK_ERROR to default Network instance when no message is supplied`() {
        assertEquals(AppError.Network(), AppError.fromCode("NETWORK_ERROR"))
    }

    @Test
    fun `fromCode maps SERVICE_UNAVAILABLE to default ServiceUnavailable instance when no message is supplied`() {
        assertEquals(AppError.ServiceUnavailable(), AppError.fromCode("SERVICE_UNAVAILABLE"))
    }

    @Test
    fun `fromCode maps INTERNAL_ERROR to default Internal instance when no message is supplied`() {
        assertEquals(AppError.Internal(), AppError.fromCode("INTERNAL_ERROR"))
    }

    @Test
    fun `fromCode falls back to Unknown preserving original code and message`() {
        val error = AppError.fromCode(code = "CUSTOM_XYZ", message = "details here")

        assertTrue(error is AppError.Unknown)
        assertEquals("CUSTOM_XYZ", error.code)
        assertEquals("details here", error.message)
    }

    @Test
    fun `fromCode with unknown code and null message still produces Unknown with non-blank message`() {
        val error = AppError.fromCode(code = "MYSTERY", message = null)

        assertTrue(error is AppError.Unknown)
        assertEquals("MYSTERY", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `sealed hierarchy is exhaustive in when expression`() {
        val error: AppError = AppError.Network()

        val label: String = when (error) {
            is AppError.BadRequest -> "tier1"
            is AppError.FileTooLarge -> "tier1"
            is AppError.Timeout -> "tier2"
            is AppError.Network -> "tier2"
            is AppError.ServiceUnavailable -> "tier3"
            is AppError.Internal -> "tier3"
            is AppError.ValidationError -> "tier1"
            is AppError.Unknown -> "tier3"
            AppError.RecorderAlreadyRunning,
            AppError.RecorderNotActive,
            AppError.RecorderNoOutputFile,
            AppError.RecorderStartFailed,
            AppError.RecorderStopFailed,
            AppError.EmptyAudioFile,
            AppError.EmptyConversationHistory,
            AppError.StreamingFailed,
            AppError.UnexpectedFailure -> "tier1"
        }

        assertEquals("tier2", label)
    }

    // region Backend-message preservation (Phase 7.2.A, Fork 5) — the six fixed-message variants

    @Test
    fun `fromCode BAD_REQUEST with specific backend message preserves the message verbatim`() {
        val backendMessage = "Audio file is empty or too short to transcribe."

        val error = AppError.fromCode("BAD_REQUEST", backendMessage)

        assertTrue(error is AppError.BadRequest)
        assertEquals("BAD_REQUEST", error.code)
        assertEquals(backendMessage, error.message)
    }

    @Test
    fun `fromCode BAD_REQUEST with null message falls back to the generic default`() {
        val error = AppError.fromCode("BAD_REQUEST", message = null)

        assertTrue(error is AppError.BadRequest)
        assertEquals("BAD_REQUEST", error.code)
        assertTrue(error.message.isNotBlank())
        assertEquals("The request was invalid.", error.message)
    }

    @Test
    fun `fromCode BAD_REQUEST with blank message falls back to the generic default`() {
        val error = AppError.fromCode("BAD_REQUEST", message = "   ")

        assertTrue(error is AppError.BadRequest)
        assertEquals("The request was invalid.", error.message)
    }

    @Test
    fun `fromCode FILE_TOO_LARGE with specific backend message preserves the message verbatim`() {
        val backendMessage = "Audio file exceeds the 25 MB size limit."

        val error = AppError.fromCode("FILE_TOO_LARGE", backendMessage)

        assertTrue(error is AppError.FileTooLarge)
        assertEquals(backendMessage, error.message)
    }

    @Test
    fun `fromCode FILE_TOO_LARGE with null message falls back to the generic default`() {
        val error = AppError.fromCode("FILE_TOO_LARGE", message = null)

        assertTrue(error is AppError.FileTooLarge)
        assertEquals("The audio file is too large.", error.message)
    }

    @Test
    fun `fromCode TIMEOUT with specific backend message preserves the message verbatim`() {
        val backendMessage = "Upstream Whisper call timed out after 30 seconds."

        val error = AppError.fromCode("TIMEOUT", backendMessage)

        assertTrue(error is AppError.Timeout)
        assertEquals(backendMessage, error.message)
    }

    @Test
    fun `fromCode TIMEOUT with null message falls back to the generic default`() {
        val error = AppError.fromCode("TIMEOUT", message = null)

        assertTrue(error is AppError.Timeout)
        assertEquals("The request timed out.", error.message)
    }

    @Test
    fun `fromCode NETWORK_ERROR with specific backend message preserves the message verbatim`() {
        val backendMessage = "Upstream connection reset during streaming."

        val error = AppError.fromCode("NETWORK_ERROR", backendMessage)

        assertTrue(error is AppError.Network)
        assertEquals(backendMessage, error.message)
    }

    @Test
    fun `fromCode NETWORK_ERROR with null message falls back to the generic default`() {
        val error = AppError.fromCode("NETWORK_ERROR", message = null)

        assertTrue(error is AppError.Network)
        assertEquals("Network connection failed.", error.message)
    }

    @Test
    fun `fromCode SERVICE_UNAVAILABLE with specific backend message preserves the message verbatim`() {
        val backendMessage = "Backend is under maintenance until 18:00 UTC."

        val error = AppError.fromCode("SERVICE_UNAVAILABLE", backendMessage)

        assertTrue(error is AppError.ServiceUnavailable)
        assertEquals(backendMessage, error.message)
    }

    @Test
    fun `fromCode SERVICE_UNAVAILABLE with null message falls back to the generic default`() {
        val error = AppError.fromCode("SERVICE_UNAVAILABLE", message = null)

        assertTrue(error is AppError.ServiceUnavailable)
        assertEquals("The service is temporarily unavailable.", error.message)
    }

    @Test
    fun `fromCode INTERNAL_ERROR with specific backend message preserves the message verbatim`() {
        val backendMessage = "Transcription service failed. Please try again."

        val error = AppError.fromCode("INTERNAL_ERROR", backendMessage)

        assertTrue(error is AppError.Internal)
        assertEquals(backendMessage, error.message)
    }

    @Test
    fun `fromCode INTERNAL_ERROR with null message falls back to the generic default`() {
        val error = AppError.fromCode("INTERNAL_ERROR", message = null)

        assertTrue(error is AppError.Internal)
        assertEquals("An internal server error occurred.", error.message)
    }

    // endregion

    // region Local variants (Phase 7.3.3.H.1 — client-originated error paths)

    @Test
    fun `RecorderAlreadyRunning carries its local code and non-blank message`() {
        val error: AppError = AppError.RecorderAlreadyRunning
        assertEquals("LOCAL_RECORDER_ALREADY_RUNNING", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `RecorderNotActive carries its local code and non-blank message`() {
        val error: AppError = AppError.RecorderNotActive
        assertEquals("LOCAL_RECORDER_NOT_ACTIVE", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `RecorderNoOutputFile carries its local code and non-blank message`() {
        val error: AppError = AppError.RecorderNoOutputFile
        assertEquals("LOCAL_RECORDER_NO_OUTPUT_FILE", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `RecorderStartFailed carries its local code and non-blank message`() {
        val error: AppError = AppError.RecorderStartFailed
        assertEquals("LOCAL_RECORDER_START_FAILED", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `RecorderStopFailed carries its local code and non-blank message`() {
        val error: AppError = AppError.RecorderStopFailed
        assertEquals("LOCAL_RECORDER_STOP_FAILED", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `EmptyAudioFile carries its local code and non-blank message`() {
        val error: AppError = AppError.EmptyAudioFile
        assertEquals("LOCAL_EMPTY_AUDIO_FILE", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `EmptyConversationHistory carries its local code and non-blank message`() {
        val error: AppError = AppError.EmptyConversationHistory
        assertEquals("LOCAL_EMPTY_CONVERSATION_HISTORY", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `StreamingFailed carries its local code and non-blank message`() {
        val error: AppError = AppError.StreamingFailed
        assertEquals("LOCAL_STREAMING_FAILED", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `UnexpectedFailure carries its local code and non-blank message`() {
        val error: AppError = AppError.UnexpectedFailure
        assertEquals("LOCAL_UNEXPECTED_FAILURE", error.code)
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `toResourceError wraps the AppError in a Resource Error carrying the message`() {
        val err = AppError.RecorderAlreadyRunning
        val cause = RuntimeException("boom")

        val resource = err.toResourceError(cause)

        assertEquals(err.message, resource.message)
        assertSame(cause, resource.throwable)
        assertSame(err, resource.error)
    }

    @Test
    fun `toResourceError works without a throwable`() {
        val err = AppError.EmptyAudioFile

        val resource = err.toResourceError()

        assertEquals(err.message, resource.message)
        assertNull(resource.throwable)
        assertSame(err, resource.error)
    }

    // endregion

    // region ValidationError (Fork 4 addendum §6 — backend emits code=VALIDATION_ERROR for /chat)

    @Test
    fun `ValidationError carries VALIDATION_ERROR code and the backend-supplied message verbatim`() {
        val backendMessage = "user messages must include role_prompt."

        val error: AppError = AppError.ValidationError(rawMessage = backendMessage)

        assertEquals("VALIDATION_ERROR", error.code)
        assertEquals(backendMessage, error.message)
    }

    @Test
    fun `fromCode maps VALIDATION_ERROR to ValidationError preserving the backend message`() {
        val backendMessage = "role must be one of user|assistant|system."

        val error = AppError.fromCode(code = "VALIDATION_ERROR", message = backendMessage)

        assertTrue(error is AppError.ValidationError)
        assertEquals("VALIDATION_ERROR", error.code)
        assertEquals(backendMessage, error.message)
    }

    @Test
    fun `fromCode VALIDATION_ERROR with null or blank message falls back to non-blank default`() {
        val nullMessageError = AppError.fromCode("VALIDATION_ERROR", message = null)
        val blankMessageError = AppError.fromCode("VALIDATION_ERROR", message = "   ")

        assertTrue(nullMessageError is AppError.ValidationError)
        assertTrue(blankMessageError is AppError.ValidationError)
        assertTrue(nullMessageError.message.isNotBlank())
        assertTrue(blankMessageError.message.isNotBlank())
    }

    // endregion
}
