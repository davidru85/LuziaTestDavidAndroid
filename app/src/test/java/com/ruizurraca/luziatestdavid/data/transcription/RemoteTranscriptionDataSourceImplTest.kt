package com.ruizurraca.luziatestdavid.data.transcription

import com.ruizurraca.luziatestdavid.data.remote.api.L1ApiClient
import com.ruizurraca.luziatestdavid.data.remote.dto.TranscribeResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Drives the contract of [RemoteTranscriptionDataSourceImpl]: it must read the
 * audio file's bytes, forward filename + language tag through to [L1ApiClient.transcribe],
 * and return the unwrapped text. Wraps no errors itself — the repo's `try/catch`
 * still owns the [com.ruizurraca.luziatestdavid.domain.common.Resource] envelope.
 */
class RemoteTranscriptionDataSourceImplTest {

    private val apiClient: L1ApiClient = mockk()
    private val dataSource = RemoteTranscriptionDataSourceImpl(apiClient)

    private fun audioFile(bytes: ByteArray, name: String = "voice.m4a"): File =
        File.createTempFile(name.removeSuffix(".m4a"), ".m4a").apply {
            writeBytes(bytes)
            deleteOnExit()
        }

    @Test
    fun `transcribe reads bytes, forwards filename and lang, returns response text`() = runTest {
        val bytes = byteArrayOf(0x1, 0x2, 0x3)
        val file = audioFile(bytes)
        coEvery {
            apiClient.transcribe(any(), any(), any())
        } returns TranscribeResponseDto("hola mundo")

        val result = dataSource.transcribe(file, languageTag = "es")

        assertEquals("hola mundo", result)
        coVerify {
            apiClient.transcribe(
                match { it.contentEquals(bytes) },
                file.name,
                "es"
            )
        }
    }

    @Test
    fun `transcribe forwards null lang as null`() = runTest {
        val file = audioFile(byteArrayOf(0x4))
        coEvery { apiClient.transcribe(any(), any(), any()) } returns TranscribeResponseDto("ok")

        dataSource.transcribe(file, languageTag = null)

        coVerify { apiClient.transcribe(any(), any(), null) }
    }
}
