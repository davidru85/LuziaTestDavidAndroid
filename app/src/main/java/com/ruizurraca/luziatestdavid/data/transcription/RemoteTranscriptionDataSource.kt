package com.ruizurraca.luziatestdavid.data.transcription

import java.io.File

interface RemoteTranscriptionDataSource {
    suspend fun transcribe(audio: File, languageTag: String?): String
}
