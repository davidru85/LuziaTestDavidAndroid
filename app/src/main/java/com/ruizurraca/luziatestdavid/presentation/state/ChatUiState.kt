package com.ruizurraca.luziatestdavid.presentation.state

import com.ruizurraca.luziatestdavid.presentation.model.ChatMessageUiModel

sealed interface ChatUiState {
    val messages: List<ChatMessageUiModel>
    val draft: String

    data class Idle(
        override val messages: List<ChatMessageUiModel> = emptyList(),
        override val draft: String = ""
    ) : ChatUiState

    data class Listening(
        override val messages: List<ChatMessageUiModel>,
        override val draft: String
    ) : ChatUiState

    data class Processing(
        override val messages: List<ChatMessageUiModel>,
        override val draft: String
    ) : ChatUiState

    data class Streaming(
        override val messages: List<ChatMessageUiModel>,
        override val draft: String
    ) : ChatUiState
}
