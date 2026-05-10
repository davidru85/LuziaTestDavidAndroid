package com.ruizurraca.luziatestdavid.data.transcription

import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton

interface SpeechRecognitionClientFactory {
    fun getClient(options: SpeechRecognizerOptions): SpeechRecognizer
}

@Singleton
class SpeechRecognitionClientFactoryImpl @Inject constructor() : SpeechRecognitionClientFactory {
    override fun getClient(options: SpeechRecognizerOptions): SpeechRecognizer {
        return SpeechRecognition.getClient(options)
    }
}
