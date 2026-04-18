package com.ruizurraca.luziatestdavid.data.remote.mapper

import com.ruizurraca.luziatestdavid.domain.common.AppError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * RED-phase contract tests for [ErrorMapper]: a pure `Throwable -> AppError`
 * classifier that maps Ktor/IO failures onto the domain error tiers defined in
 * `TECHNICAL_SPEC.md §Error Handling`.
 *
 * Ktor HTTP exceptions are produced via `MockEngine` so tests exercise real
 * [ClientRequestException] / [ServerResponseException] instances rather than
 * mocked surrogates.
 */
class ErrorMapperTest {

    private val mapper = ErrorMapper()

    // region Timeout family

    @Test
    fun `HttpRequestTimeoutException maps to Timeout`() = runTest {
        val exception = captureHttpTimeout()

        assertSame(AppError.Timeout, mapper.fromThrowable(exception))
    }

    @Test
    fun `SocketTimeoutException maps to Timeout`() {
        val exception = SocketTimeoutException("read timed out")

        assertSame(AppError.Timeout, mapper.fromThrowable(exception))
    }

    // endregion

    // region Network family

    @Test
    fun `UnknownHostException maps to Network`() {
        val exception = UnknownHostException("nodename nor servname provided")

        assertSame(AppError.Network, mapper.fromThrowable(exception))
    }

    @Test
    fun `ConnectException maps to Network`() {
        val exception = ConnectException("connection refused")

        assertSame(AppError.Network, mapper.fromThrowable(exception))
    }

    @Test
    fun `generic IOException maps to Network`() {
        val exception = IOException("socket reset")

        assertSame(AppError.Network, mapper.fromThrowable(exception))
    }

    // endregion

    // region 4xx ClientRequestException

    @Test
    fun `ClientRequestException with status 400 maps to BadRequest`() = runTest {
        val exception = captureHttpException(HttpStatusCode.BadRequest)

        assertTrue(exception is ClientRequestException)
        assertSame(AppError.BadRequest, mapper.fromThrowable(exception))
    }

    @Test
    fun `ClientRequestException with status 413 maps to FileTooLarge`() = runTest {
        val exception = captureHttpException(HttpStatusCode.PayloadTooLarge)

        assertTrue(exception is ClientRequestException)
        assertSame(AppError.FileTooLarge, mapper.fromThrowable(exception))
    }

    @Test
    fun `ClientRequestException with unclassified 4xx status maps to Unknown carrying the status code`() = runTest {
        val exception = captureHttpException(HttpStatusCode.Forbidden)

        val result = mapper.fromThrowable(exception)

        assertTrue(result is AppError.Unknown)
        assertEquals("403", result.code)
        assertTrue(result.message.isNotBlank())
    }

    // endregion

    // region 5xx ServerResponseException

    @Test
    fun `ServerResponseException with status 503 maps to ServiceUnavailable`() = runTest {
        val exception = captureHttpException(HttpStatusCode.ServiceUnavailable)

        assertTrue(exception is ServerResponseException)
        assertSame(AppError.ServiceUnavailable, mapper.fromThrowable(exception))
    }

    @Test
    fun `ServerResponseException with status 500 maps to Internal`() = runTest {
        val exception = captureHttpException(HttpStatusCode.InternalServerError)

        assertTrue(exception is ServerResponseException)
        assertSame(AppError.Internal, mapper.fromThrowable(exception))
    }

    @Test
    fun `ServerResponseException with status 502 maps to Internal`() = runTest {
        val exception = captureHttpException(HttpStatusCode.BadGateway)

        assertTrue(exception is ServerResponseException)
        assertSame(AppError.Internal, mapper.fromThrowable(exception))
    }

    // endregion

    // region Unknown fallback

    @Test
    fun `unclassified Throwable falls back to Unknown preserving class name and message`() {
        val exception = RuntimeException("weird state")

        val result = mapper.fromThrowable(exception)

        assertTrue(result is AppError.Unknown)
        assertEquals("RuntimeException", result.code)
        assertEquals("weird state", result.message)
    }

    @Test
    fun `unclassified Throwable with null message still maps to Unknown with non-blank message`() {
        val exception = RuntimeException()

        val result = mapper.fromThrowable(exception)

        assertTrue(result is AppError.Unknown)
        assertTrue(result.message.isNotBlank())
    }

    // endregion

    // region helpers

    /**
     * Produce a real Ktor [ResponseException] subclass instance for the given
     * status code by round-tripping a request through a [MockEngine].
     */
    private suspend fun captureHttpException(status: HttpStatusCode): ResponseException {
        val client = HttpClient(MockEngine { respond(content = "", status = status) }) {
            expectSuccess = true
        }
        return try {
            client.get("http://test/")
            error("Expected ResponseException for status $status")
        } catch (e: ResponseException) {
            e
        } finally {
            client.close()
        }
    }

    /**
     * Produce a real [HttpRequestTimeoutException] by configuring a request
     * timeout shorter than the mock engine's artificial delay.
     */
    private suspend fun captureHttpTimeout(): HttpRequestTimeoutException {
        val client = HttpClient(
            MockEngine {
                delay(200)
                respond(content = "")
            }
        ) {
            expectSuccess = true
            install(HttpTimeout) {
                requestTimeoutMillis = 10
            }
        }
        return try {
            client.get("http://test/")
            error("Expected HttpRequestTimeoutException")
        } catch (e: HttpRequestTimeoutException) {
            e
        } finally {
            client.close()
        }
    }

    // endregion
}
