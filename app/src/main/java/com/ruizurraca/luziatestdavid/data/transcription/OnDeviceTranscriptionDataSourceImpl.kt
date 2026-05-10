package com.ruizurraca.luziatestdavid.data.transcription

import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
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
        if (sdkInt < MIN_SUPPORTED_SDK) {
            Log.d(TAG, "isAvailable: SDK $sdkInt < $MIN_SUPPORTED_SDK -> false")
            return false
        }
        val recognizer = SpeechRecognition.getClient(buildOptions(languageTag = null))
        return try {
            val status = recognizer.checkStatus()
            val available = status == FeatureStatus.AVAILABLE
            Log.d(TAG, "isAvailable: checkStatus=$status (AVAILABLE=${FeatureStatus.AVAILABLE}) -> $available")
            available
        } finally {
            recognizer.close()
        }
    }

    override suspend fun transcribe(audio: File, languageTag: String?): String {
        check(sdkInt >= MIN_SUPPORTED_SDK) {
            "On-device transcription requires API $MIN_SUPPORTED_SDK+, current is $sdkInt"
        }
        Log.d(TAG, "transcribe: file=${audio.absolutePath} size=${audio.length()} lang=$languageTag")
        val recognizer = SpeechRecognition.getClient(buildOptions(languageTag))
        return try {
            ParcelFileDescriptor.open(audio, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                val request = speechRecognizerRequest {
                    audioSource = AudioSource.fromPfd(pfd)
                }
                val finalText = StringBuilder()
                var lastPartial: String? = null
                recognizer.startRecognition(request).collect { response ->
                    when (response) {
                        is SpeechRecognizerResponse.FinalTextResponse -> {
                            Log.d(TAG, "FinalTextResponse: '${response.text}'")
                            finalText.append(response.text)
                        }
                        is SpeechRecognizerResponse.PartialTextResponse -> {
                            Log.d(TAG, "PartialTextResponse: '${response.text}'")
                            lastPartial = response.text
                        }
                        is SpeechRecognizerResponse.ErrorResponse -> {
                            Log.e(TAG, "ErrorResponse: ${response.e}", response.e)
                            throw response.e
                        }
                        is SpeechRecognizerResponse.CompletedResponse -> {
                            Log.d(TAG, "CompletedResponse")
                        }
                    }
                }
                val text = if (finalText.isNotEmpty()) finalText.toString() else lastPartial.orEmpty()
                Log.d(TAG, "transcribe done: result='$text' (final=${finalText.isNotEmpty()})")
                text
            }
        } finally {
            recognizer.close()
        }
    }

    private fun buildOptions(languageTag: String?) = speechRecognizerOptions {
        locale = resolveLocale(languageTag).also {
            Log.d(TAG, "buildOptions: locale=$it preferredMode=MODE_BASIC")
        }
        preferredMode = SpeechRecognizerOptions.Mode.MODE_BASIC
    }

    /**
     * ML Kit basic-mode locales are full language-region tags ("es-ES", "en-US",
     * etc.). The app's [com.ruizurraca.luziatestdavid.domain.locale.LocaleProvider]
     * may return just a language code ("es"), so we expand short tags to the
     * canonical region the doc lists as supported.
     */
    private fun resolveLocale(languageTag: String?): Locale {
        if (languageTag.isNullOrBlank()) return Locale.getDefault()
        val parsed = Locale.forLanguageTag(languageTag)
        if (parsed.country.isNotBlank()) return parsed
        val region = SHORT_TAG_DEFAULT_REGION[parsed.language] ?: return parsed
        return Locale(parsed.language, region)
    }

    private companion object {
        const val MIN_SUPPORTED_SDK = 31
        const val TAG = "OnDeviceTranscribe"
        // Aligns with ML Kit GenAI Speech Recognition basic-mode supported locales.
        val SHORT_TAG_DEFAULT_REGION = mapOf(
            "es" to "ES",
            "en" to "US",
            "pt" to "BR",
            "it" to "IT",
            "fr" to "FR",
            "de" to "DE",
            "ja" to "JP",
            "ko" to "KR",
            "ru" to "RU",
            "vi" to "VN",
            "hi" to "IN",
            "tr" to "TR",
            "pl" to "PL"
        )
    }
}
