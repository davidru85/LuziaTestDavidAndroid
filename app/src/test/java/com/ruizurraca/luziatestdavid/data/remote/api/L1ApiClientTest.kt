package com.ruizurraca.luziatestdavid.data.remote.api

import app.cash.turbine.test
import com.ruizurraca.luziatestdavid.data.remote.dto.ChatMessageDto
import com.ruizurraca.luziatestdavid.data.remote.dto.ChatRequestDto
import com.ruizurraca.luziatestdavid.data.remote.dto.TranscribeResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * RED-phase contract tests for [L1ApiClient], the thin Ktor transport wrapper over
 * the backend API defined in TECHNICAL_SPEC §API.
 *
 * Backed by Ktor's [MockEngine] — no real network involved.
 *
 * Contract:
 *   - `transcribe(audio)`: POST multipart to `/transcribe`, returns [TranscribeResponseDto].
 *   - `streamChat(dto)`: POST JSON to `/chat`, returns `Flow<String>` of raw SSE lines
 *     (blank separators included) for [SseParser] to consume.
 *   - Both throw on non-2xx (Ktor `expectSuccess = true`). The repository layer wraps
 *     thrown exceptions into `Resource.Error`.
 */
class L1ApiClientTest {

    private fun clientWith(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData
    ): L1ApiClient {
        val httpClient = HttpClient(MockEngine(handler)) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            defaultRequest {
                url("http://test/")
            }
        }
        return L1ApiClient(httpClient)
    }

    // region /transcribe

    @Test
    fun `transcribe posts multipart to transcribe endpoint and decodes response`() = runTest {
        val apiClient = clientWith { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/transcribe", request.url.encodedPath)

            val contentType = request.body.contentType
            assertNotNull(contentType)
            assertEquals("multipart", contentType!!.contentType)
            assertEquals("form-data", contentType.contentSubtype)

            respond(
                content = """{"text":"hello world"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val result = apiClient.transcribe(audio = byteArrayOf(0x1, 0x2, 0x3))

        assertEquals(TranscribeResponseDto(text = "hello world"), result)
    }

    @Test
    fun `transcribe throws on non-2xx response`() = runTest {
        val apiClient = clientWith {
            respond(
                content = """{"error":{"code":"INTERNAL_ERROR","message":"boom"}}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        assertThrows<ResponseException> {
            apiClient.transcribe(audio = byteArrayOf(0x1))
        }
    }

    // endregion

    // region /chat (SSE)

    private val sampleRequest = ChatRequestDto(
        messages = listOf(ChatMessageDto(role = "user", content = "Hi"))
    )

    @Test
    fun `streamChat posts JSON body to chat endpoint`() = runTest {
        val apiClient = clientWith { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/chat", request.url.encodedPath)
            assertEquals(ContentType.Application.Json, request.body.contentType)

            respond(
                content = "data: [DONE]\n\n",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream")
            )
        }

        apiClient.streamChat(sampleRequest).toList()
    }

    @Test
    fun `streamChat emits each SSE line including blank separators`() = runTest {
        val sseBody = "data: hello\n\ndata: world\n\ndata: [DONE]\n\n"
        val apiClient = clientWith {
            respond(
                content = sseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream")
            )
        }

        apiClient.streamChat(sampleRequest).test {
            assertEquals("data: hello", awaitItem())
            assertEquals("", awaitItem())
            assertEquals("data: world", awaitItem())
            assertEquals("", awaitItem())
            assertEquals("data: [DONE]", awaitItem())
            assertEquals("", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `streamChat throws on non-2xx response`() = runTest {
        val apiClient = clientWith {
            respond(
                content = """{"error":{"code":"INTERNAL_ERROR","message":"boom"}}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val error = assertThrows<ResponseException> {
            apiClient.streamChat(sampleRequest).toList()
        }
        assertTrue(error.response.status == HttpStatusCode.InternalServerError)
    }

    // endregion
}
