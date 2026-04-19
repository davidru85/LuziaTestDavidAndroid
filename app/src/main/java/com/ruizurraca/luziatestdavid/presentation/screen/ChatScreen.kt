package com.ruizurraca.luziatestdavid.presentation.screen

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ruizurraca.luziatestdavid.R
import com.ruizurraca.luziatestdavid.domain.catalog.PersonaCatalog
import com.ruizurraca.luziatestdavid.presentation.component.LuziaAlertDialog
import com.ruizurraca.luziatestdavid.presentation.state.ChatEvent
import com.ruizurraca.luziatestdavid.presentation.state.ChatUiState
import com.ruizurraca.luziatestdavid.presentation.state.BlockingErrorDialogKind
import com.ruizurraca.luziatestdavid.presentation.state.TransientSnackbarKind
import com.ruizurraca.luziatestdavid.presentation.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    personaCatalog: PersonaCatalog,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedPersona by viewModel.selectedPersona.collectAsStateWithLifecycle()
    val currentlySpeakingId by viewModel.currentlySpeakingId.collectAsStateWithLifecycle()
    val personaEntries = remember { personaCatalog.entries() }
    val snackbarHostState = remember { SnackbarHostState() }
    var blockingErrorEvent by remember { mutableStateOf<ChatEvent.BlockingErrorDialog?>(null) }

    // Phase 10.6.D: resolve the TTS locale from the current app configuration
    // rather than `Locale.getDefault()` so it tracks in-app locale changes.
    val configuration = LocalConfiguration.current
    val ttsLocale = remember(configuration) { configuration.locales[0] }

    // Pre-resolve every TransientSnackbarKind → translated copy at composable
    // time so the LaunchedEffect lambda can do a pure map lookup. Avoids the
    // `LocalContextGetResourceValueCall` lint: Context captured inside a
    // LaunchedEffect wouldn't invalidate on locale change, whereas the map
    // rebuilds as a normal Compose recomposition would.
    val snackbarCopyByKind: Map<TransientSnackbarKind, String> =
        TransientSnackbarKind.entries.associateWith { kind ->
            stringResource(kind.messageRes())
        }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatEvent.TransientSnackbar -> {
                    // Prefer backend-supplied message when present (option iii —
                    // backend copy often carries actionable specificity).
                    // Otherwise surface the translated copy for the semantic kind.
                    val text = event.backendMessage ?: snackbarCopyByKind.getValue(event.kind)
                    snackbarHostState.showSnackbar(text)
                }
                is ChatEvent.BlockingErrorDialog -> blockingErrorEvent = event
            }
        }
    }

    val context = LocalContext.current
    var showRationale by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startRecording()
        } else {
            val activity = context as? Activity
            val shouldShow = activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.RECORD_AUDIO
            ) ?: false
            if (shouldShow) {
                showRationale = true
            }
        }
    }

    val isRecording by remember { derivedStateOf { state is ChatUiState.Listening } }

    val onMicTap: () -> Unit = remember(viewModel, permissionLauncher, context) {
        {
            if (isRecording) {
                viewModel.stopRecording()
            } else {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    viewModel.startRecording()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    ChatScreenContent(
        state = state,
        selectedPersona = selectedPersona,
        personaEntries = personaEntries,
        isRecording = isRecording,
        currentlySpeakingId = currentlySpeakingId,
        snackbarHostState = snackbarHostState,
        onDraftChange = viewModel::onDraftChange,
        onMicTap = onMicTap,
        onSendTap = viewModel::onSendTap,
        onPersonaSelected = viewModel::onPersonaSelected,
        onRetryLastFailure = viewModel::onRetryLastFailure,
        onConfirmClearConversation = viewModel::onClearConversation,
        onTtsTap = { id, text -> viewModel.onTtsTap(id, text, ttsLocale) }
    )

    if (showRationale) {
        LuziaAlertDialog(
            onDismissRequest = { showRationale = false },
            title = stringResource(R.string.dialog_mic_permission_title),
            body = stringResource(R.string.dialog_mic_permission_message),
            icon = Icons.Filled.Mic,
            iconTint = MaterialTheme.colorScheme.primary,
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text(stringResource(R.string.dialog_retry))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    blockingErrorEvent?.let { event ->
        LuziaAlertDialog(
            onDismissRequest = { blockingErrorEvent = null },
            title = stringResource(event.kind.titleRes()),
            body = stringResource(event.kind.bodyRes()),
            icon = event.kind.icon(),
            iconTint = MaterialTheme.colorScheme.error,
            detailsMessage = event.detailsMessage,
            confirmButton = {
                TextButton(onClick = { blockingErrorEvent = null }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        )
    }
}

private fun TransientSnackbarKind.messageRes(): Int = when (this) {
    TransientSnackbarKind.BadRequest -> R.string.transient_snackbar_bad_request
    TransientSnackbarKind.FileTooLarge -> R.string.transient_snackbar_file_too_large
    TransientSnackbarKind.Timeout -> R.string.transient_snackbar_timeout
    TransientSnackbarKind.Network -> R.string.transient_snackbar_network
    TransientSnackbarKind.ValidationError -> R.string.transient_snackbar_validation_error
    TransientSnackbarKind.RecorderAlreadyRunning -> R.string.transient_snackbar_recorder_already_running
    TransientSnackbarKind.RecorderNotActive -> R.string.transient_snackbar_recorder_not_active
    TransientSnackbarKind.RecorderNoOutputFile -> R.string.transient_snackbar_recorder_no_output
    TransientSnackbarKind.RecorderStartFailed -> R.string.transient_snackbar_recorder_start_failed
    TransientSnackbarKind.RecorderStopFailed -> R.string.transient_snackbar_recorder_stop_failed
    TransientSnackbarKind.EmptyAudioFile -> R.string.transient_snackbar_empty_audio_file
    TransientSnackbarKind.EmptyConversationHistory -> R.string.transient_snackbar_empty_conversation_history
    TransientSnackbarKind.StreamingFailed -> R.string.transient_snackbar_streaming_failed
    TransientSnackbarKind.UnexpectedFailure -> R.string.transient_snackbar_unexpected_failure
    TransientSnackbarKind.TtsUnavailable -> R.string.transient_snackbar_tts_unavailable
    // Unknown: reached when Resource.Error carried only a raw message with no
    // AppError (legacy / test fixtures). Backend message should always be
    // present on this path — the fallback is defensive.
    TransientSnackbarKind.Unknown -> R.string.transient_snackbar_unexpected_failure
}

private fun BlockingErrorDialogKind.titleRes(): Int = when (this) {
    BlockingErrorDialogKind.ServiceUnavailable -> R.string.blocking_error_dialog_service_unavailable_title
    BlockingErrorDialogKind.InternalError -> R.string.blocking_error_dialog_internal_title
    BlockingErrorDialogKind.Unexpected -> R.string.blocking_error_dialog_unexpected_title
}

private fun BlockingErrorDialogKind.bodyRes(): Int = when (this) {
    BlockingErrorDialogKind.ServiceUnavailable -> R.string.blocking_error_dialog_service_unavailable_body
    BlockingErrorDialogKind.InternalError -> R.string.blocking_error_dialog_internal_body
    BlockingErrorDialogKind.Unexpected -> R.string.blocking_error_dialog_unexpected_body
}

private fun BlockingErrorDialogKind.icon(): ImageVector = when (this) {
    BlockingErrorDialogKind.ServiceUnavailable -> Icons.Filled.CloudOff
    BlockingErrorDialogKind.InternalError -> Icons.Filled.ErrorOutline
    BlockingErrorDialogKind.Unexpected -> Icons.AutoMirrored.Filled.HelpOutline
}
