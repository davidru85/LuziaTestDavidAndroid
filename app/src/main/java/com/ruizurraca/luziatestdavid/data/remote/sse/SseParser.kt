package com.ruizurraca.luziatestdavid.data.remote.sse

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ErrorPayload(val code: String, val message: String)

class SseParser {

    fun parse(lines: Flow<String>): Flow<SseEvent> = flow {
        var eventType: String? = null
        var dataBuffer: String? = null
        var done = false

        lines.collect { line ->
            if (done) return@collect

            if (line.isEmpty()) {
                val data = dataBuffer
                val type = eventType
                eventType = null
                dataBuffer = null

                if (data == null) return@collect

                when (type) {
                    EVENT_ERROR -> {
                        val payload = Json.decodeFromString<ErrorPayload>(data)
                        emit(SseEvent.Error(payload.code, payload.message))
                        done = true
                    }
                    else -> {
                        if (data == DONE_SENTINEL) {
                            done = true
                        } else {
                            emit(SseEvent.Token(data))
                        }
                    }
                }
            } else if (line.startsWith(EVENT_PREFIX)) {
                eventType = stripOneLeadingSpace(line.substring(EVENT_PREFIX.length))
            } else if (line.startsWith(DATA_PREFIX)) {
                val value = stripOneLeadingSpace(line.substring(DATA_PREFIX.length))
                dataBuffer = if (dataBuffer == null) value else "$dataBuffer\n$value"
            }
        }
    }

    private fun stripOneLeadingSpace(s: String): String =
        if (s.startsWith(" ")) s.substring(1) else s

    private companion object {
        const val DATA_PREFIX = "data:"
        const val EVENT_PREFIX = "event:"
        const val EVENT_ERROR = "error"
        const val DONE_SENTINEL = "[DONE]"
    }
}
