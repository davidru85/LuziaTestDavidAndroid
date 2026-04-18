package com.ruizurraca.luziatestdavid.domain.usecase

import com.ruizurraca.luziatestdavid.domain.common.AppError
import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.repository.ChatRepository
import java.io.File
import javax.inject.Inject

class TranscribeAudioUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(audio: File): Resource<String> {
        if (!audio.exists() || audio.length() == 0L) {
            return AppError.EmptyAudioFile.toResourceError()
        }
        return repository.transcribeAudio(audio)
    }
}
