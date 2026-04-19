package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ruizurraca.luziatestdavid.R

@Composable
fun MorphingActionButton(
    hasText: Boolean,
    isRecording: Boolean,
    onMicTap: () -> Unit,
    onSendTap: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    AnimatedContent(
        targetState = hasText,
        modifier = modifier,
        label = "morphing-action-button"
    ) { showSend ->
        if (showSend) {
            IconButton(
                onClick = onSendTap,
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.cd_send_message)
                )
            }
        } else {
            FilledIconButton(
                onClick = onMicTap,
                enabled = enabled
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = stringResource(
                        if (isRecording) R.string.cd_stop_recording else R.string.cd_record_voice_message
                    )
                )
            }
        }
    }
}
