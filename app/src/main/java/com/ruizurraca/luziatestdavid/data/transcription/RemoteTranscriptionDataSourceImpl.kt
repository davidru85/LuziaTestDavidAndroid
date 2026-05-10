package com.ruizurraca.luziatestdavid.data.transcription

import com.ruizurraca.luziatestdavid.data.remote.api.L1ApiClient
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteTranscriptionDataSourceImpl @Inject constructor(
    private val apiClient: L1ApiClient
) : RemoteTranscriptionDataSource {
    override suspend fun transcribe(audio: File, languageTag: String?): String =
        apiClient.transcribe(
            audio = audio.readBytes(),
            filename = audio.name,
            lang = languageTag
        ).text
}
