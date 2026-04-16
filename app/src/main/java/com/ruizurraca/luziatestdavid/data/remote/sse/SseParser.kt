package com.ruizurraca.luziatestdavid.data.remote.sse

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
private data class ErrorPayload(val code: String, val message: String)

class SseParser @Inject constructor() {

    fun parse(lines: Flow<String>): Flow<SseEvent> = flow {
        var eventType: String? = null
        var dataBuffer: String? = null

        emitAll(
            lines.transformWhile { line ->
                when {
                    line.isEmpty() -> {
                        val data = dataBuffer
                        val type = eventType
                        eventType = null
                        dataBuffer = null

                        when {
                            data == null -> true
                            type == EVENT_ERROR -> {
                                val payload = json.decodeFromString<ErrorPayload>(data)
                                emit(SseEvent.Error(payload.code, payload.message))
                                false
                            }
                            data == DONE_SENTINEL -> false
                            else -> {
                                emit(SseEvent.Token(data))
                                true
                            }
                        }
                    }
                    line.startsWith(EVENT_PREFIX) -> {
                        eventType = stripLeadingSpace(line.substring(EVENT_PREFIX.length))
                        true
                    }
                    line.startsWith(DATA_PREFIX) -> {
                        val value = stripLeadingSpace(line.substring(DATA_PREFIX.length))
                        dataBuffer = dataBuffer?.let { "$it\n$value" } ?: value
                        true
                    }
                    else -> true
                }
            }
        )
    }

    private companion object {
        const val DATA_PREFIX = "data:"
        const val EVENT_PREFIX = "event:"
        const val EVENT_ERROR = "error"
        const val DONE_SENTINEL = "[DONE]"

        val json = Json { ignoreUnknownKeys = true }

        fun stripLeadingSpace(s: String): String =
            if (s.startsWith(" ")) s.substring(1) else s
    }
}
