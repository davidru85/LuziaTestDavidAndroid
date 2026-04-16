package com.ruizurraca.luziatestdavid.domain.usecase

import app.cash.turbine.test
import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import com.ruizurraca.luziatestdavid.domain.model.MessageStatus
import com.ruizurraca.luziatestdavid.domain.repository.ChatRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StreamAssistantReplyUseCaseTest {

    private val sendMessage: SendMessageUseCase = mockk()
    private val repository: ChatRepository = mockk()
    private val useCase = StreamAssistantReplyUseCase(sendMessage, repository)

    private val history = listOf(
        ChatMessage(
            id = "u1",
            role = MessageRole.USER,
            content = "hi",
            timestamp = 1L,
            status = MessageStatus.DELIVERED
        )
    )

    @Test
    fun `happy path persists placeholder, accumulates tokens, finalizes DELIVERED`() = runTest {
        every { sendMessage(history) } returns flowOf(
            Resource.Success("Hel"),
            Resource.Success("lo"),
            Resource.Success("!")
        )
        val saved = mutableListOf<ChatMessage>()
        coEvery { repository.saveMessage(capture(saved)) } just Runs

        useCase(history).test {
            assertTrue(awaitItem() is Resource.Success)
            assertTrue(awaitItem() is Resource.Success)
            assertTrue(awaitItem() is Resource.Success)
            awaitComplete()
        }

        assertEquals(5, saved.size)

        assertEquals(MessageRole.ASSISTANT, saved[0].role)
        assertEquals("", saved[0].content)
        assertEquals(MessageStatus.PENDING, saved[0].status)

        assertEquals("Hel", saved[1].content)
        assertEquals(MessageStatus.PENDING, saved[1].status)
        assertEquals("Hello", saved[2].content)
        assertEquals("Hello!", saved[3].content)

        assertEquals("Hello!", saved[4].content)
        assertEquals(MessageStatus.DELIVERED, saved[4].status)

        assertTrue(saved.all { it.id == saved[0].id })
        assertTrue(saved.all { it.timestamp == saved[0].timestamp })
    }

    @Test
    fun `error mid-stream marks FAILED with accumulated content and stops emitting`() = runTest {
        every { sendMessage(history) } returns flowOf(
            Resource.Success("Hel"),
            Resource.Error("network down"),
            Resource.Success("should be ignored")
        )
        val saved = mutableListOf<ChatMessage>()
        coEvery { repository.saveMessage(capture(saved)) } just Runs

        useCase(history).test {
            assertTrue(awaitItem() is Resource.Success)
            val err = awaitItem()
            assertTrue(err is Resource.Error)
            assertEquals("network down", (err as Resource.Error).message)
            awaitComplete()
        }

        assertEquals(3, saved.size)
        assertEquals(MessageStatus.PENDING, saved[0].status)
        assertEquals("Hel", saved[1].content)
        assertEquals(MessageStatus.PENDING, saved[1].status)
        assertEquals("Hel", saved[2].content)
        assertEquals(MessageStatus.FAILED, saved[2].status)
    }

    @Test
    fun `immediate error persists placeholder then FAILED with empty content`() = runTest {
        every { sendMessage(history) } returns flowOf(
            Resource.Error("server unavailable")
        )
        val saved = mutableListOf<ChatMessage>()
        coEvery { repository.saveMessage(capture(saved)) } just Runs

        useCase(history).test {
            assertTrue(awaitItem() is Resource.Error)
            awaitComplete()
        }

        assertEquals(2, saved.size)
        assertEquals(MessageStatus.PENDING, saved[0].status)
        assertEquals("", saved[0].content)
        assertEquals(MessageStatus.FAILED, saved[1].status)
        assertEquals("", saved[1].content)
    }

    @Test
    fun `empty stream finalizes DELIVERED with empty content`() = runTest {
        every { sendMessage(history) } returns flowOf()
        val saved = mutableListOf<ChatMessage>()
        coEvery { repository.saveMessage(capture(saved)) } just Runs

        useCase(history).test {
            awaitComplete()
        }

        assertEquals(2, saved.size)
        assertEquals(MessageStatus.PENDING, saved[0].status)
        assertEquals("", saved[0].content)
        assertEquals(MessageStatus.DELIVERED, saved[1].status)
        assertEquals("", saved[1].content)
    }
}
