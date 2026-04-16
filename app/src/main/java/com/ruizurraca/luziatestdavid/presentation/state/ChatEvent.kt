package com.ruizurraca.luziatestdavid.presentation.state

sealed interface ChatEvent {
    data class Error(val message: String) : ChatEvent
}
