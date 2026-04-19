package com.ruizurraca.luziatestdavid.domain.common

sealed class AppError(
    val code: String,
    val message: String
) {
    data class BadRequest(
        val rawMessage: String = "The request was invalid."
    ) : AppError(code = "BAD_REQUEST", message = rawMessage)

    data class FileTooLarge(
        val rawMessage: String = "The audio file is too large."
    ) : AppError(code = "FILE_TOO_LARGE", message = rawMessage)

    data class Timeout(
        val rawMessage: String = "The request timed out."
    ) : AppError(code = "TIMEOUT", message = rawMessage)

    data class Network(
        val rawMessage: String = "Network connection failed."
    ) : AppError(code = "NETWORK_ERROR", message = rawMessage)

    data class ServiceUnavailable(
        val rawMessage: String = "The service is temporarily unavailable."
    ) : AppError(code = "SERVICE_UNAVAILABLE", message = rawMessage)

    data class Internal(
        val rawMessage: String = "An internal server error occurred."
    ) : AppError(code = "INTERNAL_ERROR", message = rawMessage)

    data class ValidationError(
        val rawMessage: String
    ) : AppError(code = VALIDATION_ERROR_CODE, message = rawMessage)

    data class Unknown(
        val rawCode: String,
        val rawMessage: String
    ) : AppError(code = rawCode, message = rawMessage)

    // Local / client-originated error paths (Phase 7.3.3.H.1).
    // The `message` defaults below are dev-facing fallbacks kept for logs and
    // for backward compat with `Resource.Error.message`; the user-facing copy
    // is resolved via `stringResource` at the presentation layer in 7.3.3.H.2.

    data object RecorderAlreadyRunning : AppError(
        code = "LOCAL_RECORDER_ALREADY_RUNNING",
        message = "Recording already in progress."
    )

    data object RecorderNotActive : AppError(
        code = "LOCAL_RECORDER_NOT_ACTIVE",
        message = "No active recording."
    )

    data object RecorderNoOutputFile : AppError(
        code = "LOCAL_RECORDER_NO_OUTPUT_FILE",
        message = "No output file for recording."
    )

    data object RecorderStartFailed : AppError(
        code = "LOCAL_RECORDER_START_FAILED",
        message = "Failed to start recording."
    )

    data object RecorderStopFailed : AppError(
        code = "LOCAL_RECORDER_STOP_FAILED",
        message = "Failed to stop recording."
    )

    data object EmptyAudioFile : AppError(
        code = "LOCAL_EMPTY_AUDIO_FILE",
        message = "Audio file is missing or empty."
    )

    data object EmptyConversationHistory : AppError(
        code = "LOCAL_EMPTY_CONVERSATION_HISTORY",
        message = "Conversation history is empty."
    )

    data object StreamingFailed : AppError(
        code = "LOCAL_STREAMING_FAILED",
        message = "Streaming failed."
    )

    data object UnexpectedFailure : AppError(
        code = "LOCAL_UNEXPECTED_FAILURE",
        message = "Unexpected failure."
    )

    data object TtsUnavailable : AppError(
        code = "LOCAL_TTS_UNAVAILABLE",
        message = "Text-to-speech is unavailable."
    )

    /**
     * Wraps this [AppError] into a [Resource.Error]. The `message` field is populated
     * with the AppError's default (dev-facing); user-facing copy resolves to a
     * `stringResource` at the presentation layer via [TransientSnackbarKind] /
     * [BlockingErrorDialogKind].
     */
    fun toResourceError(throwable: Throwable? = null): Resource.Error = Resource.Error(
        message = message,
        throwable = throwable,
        error = this
    )

    companion object {
        private const val VALIDATION_ERROR_CODE = "VALIDATION_ERROR"

        fun fromCode(code: String, message: String? = null): AppError {
            val preserved = message?.takeIf { it.isNotBlank() }
            return when (code) {
                "BAD_REQUEST" -> preserved?.let { BadRequest(it) } ?: BadRequest()
                "FILE_TOO_LARGE" -> preserved?.let { FileTooLarge(it) } ?: FileTooLarge()
                "TIMEOUT" -> preserved?.let { Timeout(it) } ?: Timeout()
                "NETWORK_ERROR" -> preserved?.let { Network(it) } ?: Network()
                "SERVICE_UNAVAILABLE" -> preserved?.let { ServiceUnavailable(it) } ?: ServiceUnavailable()
                "INTERNAL_ERROR" -> preserved?.let { Internal(it) } ?: Internal()
                VALIDATION_ERROR_CODE -> ValidationError(rawMessage = preserved ?: "Validation error.")
                else -> Unknown(
                    rawCode = code,
                    rawMessage = preserved ?: "Unknown error (code=$code)."
                )
            }
        }
    }
}
