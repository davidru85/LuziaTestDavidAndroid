package com.ruizurraca.luziatestdavid.data.repository

import app.cash.turbine.test
import com.ruizurraca.luziatestdavid.data.local.dao.ChatMessageDao
import com.ruizurraca.luziatestdavid.data.local.entity.ChatMessageEntity
import com.ruizurraca.luziatestdavid.data.remote.api.L1ApiClient
import com.ruizurraca.luziatestdavid.data.remote.dto.TranscribeResponseDto
import com.ruizurraca.luziatestdavid.data.remote.mapper.ChatMapper
import com.ruizurraca.luziatestdavid.data.remote.sse.SseParser
import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import com.ruizurraca.luziatestdavid.domain.model.MessageStatus
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
    private val dao: ChatMessageDao = mockk()
    private val dispatcher = UnconfinedTestDispatcher()

    private val repository = ChatRepositoryImpl(
        apiClient = apiClient,
        sseParser = sseParser,
        chatMapper = chatMapper,
        dao = dao,
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

    // region persistence

    @Test
    fun `saveMessage maps domain to entity and calls dao insert`() = runTest(dispatcher) {
        val message = ChatMessage(
            id = "u1",
            role = MessageRole.USER,
            content = "Hi",
            timestamp = 1_000L,
            status = MessageStatus.DELIVERED
        )
        coEvery { dao.insert(any()) } returns Unit

        repository.saveMessage(message)

        coVerify {
            dao.insert(
                ChatMessageEntity(
                    id = "u1",
                    role = "user",
                    content = "Hi",
                    timestamp = 1_000L,
                    status = "DELIVERED"
                )
            )
        }
    }

    @Test
    fun `observeConversation maps entities to domain preserving all roles and statuses`() = runTest(dispatcher) {
        val entities = listOf(
            ChatMessageEntity(
                id = "u",
                role = "user",
                content = "Hello",
                timestamp = 1L,
                status = "PENDING"
            ),
            ChatMessageEntity(
                id = "a",
                role = "assistant",
                content = "Hi!",
                timestamp = 2L,
                status = "FAILED"
            )
        )
        every { dao.observeAll() } returns flowOf(entities)

        repository.observeConversation().test {
            val messages = awaitItem()
            assertEquals(2, messages.size)

            assertEquals(MessageRole.USER, messages[0].role)
            assertEquals(MessageStatus.PENDING, messages[0].status)
            assertEquals("Hello", messages[0].content)

            assertEquals(MessageRole.ASSISTANT, messages[1].role)
            assertEquals(MessageStatus.FAILED, messages[1].status)

            awaitComplete()
        }
    }

    @Test
    fun `clearConversation delegates to dao deleteAll`() = runTest(dispatcher) {
        coEvery { dao.deleteAll() } returns Unit

        repository.clearConversation()

        coVerify { dao.deleteAll() }
    }

    @Test
    fun `deleteMessage delegates to dao delete with the given id`() = runTest(dispatcher) {
        coEvery { dao.delete("msg-42") } returns Unit

        repository.deleteMessage("msg-42")

        coVerify { dao.delete("msg-42") }
    }

    // ----- Per-message persona (Phase 5.5.B, MEMORY.md Fork 1) ----------------

    @Test
    fun `saveMessage persists personaPrompt for user messages`() = runTest(dispatcher) {
        val prompt = "You are a patient, educational tutor. " +
            "Explain concepts step by step and encourage learning."
        val message = ChatMessage(
            id = "u1",
            role = MessageRole.USER,
            content = "How does photosynthesis work?",
            timestamp = 1_000L,
            status = MessageStatus.DELIVERED,
            personaPrompt = prompt
        )
        coEvery { dao.insert(any()) } returns Unit

        repository.saveMessage(message)

        coVerify {
            dao.insert(
                ChatMessageEntity(
                    id = "u1",
                    role = "user",
                    content = "How does photosynthesis work?",
                    timestamp = 1_000L,
                    status = "DELIVERED",
                    personaPrompt = prompt
                )
            )
        }
    }

    @Test
    fun `observeConversation round-trips personaPrompt from entity to domain`() = runTest(dispatcher) {
        val tutor = "You are a patient, educational tutor."
        val artist = "You are a creative artist."

        val entities = listOf(
            ChatMessageEntity(
                id = "u1",
                role = "user",
                content = "Explain gravity",
                timestamp = 1L,
                status = "DELIVERED",
                personaPrompt = tutor
            ),
            ChatMessageEntity(
                id = "a1",
                role = "assistant",
                content = "Gravity is…",
                timestamp = 2L,
                status = "DELIVERED",
                personaPrompt = null
            ),
            ChatMessageEntity(
                id = "u2",
                role = "user",
                content = "Now as a poem",
                timestamp = 3L,
                status = "DELIVERED",
                personaPrompt = artist
            )
        )
        every { dao.observeAll() } returns flowOf(entities)

        repository.observeConversation().test {
            val messages = awaitItem()
            assertEquals(3, messages.size)
            assertEquals(tutor, messages[0].personaPrompt)
            assertEquals(null, messages[1].personaPrompt)
            assertEquals(artist, messages[2].personaPrompt)
            awaitComplete()
        }
    }

    // endregion
}
