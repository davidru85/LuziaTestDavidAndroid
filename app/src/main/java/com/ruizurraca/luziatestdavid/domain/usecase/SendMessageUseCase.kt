package com.ruizurraca.luziatestdavid.domain.usecase

import com.ruizurraca.luziatestdavid.domain.common.AppError
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
            return flowOf(AppError.EmptyConversationHistory.toResourceError())
        }
        return flow {
            repository.streamChat(history).collect { emit(it) }
        }.catch { cause ->
            emit(AppError.StreamingFailed.toResourceError(cause))
        }
    }
}
