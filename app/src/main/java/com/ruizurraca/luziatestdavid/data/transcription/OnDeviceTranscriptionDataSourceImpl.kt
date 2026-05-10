package com.ruizurraca.luziatestdavid.data.transcription

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnDeviceTranscriptionDataSourceImpl @Inject constructor() : OnDeviceTranscriptionDataSource {
    override suspend fun isAvailable(): Boolean = false
    override suspend fun transcribe(audio: File, languageTag: String?): String =
        throw NotImplementedError("Pending Cycle 3 GREEN")
}
