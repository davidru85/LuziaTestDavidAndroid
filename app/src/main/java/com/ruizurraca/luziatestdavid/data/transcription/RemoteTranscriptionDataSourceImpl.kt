package com.ruizurraca.luziatestdavid.data.transcription

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteTranscriptionDataSourceImpl @Inject constructor() : RemoteTranscriptionDataSource {
    override suspend fun transcribe(audio: File, languageTag: String?): String =
        throw NotImplementedError("Pending Cycle 2 GREEN")
}
