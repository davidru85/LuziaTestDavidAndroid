package com.ruizurraca.luziatestdavid.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequestDto(
    val messages: List<ChatMessageDto>
)
