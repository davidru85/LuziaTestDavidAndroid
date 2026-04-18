package com.ruizurraca.luziatestdavid.data.remote.mapper

import com.ruizurraca.luziatestdavid.domain.common.AppError
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class ErrorMapper @Inject constructor() {

    fun fromThrowable(throwable: Throwable): AppError = when (throwable) {
        is HttpRequestTimeoutException,
        is SocketTimeoutException -> AppError.Timeout

        is UnknownHostException,
        is ConnectException -> AppError.Network

        is ClientRequestException -> when (throwable.response.status.value) {
            400 -> AppError.BadRequest
            413 -> AppError.FileTooLarge
            else -> AppError.Unknown(
                rawCode = throwable.response.status.value.toString(),
                rawMessage = throwable.message.orFallback("HTTP ${throwable.response.status.value}")
            )
        }

        is ServerResponseException -> when (throwable.response.status.value) {
            503 -> AppError.ServiceUnavailable
            else -> AppError.Internal
        }

        is IOException -> AppError.Network

        else -> AppError.Unknown(
            rawCode = throwable::class.simpleName ?: "UNKNOWN",
            rawMessage = throwable.message.orFallback("Unexpected failure.")
        )
    }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
