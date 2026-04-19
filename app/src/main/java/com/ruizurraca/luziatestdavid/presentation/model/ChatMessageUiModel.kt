package com.ruizurraca.luziatestdavid.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface ChatMessageUiModel {
    val id: String
    val timestamp: Long
    val content: String

    data class User(
        override val id: String,
        override val timestamp: Long,
        override val content: String,
        val deliveryState: UserDeliveryState
    ) : ChatMessageUiModel

    data class Assistant(
        override val id: String,
        override val timestamp: Long,
        override val content: String,
        val streamState: AssistantStreamState
    ) : ChatMessageUiModel
}

enum class UserDeliveryState { SENDING, SENT, FAILED }

enum class AssistantStreamState { LOADING, STREAMING, RECEIVED, FAILED }
