package com.ruizurraca.luziatestdavid.data.remote.mapper

import com.ruizurraca.luziatestdavid.data.remote.dto.ChatMessageDto
import com.ruizurraca.luziatestdavid.data.remote.dto.ChatRequestDto
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import javax.inject.Inject

class ChatMapper @Inject constructor() {

    fun toRequestDto(messages: List<ChatMessage>): ChatRequestDto =
        ChatRequestDto(messages = messages.map { it.toDto() })

    private fun ChatMessage.toDto(): ChatMessageDto = when (role) {
        MessageRole.USER -> ChatMessageDto(
            role = MessageRole.USER.wire,
            rolePrompt = requireValidUserPrompt(),
            content = content
        )
        MessageRole.ASSISTANT -> ChatMessageDto(
            role = MessageRole.ASSISTANT.wire,
            rolePrompt = null,
            content = content
        )
    }

    private fun ChatMessage.requireValidUserPrompt(): String {
        val prompt = personaPrompt
        check(!prompt.isNullOrBlank()) {
            "User message $id has a null or blank personaPrompt; " +
                "every user turn must carry a role_prompt (Fork 4)."
        }
        return prompt
    }
}
