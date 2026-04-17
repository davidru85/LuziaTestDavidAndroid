package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MorphingActionButton(
    hasText: Boolean,
    isRecording: Boolean,
    onMicTap: () -> Unit,
    onSendTap: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
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
                    contentDescription = "Send message"
                )
            }
        } else {
            FilledIconButton(
                onClick = onMicTap,
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = if (isRecording) "Stop recording" else "Record voice message"
                )
            }
        }
    }
}
