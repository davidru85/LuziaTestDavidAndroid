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
    is AppError.ValidationError,
    // Local (client-originated) variants — user-facing copy is resolved via
    // stringResource at the composable layer in 7.3.3.H.2. Until then, the
    // `message` carries the AppError default as a dev-facing fallback.
    AppError.RecorderAlreadyRunning,
    AppError.RecorderNotActive,
    AppError.RecorderNoOutputFile,
    AppError.RecorderStartFailed,
    AppError.RecorderStopFailed,
    AppError.EmptyAudioFile,
    AppError.EmptyConversationHistory,
    AppError.StreamingFailed,
    AppError.UnexpectedFailure -> ChatEvent.Tier1(message)

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
