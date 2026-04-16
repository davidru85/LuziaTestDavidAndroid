package com.ruizurraca.luziatestdavid.presentation.model

import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import com.ruizurraca.luziatestdavid.domain.model.MessageStatus

fun List<ChatMessage>.toUiModels(): List<ChatMessageUiModel> =
    mapNotNull { it.toUiModelOrNull() }

private fun ChatMessage.toUiModelOrNull(): ChatMessageUiModel? = when (role) {
    MessageRole.USER -> ChatMessageUiModel.User(
        id = id,
        timestamp = timestamp,
        content = content,
        deliveryState = status.toUserDeliveryState()
    )
    MessageRole.ASSISTANT -> ChatMessageUiModel.Assistant(
        id = id,
        timestamp = timestamp,
        content = content,
        streamState = toAssistantStreamState()
    )
    MessageRole.SYSTEM -> null
}

private fun MessageStatus.toUserDeliveryState(): UserDeliveryState = when (this) {
    MessageStatus.PENDING -> UserDeliveryState.SENDING
    MessageStatus.DELIVERED -> UserDeliveryState.SENT
    MessageStatus.FAILED -> UserDeliveryState.FAILED
}

private fun ChatMessage.toAssistantStreamState(): AssistantStreamState = when (status) {
    MessageStatus.PENDING -> if (content.isEmpty()) AssistantStreamState.LOADING
                            else AssistantStreamState.STREAMING
    MessageStatus.DELIVERED -> AssistantStreamState.RECEIVED
    MessageStatus.FAILED -> AssistantStreamState.FAILED
}
