package com.ruizurraca.luziatestdavid.presentation.state

import com.ruizurraca.luziatestdavid.domain.common.AppError

sealed interface ChatEvent {
    data class Tier1(val message: String) : ChatEvent

    /**
     * Critical-severity one-shot event that materialises as a Tier-3 AlertDialog.
     * Carries a semantic [Tier3Kind] (the composable resolves icon + title +
     * hand-written friendly body via stringResource) and the optional backend-
     * verbatim `detailsMessage` for the dialog's collapsible Details section.
     */
    data class Tier3(
        val kind: Tier3Kind,
        val detailsMessage: String? = null
    ) : ChatEvent
}

enum class Tier3Kind {
    ServiceUnavailable,
    InternalError,
    Unexpected
}

fun AppError.toChatEvent(): ChatEvent = when (this) {
    is AppError.BadRequest,
    is AppError.FileTooLarge,
    is AppError.Timeout,
    is AppError.Network,
    is AppError.ValidationError -> ChatEvent.Tier1(message)

    is AppError.ServiceUnavailable -> ChatEvent.Tier3(
        kind = Tier3Kind.ServiceUnavailable,
        detailsMessage = message
    )

    is AppError.Internal -> ChatEvent.Tier3(
        kind = Tier3Kind.InternalError,
        detailsMessage = message
    )

    is AppError.Unknown -> ChatEvent.Tier3(
        kind = Tier3Kind.Unexpected,
        detailsMessage = message
    )
}
