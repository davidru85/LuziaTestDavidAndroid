package com.ruizurraca.luziatestdavid.data.repository

import app.cash.turbine.test
import com.ruizurraca.luziatestdavid.data.remote.api.L1ApiClient
import com.ruizurraca.luziatestdavid.data.remote.dto.TranscribeResponseDto
import com.ruizurraca.luziatestdavid.data.remote.mapper.ChatMapper
import com.ruizurraca.luziatestdavid.data.remote.sse.SseParser
import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException

/**
 * RED-phase integration tests for [ChatRepositoryImpl], the orchestrator that wires
 * [L1ApiClient] + [SseParser] + [ChatMapper] behind the [ChatRepository] domain
 * interface, wrapping thrown exceptions in `Resource.Error` per the 3-Tier Error
 * Strategy (TECHNICAL_SPEC §Error Handling).
 *
 * Strategy (per TECHNICAL_SPEC §Testing §Integration): mock only [L1ApiClient] (the
 * I/O boundary); use the real [SseParser] and [ChatMapper] so the test exercises
 * the full pipeline rather than a chain of stubs.
 *
 * Scope: only `transcribeAudio` and `streamChat`. The persistence methods
 * (`saveMessage`, `observeConversation`, `clearConversation`) are covered in the
 * follow-up 4.1.d.2 cycle once the Room DAO is defined.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryImplTest {

    private val apiClient: L1ApiClient = mockk()
    private val sseParser = SseParser()
    private val chatMapper = ChatMapper()
    private val dispatcher = UnconfinedTestDispatcher()

    private val repository = ChatRepositoryImpl(
        apiClient = apiClient,
        sseParser = sseParser,
        chatMapper = chatMapper,
        ioDispatcher = dispatcher
    )

    // region transcribeAudio

    @Test
    fun `transcribeAudio reads file bytes posts via api and returns Success`() = runTest(dispatcher) {
        val audioBytes = byteArrayOf(0x1, 0x2, 0x3)
        val audioFile = File.createTempFile("audio", ".m4a").apply {
            writeBytes(audioBytes)
            deleteOnExit()
        }
        coEvery { apiClient.transcribe(any(), any()) } returns TranscribeResponseDto("hello world")

        val result = repository.transcribeAudio(audioFile)

        assertEquals(Resource.Success("hello world"), result)
        coVerify { apiClient.transcribe(match { it.contentEquals(audioBytes) }, any()) }
    }

    @Test
    fun `transcribeAudio wraps thrown exception as Resource Error`() = runTest(dispatcher) {
        val audioFile = File.createTempFile("audio", ".m4a").apply {
            writeBytes(byteArrayOf(0x1))
            deleteOnExit()
        }
        coEvery { apiClient.transcribe(any(), any()) } throws IOException("socket reset")

        val result = repository.transcribeAudio(audioFile)

        assertTrue(result is Resource.Error)
    }

    // endregion

    // region streamChat

    private val userHistory = listOf(
        ChatMessage("u1", MessageRole.USER, "Hi", 1L)
    )

    @Test
    fun `streamChat emits Resource Success for each SSE Token`() = runTest(dispatcher) {
        every { apiClient.streamChat(any()) } returns flowOf(
            "data: Hello",
            "",
            "data: , world",
            "",
            "data: [DONE]",
            ""
        )

        repository.streamChat(userHistory).test {
            assertEquals(Resource.Success("Hello"), awaitItem())
            assertEquals(Resource.Success(", world"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `streamChat emits Resource Error for SSE error event`() = runTest(dispatcher) {
        every { apiClient.streamChat(any()) } returns flowOf(
            "event: error",
            "data: {\"code\":\"INTERNAL_ERROR\",\"message\":\"boom\"}",
            ""
        )

        repository.streamChat(userHistory).test {
            val emission = awaitItem()
            assertTrue(emission is Resource.Error) { "expected Error, got $emission" }
            awaitComplete()
        }
    }

    @Test
    fun `streamChat emits Resource Error when transport throws mid-stream`() = runTest(dispatcher) {
        every { apiClient.streamChat(any()) } returns flow {
            emit("data: partial")
            emit("")
            throw IOException("connection reset")
        }

        repository.streamChat(userHistory).test {
            assertEquals(Resource.Success("partial"), awaitItem())
            val tail = awaitItem()
            assertTrue(tail is Resource.Error) { "expected Error, got $tail" }
            awaitComplete()
        }
    }

    // endregion
}
