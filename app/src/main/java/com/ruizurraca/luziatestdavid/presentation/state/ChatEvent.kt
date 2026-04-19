package com.ruizurraca.luziatestdavid.presentation.state

import com.ruizurraca.luziatestdavid.domain.common.AppError

sealed interface ChatEvent {
    /**
     * Non-blocking severity event rendered as a transient Snackbar. Carries a
     * semantic [TransientSnackbarKind] that the composable resolves to translated
     * copy via stringResource. If `backendMessage` is non-null, it preempts the
     * resolved copy (option iii: backend-supplied message wins when present).
     */
    data class TransientSnackbar(
        val kind: TransientSnackbarKind,
        val backendMessage: String? = null
    ) : ChatEvent

    /**
     * Critical-severity one-shot event that materialises as a blocking
     * AlertDialog. Carries a semantic [BlockingErrorDialogKind] and an optional
     * backend-verbatim `detailsMessage` for the dialog's collapsible Details
     * section.
     */
    data class BlockingErrorDialog(
        val kind: BlockingErrorDialogKind,
        val detailsMessage: String? = null
    ) : ChatEvent
}

enum class TransientSnackbarKind {
    // Backend-facing (AppError variants that reach the transient-snackbar channel)
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

enum class BlockingErrorDialogKind {
    ServiceUnavailable,
    InternalError,
    Unexpected
}

fun AppError.toChatEvent(): ChatEvent = when (this) {
    // Backend-facing snackbar variants: backend message (rawMessage when user
    // provided it) passes through so the composable can prefer it over the
    // translated copy.
    is AppError.BadRequest -> ChatEvent.TransientSnackbar(
        kind = TransientSnackbarKind.BadRequest,
        backendMessage = backendMessageOrNull(rawMessage, default = AppError.BadRequest().rawMessage)
    )
    is AppError.FileTooLarge -> ChatEvent.TransientSnackbar(
        kind = TransientSnackbarKind.FileTooLarge,
        backendMessage = backendMessageOrNull(rawMessage, default = AppError.FileTooLarge().rawMessage)
    )
    is AppError.Timeout -> ChatEvent.TransientSnackbar(
        kind = TransientSnackbarKind.Timeout,
        backendMessage = backendMessageOrNull(rawMessage, default = AppError.Timeout().rawMessage)
    )
    is AppError.Network -> ChatEvent.TransientSnackbar(
        kind = TransientSnackbarKind.Network,
        backendMessage = backendMessageOrNull(rawMessage, default = AppError.Network().rawMessage)
    )
    is AppError.ValidationError -> ChatEvent.TransientSnackbar(
        kind = TransientSnackbarKind.ValidationError,
        backendMessage = rawMessage  // ValidationError has no default — always backend-supplied.
    )

    // Local variants: never carry a backend message (client-originated).
    AppError.RecorderAlreadyRunning -> ChatEvent.TransientSnackbar(kind = TransientSnackbarKind.RecorderAlreadyRunning)
    AppError.RecorderNotActive -> ChatEvent.TransientSnackbar(kind = TransientSnackbarKind.RecorderNotActive)
    AppError.RecorderNoOutputFile -> ChatEvent.TransientSnackbar(kind = TransientSnackbarKind.RecorderNoOutputFile)
    AppError.RecorderStartFailed -> ChatEvent.TransientSnackbar(kind = TransientSnackbarKind.RecorderStartFailed)
    AppError.RecorderStopFailed -> ChatEvent.TransientSnackbar(kind = TransientSnackbarKind.RecorderStopFailed)
    AppError.EmptyAudioFile -> ChatEvent.TransientSnackbar(kind = TransientSnackbarKind.EmptyAudioFile)
    AppError.EmptyConversationHistory -> ChatEvent.TransientSnackbar(kind = TransientSnackbarKind.EmptyConversationHistory)
    AppError.StreamingFailed -> ChatEvent.TransientSnackbar(kind = TransientSnackbarKind.StreamingFailed)
    AppError.UnexpectedFailure -> ChatEvent.TransientSnackbar(kind = TransientSnackbarKind.UnexpectedFailure)
    AppError.TtsUnavailable -> ChatEvent.TransientSnackbar(kind = TransientSnackbarKind.TtsUnavailable)

    is AppError.ServiceUnavailable -> ChatEvent.BlockingErrorDialog(
        kind = BlockingErrorDialogKind.ServiceUnavailable,
        detailsMessage = message
    )
    is AppError.Internal -> ChatEvent.BlockingErrorDialog(
        kind = BlockingErrorDialogKind.InternalError,
        detailsMessage = message
    )
    is AppError.Unknown -> ChatEvent.BlockingErrorDialog(
        kind = BlockingErrorDialogKind.Unexpected,
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
