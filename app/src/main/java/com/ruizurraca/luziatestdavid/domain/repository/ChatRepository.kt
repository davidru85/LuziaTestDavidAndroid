package com.ruizurraca.luziatestdavid.domain.repository

import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ChatRepository {

    suspend fun transcribeAudio(audio: File): Resource<String>

    fun streamChat(history: List<ChatMessage>): Flow<Resource<String>>

    suspend fun saveMessage(message: ChatMessage)

    suspend fun deleteMessage(id: String)

    fun observeConversation(): Flow<List<ChatMessage>>

    suspend fun clearConversation()
}
