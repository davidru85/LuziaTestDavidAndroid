package com.ruizurraca.luziatestdavid.data.remote.mapper

import com.ruizurraca.luziatestdavid.data.remote.dto.ChatMessageDto
import com.ruizurraca.luziatestdavid.data.remote.dto.ChatRequestDto
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import javax.inject.Inject

class ChatMapper @Inject constructor() {

    fun toRequestDto(messages: List<ChatMessage>): ChatRequestDto =
        ChatRequestDto(
            messages = messages
                .filterNot { it.isBlankAssistant() }
                .map { it.toDto() }
        )

    // An assistant row with no content is transient residue from a stream that errored
    // before the first token (StreamAssistantReplyUseCase's PENDING placeholder, finalised
    // as FAILED with empty content). The backend accepts-and-drops it (Fork 4 addendum §2)
    // but replaying it pollutes every subsequent /chat payload — drop it at the wire.
    private fun ChatMessage.isBlankAssistant(): Boolean =
        role == MessageRole.ASSISTANT && content.isBlank()

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
