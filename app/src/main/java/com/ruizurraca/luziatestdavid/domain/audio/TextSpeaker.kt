package com.ruizurraca.luziatestdavid.domain.audio

import com.ruizurraca.luziatestdavid.domain.common.Resource
import java.util.Locale

/**
 * Output-side text-to-speech abstraction for the on-demand "read aloud"
 * affordance on assistant replies (Golden Rules #1/#6 narrowed — see
 * MEMORY.md §Phase 10 Fork 6). Pure-Kotlin contract; the Android-backed
 * implementation lives in `data/local/audio/AndroidTextSpeaker`.
 */
interface TextSpeaker {

    /**
     * Speaks [text] using the system TTS engine at the given [locale]. Suspends
     * until the utterance completes or the caller's coroutine is cancelled.
     *
     * Returns [Resource.Success] on clean completion; [Resource.Error] wrapping
     * `AppError.TtsUnavailable` when the engine fails to initialise, the
     * requested language is missing, or the engine raises an error mid-utterance.
     */
    suspend fun speak(text: String, locale: Locale): Resource<Unit>

    /** Stops the current utterance if any. No-op if nothing is playing. */
    fun stop()

    /**
     * Best-effort synchronous teardown for when the owning UI is going away.
     * Implementations release native TTS resources. Safe to call from
     * non-suspend contexts and safe to call repeatedly.
     */
    fun release()
}
