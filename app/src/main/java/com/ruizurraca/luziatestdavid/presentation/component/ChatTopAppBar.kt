package com.ruizurraca.luziatestdavid.presentation.component

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ruizurraca.luziatestdavid.R
import com.ruizurraca.luziatestdavid.presentation.theme.LuziaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopAppBar(
    title: String,
    isConversationEmpty: Boolean,
    onConfirmClearConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        actions = {
            IconButton(
                onClick = { showConfirmDialog = true },
                enabled = !isConversationEmpty
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteSweep,
                    contentDescription = stringResource(R.string.cd_clear_conversation)
                )
            }
        }
    )

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.dialog_clear_conversation_title)) },
            text = { Text(stringResource(R.string.dialog_clear_conversation_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onConfirmClearConversation()
                }) {
                    Text(stringResource(R.string.dialog_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Preview(showBackground = true, name = "Light — Non-empty (enabled)")
@Composable
private fun ChatTopAppBarPreview_NonEmpty() {
    LuziaTheme {
        ChatTopAppBar(
            title = "Luzia",
            isConversationEmpty = false,
            onConfirmClearConversation = {}
        )
    }
}

@Preview(showBackground = true, name = "Light — Empty (disabled)")
@Composable
private fun ChatTopAppBarPreview_Empty() {
    LuziaTheme {
        ChatTopAppBar(
            title = "Luzia",
            isConversationEmpty = true,
            onConfirmClearConversation = {}
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark — Non-empty"
)
@Composable
private fun ChatTopAppBarPreview_DarkNonEmpty() {
    LuziaTheme {
        ChatTopAppBar(
            title = "Luzia",
            isConversationEmpty = false,
            onConfirmClearConversation = {}
        )
    }
}
