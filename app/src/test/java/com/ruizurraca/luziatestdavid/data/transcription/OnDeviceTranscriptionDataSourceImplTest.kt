package com.ruizurraca.luziatestdavid.data.transcription

import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class OnDeviceTranscriptionDataSourceImplTest {

    private val m4aToWavConverter = mockk<M4aToWavConverter>(relaxed = true)
    private val context = mockk<android.content.Context>(relaxed = true)
    private val clientFactory = mockk<SpeechRecognitionClientFactory>()

    @Test
    fun `isAvailable returns false when API level is below 31`() = runTest {
        val ds = OnDeviceTranscriptionDataSourceImpl(
            context = context,
            sdkInt = 30,
            m4aToWavConverter = m4aToWavConverter,
            clientFactory = clientFactory
        )

        assertFalse(ds.isAvailable())
    }

    @Test
    fun `transcribe returns empty string when no speech is detected`() = runTest {
        // Arrange
        val ds = OnDeviceTranscriptionDataSourceImpl(
            context = context,
            sdkInt = 33,
            m4aToWavConverter = m4aToWavConverter,
            clientFactory = clientFactory
        )
        val audioFile = File("test.m4a")
        val wavFile = File.createTempFile("test", ".wav")
        every { m4aToWavConverter.convertToWav(audioFile) } returns wavFile

        val recognizer = mockk<SpeechRecognizer>(relaxed = true)
        every { clientFactory.getClient(any()) } returns recognizer

        val noSpeechException = mockk<GenAiException>(relaxed = true)
        every { noSpeechException.message } returns "Speech recognition engine is closed due to internal error: ERROR_TYPE_NO_SPEECH_DETECTED"
        val errorResponse = SpeechRecognizerResponse.ErrorResponse(noSpeechException)
        every { recognizer.startRecognition(any()) } returns flowOf(errorResponse)

        // Act
        val result = ds.transcribe(audioFile, "en-US")

        // Assert
        assertEquals("", result)
    }
}
