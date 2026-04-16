package com.ruizurraca.luziatestdavid.presentation.model

import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import com.ruizurraca.luziatestdavid.domain.model.MessageStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatMessageUiMapperTest {

    private fun message(
        id: String = "m-1",
        role: MessageRole = MessageRole.USER,
        content: String = "hello",
        status: MessageStatus = MessageStatus.DELIVERED,
        timestamp: Long = 1_000L
    ) = ChatMessage(id = id, role = role, content = content, timestamp = timestamp, status = status)

    @Test
    fun `user PENDING maps to User with SENDING`() {
        val result = listOf(message(role = MessageRole.USER, status = MessageStatus.PENDING))
            .toUiModels()

        assertEquals(1, result.size)
        val user = result.single() as ChatMessageUiModel.User
        assertEquals(UserDeliveryState.SENDING, user.deliveryState)
        assertEquals("hello", user.content)
    }

    @Test
    fun `user DELIVERED maps to User with SENT`() {
        val result = listOf(message(role = MessageRole.USER, status = MessageStatus.DELIVERED))
            .toUiModels()

        val user = result.single() as ChatMessageUiModel.User
        assertEquals(UserDeliveryState.SENT, user.deliveryState)
    }

    @Test
    fun `user FAILED maps to User with FAILED`() {
        val result = listOf(message(role = MessageRole.USER, status = MessageStatus.FAILED))
            .toUiModels()

        val user = result.single() as ChatMessageUiModel.User
        assertEquals(UserDeliveryState.FAILED, user.deliveryState)
    }

    @Test
    fun `assistant PENDING with empty content maps to LOADING`() {
        val result = listOf(
            message(role = MessageRole.ASSISTANT, content = "", status = MessageStatus.PENDING)
        ).toUiModels()

        val assistant = result.single() as ChatMessageUiModel.Assistant
        assertEquals(AssistantStreamState.LOADING, assistant.streamState)
        assertEquals("", assistant.content)
    }

    @Test
    fun `assistant PENDING with partial content maps to STREAMING`() {
        val result = listOf(
            message(role = MessageRole.ASSISTANT, content = "Hel", status = MessageStatus.PENDING)
        ).toUiModels()

        val assistant = result.single() as ChatMessageUiModel.Assistant
        assertEquals(AssistantStreamState.STREAMING, assistant.streamState)
        assertEquals("Hel", assistant.content)
    }

    @Test
    fun `assistant DELIVERED maps to RECEIVED`() {
        val result = listOf(
            message(
                role = MessageRole.ASSISTANT,
                content = "Hello.",
                status = MessageStatus.DELIVERED
            )
        ).toUiModels()

        val assistant = result.single() as ChatMessageUiModel.Assistant
        assertEquals(AssistantStreamState.RECEIVED, assistant.streamState)
    }

    @Test
    fun `assistant FAILED maps to FAILED`() {
        val result = listOf(
            message(role = MessageRole.ASSISTANT, status = MessageStatus.FAILED)
        ).toUiModels()

        val assistant = result.single() as ChatMessageUiModel.Assistant
        assertEquals(AssistantStreamState.FAILED, assistant.streamState)
    }

    @Test
    fun `system messages are filtered out`() {
        val result = listOf(
            message(id = "sys", role = MessageRole.SYSTEM, content = "Pretend you are a poet."),
            message(id = "u", role = MessageRole.USER, content = "hi")
        ).toUiModels()

        assertEquals(1, result.size)
        assertEquals("u", result.single().id)
        assertTrue(result.single() is ChatMessageUiModel.User)
    }

    @Test
    fun `preserves id and timestamp on both sides`() {
        val result = listOf(
            message(id = "u1", role = MessageRole.USER, timestamp = 100L),
            message(id = "a1", role = MessageRole.ASSISTANT, timestamp = 200L)
        ).toUiModels()

        assertEquals("u1", result[0].id)
        assertEquals(100L, result[0].timestamp)
        assertEquals("a1", result[1].id)
        assertEquals(200L, result[1].timestamp)
    }

    @Test
    fun `preserves chronological order`() {
        val result = listOf(
            message(id = "a", timestamp = 1L),
            message(id = "b", timestamp = 2L),
            message(id = "c", timestamp = 3L)
        ).toUiModels()

        assertEquals(listOf("a", "b", "c"), result.map { it.id })
    }

    @Test
    fun `empty input maps to empty output`() {
        assertEquals(emptyList<ChatMessageUiModel>(), emptyList<ChatMessage>().toUiModels())
    }
}
