package com.ruizurraca.luziatestdavid.domain.common

sealed class AppError(
    val code: String,
    val message: String
) {
    data object BadRequest : AppError(
        code = "BAD_REQUEST",
        message = "The request was invalid."
    )

    data object FileTooLarge : AppError(
        code = "FILE_TOO_LARGE",
        message = "The audio file is too large."
    )

    data object Timeout : AppError(
        code = "TIMEOUT",
        message = "The request timed out."
    )

    data object Network : AppError(
        code = "NETWORK_ERROR",
        message = "Network connection failed."
    )

    data object ServiceUnavailable : AppError(
        code = "SERVICE_UNAVAILABLE",
        message = "The service is temporarily unavailable."
    )

    data object Internal : AppError(
        code = "INTERNAL_ERROR",
        message = "An internal server error occurred."
    )

    data class ValidationError(
        val rawMessage: String
    ) : AppError(code = VALIDATION_ERROR_CODE, message = rawMessage)

    data class Unknown(
        val rawCode: String,
        val rawMessage: String
    ) : AppError(code = rawCode, message = rawMessage)

    companion object {
        private const val VALIDATION_ERROR_CODE = "VALIDATION_ERROR"

        fun fromCode(code: String, message: String? = null): AppError = when (code) {
            BadRequest.code -> BadRequest
            FileTooLarge.code -> FileTooLarge
            Timeout.code -> Timeout
            Network.code -> Network
            ServiceUnavailable.code -> ServiceUnavailable
            Internal.code -> Internal
            VALIDATION_ERROR_CODE -> ValidationError(
                rawMessage = message?.takeIf { it.isNotBlank() }
                    ?: "Validation error."
            )
            else -> Unknown(
                rawCode = code,
                rawMessage = message?.takeIf { it.isNotBlank() }
                    ?: "Unknown error (code=$code)."
            )
        }
    }
}
