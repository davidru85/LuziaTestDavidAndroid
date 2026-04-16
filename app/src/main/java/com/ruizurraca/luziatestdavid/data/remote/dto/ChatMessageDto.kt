package com.ruizurraca.luziatestdavid.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String
)
