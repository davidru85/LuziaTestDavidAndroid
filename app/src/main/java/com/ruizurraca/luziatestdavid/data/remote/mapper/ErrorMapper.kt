package com.ruizurraca.luziatestdavid.data.remote.mapper

import com.ruizurraca.luziatestdavid.data.remote.dto.ApiErrorEnvelope
import com.ruizurraca.luziatestdavid.domain.common.AppError
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class ErrorMapper @Inject constructor(
    private val json: Json
) {

    suspend fun fromThrowable(throwable: Throwable): AppError = when (throwable) {
        is HttpRequestTimeoutException,
        is SocketTimeoutException -> AppError.Timeout()

        is UnknownHostException,
        is ConnectException -> AppError.Network()

        is ResponseException -> throwable.classify()

        is IOException -> AppError.Network()

        else -> AppError.UnexpectedFailure
    }

    private suspend fun ResponseException.classify(): AppError =
        readEnvelope()
            ?.let { AppError.fromCode(code = it.error.code, message = it.error.message) }
            ?: classifyByStatus()

    private suspend fun ResponseException.readEnvelope(): ApiErrorEnvelope? {
        val body = runCatching { response.bodyAsText() }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return runCatching { json.decodeFromString<ApiErrorEnvelope>(body) }.getOrNull()
    }

    private fun ResponseException.classifyByStatus(): AppError =
        when (val status = response.status.value) {
            400 -> AppError.BadRequest()
            413 -> AppError.FileTooLarge()
            503 -> AppError.ServiceUnavailable()
            in 500..599 -> AppError.Internal()
            else -> AppError.Unknown(
                rawCode = status.toString(),
                rawMessage = message.orFallback("HTTP $status")
            )
        }

    private fun String?.orFallback(fallback: String): String =
        this?.takeIf { it.isNotBlank() } ?: fallback
}
