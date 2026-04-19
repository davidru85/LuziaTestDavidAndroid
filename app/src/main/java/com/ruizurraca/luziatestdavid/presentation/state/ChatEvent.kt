package com.ruizurraca.luziatestdavid.presentation.state

import com.ruizurraca.luziatestdavid.domain.common.AppError

sealed interface ChatEvent {
    /**
     * Non-blocking severity event rendered as a Tier-1 Snackbar.
     * Carries a semantic [Tier1Kind] that the composable resolves to translated
     * copy via stringResource. If `backendMessage` is non-null, it preempts the
     * resolved copy (option iii: backend-supplied message wins when present).
     */
    data class Tier1(
        val kind: Tier1Kind,
        val backendMessage: String? = null
    ) : ChatEvent

    /**
     * Critical-severity one-shot event that materialises as a Tier-3 AlertDialog.
     * Carries a semantic [Tier3Kind] and an optional backend-verbatim
     * `detailsMessage` for the dialog's collapsible Details section.
     */
    data class Tier3(
        val kind: Tier3Kind,
        val detailsMessage: String? = null
    ) : ChatEvent
}

enum class Tier1Kind {
    // Backend-facing (AppError variants that reach Tier 1)
    BadRequest,
    FileTooLarge,
    Timeout,
    Network,
    ValidationError,

    // Local / client-originated error paths
    RecorderAlreadyRunning,
    RecorderNotActive,
    RecorderNoOutputFile,
    RecorderStartFailed,
    RecorderStopFailed,
    EmptyAudioFile,
    EmptyConversationHistory,
    StreamingFailed,
    UnexpectedFailure,
    TtsUnavailable,

    // Legacy/unknown — no AppError attached on the source Resource.Error. The
    // composable falls back to `backendMessage` verbatim when resolving this kind.
    Unknown
}

enum class Tier3Kind {
    ServiceUnavailable,
    InternalError,
    Unexpected
}

fun AppError.toChatEvent(): ChatEvent = when (this) {
    // Backend-facing Tier-1: backend message (rawMessage when user provided it)
    // passes through so the composable can prefer it over the translated copy.
    is AppError.BadRequest -> ChatEvent.Tier1(
        kind = Tier1Kind.BadRequest,
        backendMessage = backendMessageOrNull(rawMessage, default = AppError.BadRequest().rawMessage)
    )
    is AppError.FileTooLarge -> ChatEvent.Tier1(
        kind = Tier1Kind.FileTooLarge,
        backendMessage = backendMessageOrNull(rawMessage, default = AppError.FileTooLarge().rawMessage)
    )
    is AppError.Timeout -> ChatEvent.Tier1(
        kind = Tier1Kind.Timeout,
        backendMessage = backendMessageOrNull(rawMessage, default = AppError.Timeout().rawMessage)
    )
    is AppError.Network -> ChatEvent.Tier1(
        kind = Tier1Kind.Network,
        backendMessage = backendMessageOrNull(rawMessage, default = AppError.Network().rawMessage)
    )
    is AppError.ValidationError -> ChatEvent.Tier1(
        kind = Tier1Kind.ValidationError,
        backendMessage = rawMessage  // ValidationError has no default — always backend-supplied.
    )

    // Local variants: never carry a backend message (client-originated).
    AppError.RecorderAlreadyRunning -> ChatEvent.Tier1(kind = Tier1Kind.RecorderAlreadyRunning)
    AppError.RecorderNotActive -> ChatEvent.Tier1(kind = Tier1Kind.RecorderNotActive)
    AppError.RecorderNoOutputFile -> ChatEvent.Tier1(kind = Tier1Kind.RecorderNoOutputFile)
    AppError.RecorderStartFailed -> ChatEvent.Tier1(kind = Tier1Kind.RecorderStartFailed)
    AppError.RecorderStopFailed -> ChatEvent.Tier1(kind = Tier1Kind.RecorderStopFailed)
    AppError.EmptyAudioFile -> ChatEvent.Tier1(kind = Tier1Kind.EmptyAudioFile)
    AppError.EmptyConversationHistory -> ChatEvent.Tier1(kind = Tier1Kind.EmptyConversationHistory)
    AppError.StreamingFailed -> ChatEvent.Tier1(kind = Tier1Kind.StreamingFailed)
    AppError.UnexpectedFailure -> ChatEvent.Tier1(kind = Tier1Kind.UnexpectedFailure)
    AppError.TtsUnavailable -> ChatEvent.Tier1(kind = Tier1Kind.TtsUnavailable)

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

/**
 * Returns [raw] as the backend message only when it diverges from the AppError
 * default — i.e. something the backend actually populated. When [raw] equals the
 * hardcoded English default, treat it as "backend silent" and return null so the
 * composable resolves translated copy instead.
 */
private fun backendMessageOrNull(raw: String, default: String): String? =
    if (raw == default) null else raw
