package com.ruizurraca.luziatestdavid.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDto(
    val role: String,
    @SerialName("role_prompt")
    val rolePrompt: String? = null,
    val content: String
)
