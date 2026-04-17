package com.ruizurraca.luziatestdavid.domain.repository

import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Compile-time contract test: this file will not compile unless ChatRepository
 * declares the exact signatures required by the domain layer.
 */
class ChatRepositoryContractTest {

    private val fake = object : ChatRepository {
        override suspend fun transcribeAudio(audio: File): Resource<String> =
            Resource.Success("transcribed")

        override fun streamChat(history: List<ChatMessage>): Flow<Resource<String>> =
            flowOf(Resource.Success("token"))

        override suspend fun saveMessage(message: ChatMessage) = Unit

        override suspend fun deleteMessage(id: String) = Unit

        override fun observeConversation(): Flow<List<ChatMessage>> =
            flowOf(emptyList())

        override suspend fun clearConversation() = Unit
    }

    @Test
    fun `transcribeAudio returns Resource of String`() = runTest {
        val result = fake.transcribeAudio(File("fake.m4a"))
        assertTrue(result is Resource.Success)
        assertEquals("transcribed", (result as Resource.Success).data)
    }

    @Test
    fun `streamChat returns Flow of Resource of String`() = runTest {
        val history = listOf(
            ChatMessage("1", MessageRole.USER, "hi", 1L)
        )
        val emissions = fake.streamChat(history).toList()
        assertEquals(1, emissions.size)
        assertTrue(emissions.first() is Resource.Success)
    }

    @Test
    fun `observeConversation emits message lists`() = runTest {
        val emissions = fake.observeConversation().toList()
        assertEquals(listOf(emptyList<ChatMessage>()), emissions)
    }

    @Test
    fun `deleteMessage is a suspend function accepting a message id`() = runTest {
        fake.deleteMessage("msg-1")
    }
}
