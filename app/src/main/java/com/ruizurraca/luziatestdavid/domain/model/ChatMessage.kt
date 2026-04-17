package com.ruizurraca.luziatestdavid.domain.model

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.DELIVERED,
    val personaPrompt: String? = null
)
