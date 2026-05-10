package com.ruizurraca.luziatestdavid.data.transcription

import java.io.File

interface OnDeviceTranscriptionDataSource {
    suspend fun isAvailable(): Boolean
    suspend fun transcribe(audio: File, languageTag: String?): String
}
