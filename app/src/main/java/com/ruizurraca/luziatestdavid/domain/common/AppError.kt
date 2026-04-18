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
