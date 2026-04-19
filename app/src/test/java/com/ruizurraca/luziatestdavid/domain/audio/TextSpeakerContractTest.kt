package com.ruizurraca.luziatestdavid.domain.audio

import com.ruizurraca.luziatestdavid.domain.common.AppError
import com.ruizurraca.luziatestdavid.domain.common.Resource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

/**
 * Compile-time + behavioural contract test for [TextSpeaker].
 *
 * Mirrors the `ChatRepositoryContractTest` pattern: the anonymous-object
 * implementation below will not compile unless [TextSpeaker] declares the
 * exact signatures required by the domain layer.
 */
class TextSpeakerContractTest {

    private var stopCalls = 0
    private var releaseCalls = 0
    private var lastSpokenText: String? = null
    private var lastLocale: Locale? = null

    private val fake = object : TextSpeaker {
        override suspend fun speak(text: String, locale: Locale): Resource<Unit> {
            lastSpokenText = text
            lastLocale = locale
            return Resource.Success(Unit)
        }

        override fun stop() {
            stopCalls++
        }

        override fun release() {
            releaseCalls++
        }
    }

    @Test
    fun `speak is suspend and returns Resource of Unit`() = runTest {
        val result = fake.speak("Hola mundo", Locale.forLanguageTag("es-ES"))
        assertTrue(result is Resource.Success)
        assertEquals("Hola mundo", lastSpokenText)
        assertEquals(Locale.forLanguageTag("es-ES"), lastLocale)
    }

    @Test
    fun `stop is callable without suspension and is idempotent`() {
        fake.stop()
        fake.stop()
        assertEquals(2, stopCalls)
    }

    @Test
    fun `release is callable without suspension and is idempotent`() {
        fake.release()
        fake.release()
        assertEquals(2, releaseCalls)
    }

    @Test
    fun `speak can surface TtsUnavailable via Resource Error`() = runTest {
        val failingSpeaker = object : TextSpeaker {
            override suspend fun speak(text: String, locale: Locale): Resource<Unit> =
                AppError.TtsUnavailable.toResourceError()

            override fun stop() = Unit
            override fun release() = Unit
        }

        val result = failingSpeaker.speak("hola", Locale.ENGLISH)
        assertTrue(result is Resource.Error)
        val error = (result as Resource.Error).error
        assertSame(AppError.TtsUnavailable, error)
    }
}
