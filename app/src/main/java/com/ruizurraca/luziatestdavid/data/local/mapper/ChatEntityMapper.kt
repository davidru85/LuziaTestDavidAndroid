package com.ruizurraca.luziatestdavid.data.local.mapper

import com.ruizurraca.luziatestdavid.data.local.entity.ChatMessageEntity
import com.ruizurraca.luziatestdavid.domain.model.ChatMessage
import com.ruizurraca.luziatestdavid.domain.model.MessageRole
import com.ruizurraca.luziatestdavid.domain.model.MessageStatus

fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    role = role.wire,
    content = content,
    timestamp = timestamp,
    status = status.name,
    personaPrompt = personaPrompt
)

fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    role = MessageRole.entries.first { it.wire == role },
    content = content,
    timestamp = timestamp,
    status = MessageStatus.valueOf(status),
    personaPrompt = personaPrompt
)
