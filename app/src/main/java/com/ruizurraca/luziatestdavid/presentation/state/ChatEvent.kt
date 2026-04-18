package com.ruizurraca.luziatestdavid.presentation.state

import com.ruizurraca.luziatestdavid.domain.common.AppError

sealed interface ChatEvent {
    data class Tier1(val message: String) : ChatEvent
    data class Tier3(val title: String, val message: String) : ChatEvent
}

fun AppError.toChatEvent(): ChatEvent = when (this) {
    AppError.BadRequest,
    AppError.FileTooLarge,
    AppError.Timeout,
    AppError.Network -> ChatEvent.Tier1(message)

    is AppError.ValidationError -> ChatEvent.Tier1(message)

    AppError.ServiceUnavailable,
    AppError.Internal -> ChatEvent.Tier3(title = "Service error", message = message)

    is AppError.Unknown -> ChatEvent.Tier3(title = "Unexpected error", message = message)
}
