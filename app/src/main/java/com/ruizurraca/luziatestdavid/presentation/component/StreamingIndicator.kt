package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StreamingIndicator(
    visible: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}
