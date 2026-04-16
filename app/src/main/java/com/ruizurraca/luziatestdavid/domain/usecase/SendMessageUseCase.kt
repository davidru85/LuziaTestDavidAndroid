package com.ruizurraca.luziatestdavid.domain.usecase

import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(history: List<ChatMessage>): Flow<Resource<String>> {
        if (history.isEmpty()) {
            return flowOf(Resource.Error("Conversation history is empty."))
        }
        return flow {
            repository.streamChat(history).collect { emit(it) }
        }.catch { cause ->
            emit(Resource.Error(cause.message ?: "Streaming failed.", cause))
        }
    }
}
