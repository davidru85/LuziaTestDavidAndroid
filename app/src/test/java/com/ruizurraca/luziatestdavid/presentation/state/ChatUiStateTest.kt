package com.ruizurraca.luziatestdavid.presentation.state

import com.ruizurraca.luziatestdavid.presentation.model.ChatMessageUiModel
import com.ruizurraca.luziatestdavid.presentation.model.UserDeliveryState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ChatUiStateTest {

    private val sampleMessages = listOf(
        ChatMessageUiModel.User(
            id = "u1",
            timestamp = 1L,
            content = "hi",
            deliveryState = UserDeliveryState.SENT
        )
    )

    @Test
    fun `Idle default has empty messages and empty draft`() {
        val state = ChatUiState.Idle()

        assertEquals(emptyList<ChatMessageUiModel>(), state.messages)
        assertEquals("", state.draft)
    }

    @Test
    fun `Idle can carry messages and draft`() {
        val state = ChatUiState.Idle(messages = sampleMessages, draft = "typing")

        assertSame(sampleMessages, state.messages)
        assertEquals("typing", state.draft)
    }

    @Test
    fun `Listening state exposes messages and draft`() {
        val state = ChatUiState.Listening(messages = sampleMessages, draft = "partial")

        assertSame(sampleMessages, state.messages)
        assertEquals("partial", state.draft)
    }

    @Test
    fun `Processing state exposes messages and draft`() {
        val state = ChatUiState.Processing(messages = sampleMessages, draft = "partial")

        assertSame(sampleMessages, state.messages)
        assertEquals("partial", state.draft)
    }

    @Test
    fun `Streaming state exposes messages and draft`() {
        val state = ChatUiState.Streaming(messages = sampleMessages, draft = "partial")

        assertSame(sampleMessages, state.messages)
        assertEquals("partial", state.draft)
    }

    @Test
    fun `all variants are ChatUiState`() {
        val states: List<ChatUiState> = listOf(
            ChatUiState.Idle(),
            ChatUiState.Listening(emptyList(), ""),
            ChatUiState.Processing(emptyList(), ""),
            ChatUiState.Streaming(emptyList(), "")
        )
        assertEquals(4, states.size)
    }
}
