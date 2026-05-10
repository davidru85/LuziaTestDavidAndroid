package com.ruizurraca.luziatestdavid.data.transcription

import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collect
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnDeviceTranscriptionDataSourceImpl(
    private val context: Context,
    private val sdkInt: Int
) : OnDeviceTranscriptionDataSource {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context, Build.VERSION.SDK_INT)

    override suspend fun isAvailable(): Boolean {
        if (sdkInt < MIN_SUPPORTED_SDK) return false
        val recognizer = SpeechRecognition.getClient(buildOptions(languageTag = null))
        return try {
            recognizer.checkStatus() == FeatureStatus.AVAILABLE
        } finally {
            recognizer.close()
        }
    }

    override suspend fun transcribe(audio: File, languageTag: String?): String {
        check(sdkInt >= MIN_SUPPORTED_SDK) {
            "On-device transcription requires API $MIN_SUPPORTED_SDK+, current is $sdkInt"
        }
        val recognizer = SpeechRecognition.getClient(buildOptions(languageTag))
        return try {
            ParcelFileDescriptor.open(audio, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                val request = speechRecognizerRequest {
                    audioSource = AudioSource.fromPfd(pfd)
                }
                buildString {
                    recognizer.startRecognition(request).collect { response ->
                        if (response is SpeechRecognizerResponse.FinalTextResponse) {
                            append(response.text)
                        }
                    }
                }
            }
        } finally {
            recognizer.close()
        }
    }

    private fun buildOptions(languageTag: String?) = speechRecognizerOptions {
        locale = languageTag?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault()
    }

    private companion object {
        const val MIN_SUPPORTED_SDK = 31
    }
}
