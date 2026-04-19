package com.ruizurraca.luziatestdavid.presentation.screen

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruizurraca.luziatestdavid.R
import com.ruizurraca.luziatestdavid.domain.catalog.PersonaCatalog
import com.ruizurraca.luziatestdavid.presentation.component.LuziaAlertDialog
import com.ruizurraca.luziatestdavid.presentation.state.ChatEvent
import com.ruizurraca.luziatestdavid.presentation.state.ChatUiState
import com.ruizurraca.luziatestdavid.presentation.state.Tier1Kind
import com.ruizurraca.luziatestdavid.presentation.state.Tier3Kind
import com.ruizurraca.luziatestdavid.presentation.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    personaCatalog: PersonaCatalog,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedPersona by viewModel.selectedPersona.collectAsStateWithLifecycle()
    val personaEntries = remember { personaCatalog.entries() }
    val snackbarHostState = remember { SnackbarHostState() }
    var tier3Event by remember { mutableStateOf<ChatEvent.Tier3?>(null) }

    // Pre-resolve every Tier1Kind → translated copy at composable time so the
    // LaunchedEffect lambda can do a pure map lookup. Avoids the
    // `LocalContextGetResourceValueCall` lint: Context captured inside a
    // LaunchedEffect wouldn't invalidate on locale change, whereas the map
    // rebuilds as a normal Compose recomposition would.
    val tier1CopyByKind: Map<Tier1Kind, String> = Tier1Kind.entries.associateWith { kind ->
        stringResource(kind.messageRes())
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatEvent.Tier1 -> {
                    // Prefer backend-supplied message when present (option iii —
                    // backend copy often carries actionable specificity).
                    // Otherwise surface the translated copy for the semantic kind.
                    val text = event.backendMessage ?: tier1CopyByKind.getValue(event.kind)
                    snackbarHostState.showSnackbar(text)
                }
                is ChatEvent.Tier3 -> tier3Event = event
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
        snackbarHostState = snackbarHostState,
        onDraftChange = viewModel::onDraftChange,
        onMicTap = onMicTap,
        onSendTap = viewModel::onSendTap,
        onPersonaSelected = viewModel::onPersonaSelected,
        onRetryLastFailure = viewModel::onRetryLastFailure,
        onConfirmClearConversation = viewModel::onClearConversation
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

    tier3Event?.let { event ->
        LuziaAlertDialog(
            onDismissRequest = { tier3Event = null },
            title = stringResource(event.kind.titleRes()),
            body = stringResource(event.kind.bodyRes()),
            icon = event.kind.icon(),
            iconTint = MaterialTheme.colorScheme.error,
            detailsMessage = event.detailsMessage,
            confirmButton = {
                TextButton(onClick = { tier3Event = null }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        )
    }
}

private fun Tier1Kind.messageRes(): Int = when (this) {
    Tier1Kind.BadRequest -> R.string.tier1_bad_request
    Tier1Kind.FileTooLarge -> R.string.tier1_file_too_large
    Tier1Kind.Timeout -> R.string.tier1_timeout
    Tier1Kind.Network -> R.string.tier1_network
    Tier1Kind.ValidationError -> R.string.tier1_validation_error
    Tier1Kind.RecorderAlreadyRunning -> R.string.tier1_recorder_already_running
    Tier1Kind.RecorderNotActive -> R.string.tier1_recorder_not_active
    Tier1Kind.RecorderNoOutputFile -> R.string.tier1_recorder_no_output
    Tier1Kind.RecorderStartFailed -> R.string.tier1_recorder_start_failed
    Tier1Kind.RecorderStopFailed -> R.string.tier1_recorder_stop_failed
    Tier1Kind.EmptyAudioFile -> R.string.tier1_empty_audio_file
    Tier1Kind.EmptyConversationHistory -> R.string.tier1_empty_conversation_history
    Tier1Kind.StreamingFailed -> R.string.tier1_streaming_failed
    Tier1Kind.UnexpectedFailure -> R.string.tier1_unexpected_failure
    // Unknown: reached when Resource.Error carried only a raw message with no
    // AppError (legacy / test fixtures). Backend message should always be
    // present on this path — the fallback is defensive.
    Tier1Kind.Unknown -> R.string.tier1_unexpected_failure
}

private fun Tier3Kind.titleRes(): Int = when (this) {
    Tier3Kind.ServiceUnavailable -> R.string.dialog_tier3_service_unavailable_title
    Tier3Kind.InternalError -> R.string.dialog_tier3_internal_title
    Tier3Kind.Unexpected -> R.string.dialog_tier3_unexpected_title
}

private fun Tier3Kind.bodyRes(): Int = when (this) {
    Tier3Kind.ServiceUnavailable -> R.string.dialog_tier3_service_unavailable_body
    Tier3Kind.InternalError -> R.string.dialog_tier3_internal_body
    Tier3Kind.Unexpected -> R.string.dialog_tier3_unexpected_body
}

private fun Tier3Kind.icon(): ImageVector = when (this) {
    Tier3Kind.ServiceUnavailable -> Icons.Filled.CloudOff
    Tier3Kind.InternalError -> Icons.Filled.ErrorOutline
    Tier3Kind.Unexpected -> Icons.Filled.HelpOutline
}
