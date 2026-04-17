package com.ruizurraca.luziatestdavid.data.remote.mapper

import com.ruizurraca.luziatestdavid.data.remote.dto.ChatMessageDto
import com.ruizurraca.luziatestdavid.data.remote.dto.ChatRequestDto
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import javax.inject.Inject

class ChatMapper @Inject constructor() {

    fun toRequestDto(messages: List<ChatMessage>): ChatRequestDto =
        ChatRequestDto(
            messages = messages.map { message ->
                ChatMessageDto(
                    role = message.toWireRole(),
                    content = message.content
                )
            }
        )

    private fun ChatMessage.toWireRole(): String = when (role) {
        MessageRole.USER -> personaPrompt ?: MessageRole.USER.wire
        MessageRole.ASSISTANT -> MessageRole.ASSISTANT.wire
    }
}
