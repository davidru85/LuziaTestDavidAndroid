package com.ruizurraca.luziatestdavid.presentation.screen

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruizurraca.luziatestdavid.R
import com.ruizurraca.luziatestdavid.domain.catalog.PersonaCatalog
import com.ruizurraca.luziatestdavid.presentation.state.ChatEvent
import com.ruizurraca.luziatestdavid.presentation.state.ChatUiState
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

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatEvent.Tier1 -> snackbarHostState.showSnackbar(event.message)
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
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.dialog_mic_permission_title)) },
            text = { Text(stringResource(R.string.dialog_mic_permission_message)) },
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
        AlertDialog(
            onDismissRequest = { tier3Event = null },
            title = { Text(event.title) },
            text = { Text(event.message) },
            confirmButton = {
                TextButton(onClick = { tier3Event = null }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        )
    }
}
