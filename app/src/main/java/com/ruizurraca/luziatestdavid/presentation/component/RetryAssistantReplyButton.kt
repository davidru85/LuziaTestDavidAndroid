package com.ruizurraca.luziatestdavid.presentation.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ruizurraca.luziatestdavid.R
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme

/**
 * Standalone retry affordance rendered beneath a FAILED assistant bubble by
 * `ChatScreenContent`. Lives outside the bubble (Phase 10.6.A) so its label
 * is not visually cramped by longer-text locales — the Spanish copy
 * "Reintentar respuesta" previously fought for horizontal room inside the
 * in-bubble IconButton.
 */
@Composable
fun RetryAssistantReplyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = stringResource(R.string.cd_retry_reply))
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun RetryAssistantReplyButtonPreview_Light() {
    LuziaTheme {
        RetryAssistantReplyButton(onClick = {})
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark"
)
@Composable
private fun RetryAssistantReplyButtonPreview_Dark() {
    LuziaTheme {
        RetryAssistantReplyButton(onClick = {})
    }
}
