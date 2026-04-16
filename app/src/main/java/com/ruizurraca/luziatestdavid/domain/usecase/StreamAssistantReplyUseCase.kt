package com.ruizurraca.luziatestdavid.domain.usecase

import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import com.ruizurraca.luziatestdavid.domain.model.MessageStatus
import com.ruizurraca.luziatestdavid.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject

class StreamAssistantReplyUseCase @Inject constructor(
    private val sendMessage: SendMessageUseCase,
    private val repository: ChatRepository
) {
    operator fun invoke(history: List<ChatMessage>): Flow<Resource<Unit>> = flow {
        val placeholder = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.ASSISTANT,
            content = "",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.PENDING
        )
        repository.saveMessage(placeholder)

        val buffer = StringBuilder()
        var errored = false

        sendMessage(history).collect { token ->
            if (errored) return@collect
            when (token) {
                is Resource.Success -> {
                    buffer.append(token.data)
                    repository.saveMessage(placeholder.copy(content = buffer.toString()))
                    emit(Resource.Success(Unit))
                }
                is Resource.Error -> {
                    errored = true
                    repository.saveMessage(
                        placeholder.copy(
                            content = buffer.toString(),
                            status = MessageStatus.FAILED
                        )
                    )
                    emit(Resource.Error(token.message, token.throwable))
                }
            }
        }

        if (!errored) {
            repository.saveMessage(
                placeholder.copy(
                    content = buffer.toString(),
                    status = MessageStatus.DELIVERED
                )
            )
        }
    }
}
