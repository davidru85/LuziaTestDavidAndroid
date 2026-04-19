package com.ruizurraca.luziatestdavid.presentation.component

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ruizurraca.luziatestdavid.R
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme

/**
 * On-demand read-aloud toggle rendered beneath the last received assistant
 * bubble (Phase 10.6.D — Golden Rules #1 and #6 narrowed per MEMORY.md Fork 6).
 * Stateless: `isSpeaking` drives icon + `contentDescription`; `onClick`
 * toggles playback upstream (ViewModel owns the lifecycle).
 */
@Composable
fun TtsPlayButton(
    isSpeaking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        if (isSpeaking) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = stringResource(R.string.cd_tts_stop),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = stringResource(R.string.cd_tts_play),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, name = "Light — Idle")
@Composable
private fun TtsPlayButtonPreview_Idle() {
    LuziaTheme {
        TtsPlayButton(isSpeaking = false, onClick = {})
    }
}

@Preview(showBackground = true, name = "Light — Speaking")
@Composable
private fun TtsPlayButtonPreview_Speaking() {
    LuziaTheme {
        TtsPlayButton(isSpeaking = true, onClick = {})
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark — Speaking"
)
@Composable
private fun TtsPlayButtonPreview_DarkSpeaking() {
    LuziaTheme {
        TtsPlayButton(isSpeaking = true, onClick = {})
    }
}
