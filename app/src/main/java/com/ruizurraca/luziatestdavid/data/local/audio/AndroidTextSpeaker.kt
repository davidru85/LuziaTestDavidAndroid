package com.ruizurraca.luziatestdavid.data.local.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.ruizurraca.luziatestdavid.di.qualifier.MainDispatcher
import com.ruizurraca.luziatestdavid.domain.audio.TextSpeaker
import com.ruizurraca.luziatestdavid.domain.common.AppError
import com.ruizurraca.luziatestdavid.domain.common.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * [TextSpeaker] adapter over Android's system [TextToSpeech] engine. Wraps the
 * callback-based framework API in a suspending contract (Phase 10.6.D).
 *
 * No unit tests — the class is a thin adapter over framework state. Correctness
 * is verified by device-level manual TTS playback (same approach as
 * `MediaRecorderAudioRecorder`).
 */
@Singleton
class AndroidTextSpeaker @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher
) : TextSpeaker {

    private var tts: TextToSpeech? = null
    private var initState: InitState = InitState.NotInitialised
    private val initMutex = Mutex()

    override suspend fun speak(text: String, locale: Locale): Resource<Unit> =
        withContext(mainDispatcher) {
            val engine = ensureInitialised()
                ?: return@withContext AppError.TtsUnavailable.toResourceError()

            val languageResult = engine.setLanguage(locale)
            if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                return@withContext AppError.TtsUnavailable.toResourceError()
            }

            val utteranceId = UUID.randomUUID().toString()
            suspendCancellableCoroutine<Resource<Unit>> { cont ->
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) = Unit
                    override fun onDone(id: String?) {
                        if (id == utteranceId && cont.isActive) {
                            cont.resume(Resource.Success(Unit))
                        }
                    }
                    override fun onError(id: String?, errorCode: Int) {
                        if (id == utteranceId && cont.isActive) {
                            cont.resume(AppError.TtsUnavailable.toResourceError())
                        }
                    }
                    @Deprecated("Kept for API < 21 parity; the framework still invokes it.")
                    override fun onError(id: String?) {
                        if (id == utteranceId && cont.isActive) {
                            cont.resume(AppError.TtsUnavailable.toResourceError())
                        }
                    }
                })
                val enqueue = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                if (enqueue == TextToSpeech.ERROR && cont.isActive) {
                    cont.resume(AppError.TtsUnavailable.toResourceError())
                }
                cont.invokeOnCancellation { engine.stop() }
            }
        }

    override fun stop() {
        tts?.stop()
    }

    override fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        initState = InitState.Released
    }

    private suspend fun ensureInitialised(): TextToSpeech? = initMutex.withLock {
        when (initState) {
            InitState.Ready -> tts
            InitState.Failed, InitState.Released -> null
            InitState.NotInitialised -> initEngine()
        }
    }

    private suspend fun initEngine(): TextToSpeech? =
        suspendCancellableCoroutine { cont ->
            lateinit var engine: TextToSpeech
            engine = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts = engine
                    initState = InitState.Ready
                    if (cont.isActive) cont.resume(engine)
                } else {
                    initState = InitState.Failed
                    if (cont.isActive) cont.resume(null)
                }
            }
        }

    private enum class InitState { NotInitialised, Ready, Failed, Released }
}
