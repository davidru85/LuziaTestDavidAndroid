package com.ruizurraca.luziatestdavid.data.remote.sse

sealed interface SseEvent {
    data class Token(val text: String) : SseEvent
    data class Error(val code: String, val message: String) : SseEvent
}
