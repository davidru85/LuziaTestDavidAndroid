package com.ruizurraca.luziatestdavid.domain.usecase

import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.repository.ChatRepository
import java.io.File
import javax.inject.Inject

class TranscribeAudioUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(audio: File): Resource<String> {
        if (!audio.exists() || audio.length() == 0L) {
            return Resource.Error("Audio file is missing or empty.")
        }
        return repository.transcribeAudio(audio)
    }
}
