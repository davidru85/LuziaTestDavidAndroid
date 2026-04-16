package com.ruizurraca.luziatestdavid.data.remote.api

import com.ruizurraca.luziatestdavid.data.remote.dto.ChatRequestDto
import com.ruizurraca.luziatestdavid.data.remote.dto.TranscribeResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class L1ApiClient @Inject constructor(
    private val httpClient: HttpClient
) {

    suspend fun transcribe(
        audio: ByteArray,
        filename: String = "audio.m4a"
    ): TranscribeResponseDto =
        httpClient.submitFormWithBinaryData(
            url = "transcribe",
            formData = formData {
                append(
                    key = "audio",
                    value = audio,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "audio/mp4")
                        append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                    }
                )
            }
        ).body()

    fun streamChat(request: ChatRequestDto): Flow<String> = flow {
        httpClient.preparePost("chat") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                emit(line)
            }
        }
    }
}
