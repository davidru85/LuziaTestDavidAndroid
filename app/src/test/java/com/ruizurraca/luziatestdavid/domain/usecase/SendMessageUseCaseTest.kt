package com.ruizurraca.luziatestdavid.domain.usecase

import app.cash.turbine.test
import com.ruizurraca.luziatestdavid.domain.common.AppError
import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import com.ruizurraca.luziatestdavid.domain.repository.ChatRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SendMessageUseCaseTest {

    private val repository: ChatRepository = mockk()
    private val useCase = SendMessageUseCase(repository)

    private val history = listOf(
        ChatMessage("u0", MessageRole.USER, "Context message", 0L),
        ChatMessage("u1", MessageRole.USER, "Hi", 1L)
    )

    @Test
    fun `invoke forwards full history to repository streamChat`() = runTest {
        every { repository.streamChat(history) } returns flowOf(Resource.Success("ok"))

        useCase(history).test {
            awaitItem()
            awaitComplete()
        }

        verify(exactly = 1) { repository.streamChat(history) }
    }

    @Test
    fun `invoke emits tokens in order then completes on DONE`() = runTest {
        every { repository.streamChat(history) } returns flowOf(
            Resource.Success("Hello"),
            Resource.Success(", "),
            Resource.Success("world")
        )

        useCase(history).test {
            assertEquals("Hello", (awaitItem() as Resource.Success).data)
            assertEquals(", ", (awaitItem() as Resource.Success).data)
            assertEquals("world", (awaitItem() as Resource.Success).data)
            awaitComplete()
        }
    }

    @Test
    fun `invoke propagates Error emission from repository`() = runTest {
        every { repository.streamChat(history) } returns flowOf(
            Resource.Success("partial"),
            Resource.Error("stream failed")
        )

        useCase(history).test {
            assertTrue(awaitItem() is Resource.Success)
            val err = awaitItem()
            assertTrue(err is Resource.Error)
            assertEquals("stream failed", (err as Resource.Error).message)
            awaitComplete()
        }
    }

    @Test
    fun `invoke wraps upstream exceptions as Resource Error carrying StreamingFailed AppError`() = runTest {
        every { repository.streamChat(history) } returns flow {
            emit(Resource.Success("partial"))
            throw RuntimeException("socket reset")
        }

        useCase(history).test {
            assertTrue(awaitItem() is Resource.Success)
            val err = awaitItem()
            assertTrue(err is Resource.Error)
            assertEquals(AppError.StreamingFailed, (err as Resource.Error).error)
            awaitComplete()
        }
    }

    @Test
    fun `invoke rejects empty history with EmptyConversationHistory AppError`() = runTest {
        useCase(emptyList()).test {
            val emission = awaitItem()
            assertTrue(emission is Resource.Error)
            assertEquals(AppError.EmptyConversationHistory, (emission as Resource.Error).error)
            awaitComplete()
        }

        verify(exactly = 0) { repository.streamChat(any()) }
    }
}
