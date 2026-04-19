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
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val mapper = ErrorMapper(json = json)

    // region Timeout family

    @Test
    fun `HttpRequestTimeoutException maps to Timeout`() = runTest {
        val exception = captureHttpTimeout()

        assertEquals(AppError.Timeout(), mapper.fromThrowable(exception))
    }

    @Test
    fun `SocketTimeoutException maps to Timeout`() = runTest {
        val exception = SocketTimeoutException("read timed out")

        assertEquals(AppError.Timeout(), mapper.fromThrowable(exception))
    }

    // endregion

    // region Network family

    @Test
    fun `UnknownHostException maps to Network`() = runTest {
        val exception = UnknownHostException("nodename nor servname provided")

        assertEquals(AppError.Network(), mapper.fromThrowable(exception))
    }

    @Test
    fun `ConnectException maps to Network`() = runTest {
        val exception = ConnectException("connection refused")

        assertEquals(AppError.Network(), mapper.fromThrowable(exception))
    }

    @Test
    fun `generic IOException maps to Network`() = runTest {
        val exception = IOException("socket reset")

        assertEquals(AppError.Network(), mapper.fromThrowable(exception))
    }

    // endregion

    // region 4xx ClientRequestException

    @Test
    fun `ClientRequestException with status 400 maps to BadRequest`() = runTest {
        val exception = captureHttpException(HttpStatusCode.BadRequest)

        assertTrue(exception is ClientRequestException)
        assertEquals(AppError.BadRequest(), mapper.fromThrowable(exception))
    }

    @Test
    fun `ClientRequestException with status 413 maps to FileTooLarge`() = runTest {
        val exception = captureHttpException(HttpStatusCode.PayloadTooLarge)

        assertTrue(exception is ClientRequestException)
        assertEquals(AppError.FileTooLarge(), mapper.fromThrowable(exception))
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
        assertEquals(AppError.ServiceUnavailable(), mapper.fromThrowable(exception))
    }

    @Test
    fun `ServerResponseException with status 500 maps to Internal`() = runTest {
        val exception = captureHttpException(HttpStatusCode.InternalServerError)

        assertTrue(exception is ServerResponseException)
        assertEquals(AppError.Internal(), mapper.fromThrowable(exception))
    }

    @Test
    fun `ServerResponseException with status 502 maps to Internal`() = runTest {
        val exception = captureHttpException(HttpStatusCode.BadGateway)

        assertTrue(exception is ServerResponseException)
        assertEquals(AppError.Internal(), mapper.fromThrowable(exception))
    }

    // endregion

    // region Unclassified fallback (Phase 7.3.3.H.1 — maps to UnexpectedFailure local
    // variant so the presentation layer can resolve a translated string; Unknown is
    // reserved for unknown backend response-body codes)

    @Test
    fun `unclassified Throwable falls back to UnexpectedFailure local AppError`() = runTest {
        val exception = RuntimeException("weird state")

        val result = mapper.fromThrowable(exception)

        assertEquals(AppError.UnexpectedFailure, result)
    }

    @Test
    fun `unclassified Throwable with null message still falls back to UnexpectedFailure`() = runTest {
        val exception = RuntimeException()

        val result = mapper.fromThrowable(exception)

        assertEquals(AppError.UnexpectedFailure, result)
    }

    // endregion

    // region Body envelope parsing (Fork 4 addendum §6 — 7.1.3.D)

    @Test
    fun `422 with VALIDATION_ERROR envelope maps to ValidationError preserving backend message`() = runTest {
        val backendMessage = "user messages must include role_prompt."
        val body = """{"error":{"code":"VALIDATION_ERROR","message":"$backendMessage"}}"""
        val exception = captureHttpExceptionWithBody(HttpStatusCode.UnprocessableEntity, body)

        val result = mapper.fromThrowable(exception)

        assertTrue(result is AppError.ValidationError) { "expected ValidationError, got $result" }
        assertEquals(backendMessage, result.message)
    }

    @Test
    fun `4xx parseable envelope takes precedence over status-based mapping`() {
        runTest {
            // Status 400 would normally map to BadRequest, but the envelope says SERVICE_UNAVAILABLE.
            // Body is authoritative.
            val body = """{"error":{"code":"SERVICE_UNAVAILABLE","message":"down"}}"""
            val exception = captureHttpExceptionWithBody(HttpStatusCode.BadRequest, body)

            val result = mapper.fromThrowable(exception)

            // Body says SERVICE_UNAVAILABLE with message "down" — preserved on the variant.
            assertEquals(AppError.ServiceUnavailable(rawMessage = "down"), result)
        }
    }

    @Test
    fun `4xx with unparseable body falls back to status-based mapping`() = runTest {
        val exception = captureHttpExceptionWithBody(HttpStatusCode.BadRequest, "this is not json")

        val result = mapper.fromThrowable(exception)

        assertEquals(AppError.BadRequest(), result)
    }

    @Test
    fun `5xx with VALIDATION_ERROR envelope maps to ValidationError (body takes precedence)`() = runTest {
        val body = """{"error":{"code":"VALIDATION_ERROR","message":"late validation"}}"""
        val exception = captureHttpExceptionWithBody(HttpStatusCode.InternalServerError, body)

        val result = mapper.fromThrowable(exception)

        assertTrue(result is AppError.ValidationError) { "expected ValidationError, got $result" }
        assertEquals("late validation", result.message)
    }

    @Test
    fun `4xx with empty body falls back to status-based mapping (regression guard for existing behaviour)`() = runTest {
        val exception = captureHttpException(HttpStatusCode.PayloadTooLarge)

        val result = mapper.fromThrowable(exception)

        assertEquals(AppError.FileTooLarge(), result)
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
     * Same as [captureHttpException] but with an explicit JSON response body —
     * used to exercise the backend-envelope parsing path in [ErrorMapper].
     */
    private suspend fun captureHttpExceptionWithBody(
        status: HttpStatusCode,
        body: String
    ): ResponseException {
        val client = HttpClient(
            MockEngine {
                respond(
                    content = body,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        ) {
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
