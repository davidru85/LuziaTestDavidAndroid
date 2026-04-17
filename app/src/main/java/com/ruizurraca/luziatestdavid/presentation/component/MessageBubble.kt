package com.ruizurraca.luziatestdavid.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ruizurraca.luziatestdavid.presentation.model.ChatMessageUiModel

@Composable
fun MessageBubble(
    model: ChatMessageUiModel,
    modifier: Modifier = Modifier
) {
    when (model) {
        is ChatMessageUiModel.User -> UserMessageBubble(model = model, modifier = modifier)
        is ChatMessageUiModel.Assistant -> AssistantMessageBubble(model = model, modifier = modifier)
    }
}
