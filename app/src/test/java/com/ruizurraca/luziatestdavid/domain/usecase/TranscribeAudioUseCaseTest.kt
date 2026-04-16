package com.ruizurraca.luziatestdavid.domain.usecase

import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.repository.ChatRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class TranscribeAudioUseCaseTest {

    private val repository: ChatRepository = mockk()
    private val useCase = TranscribeAudioUseCase(repository)

    @Test
    fun `invoke delegates to repository and forwards Success`() = runTest {
        val audio = File("sample.m4a")
        coEvery { repository.transcribeAudio(audio) } returns Resource.Success("hello world")

        val result = useCase(audio)

        assertTrue(result is Resource.Success)
        assertEquals("hello world", (result as Resource.Success).data)
        coVerify(exactly = 1) { repository.transcribeAudio(audio) }
    }

    @Test
    fun `invoke forwards Error from repository unchanged`() = runTest {
        val audio = File("sample.m4a")
        val cause = RuntimeException("network")
        coEvery { repository.transcribeAudio(audio) } returns Resource.Error("Offline", cause)

        val result = useCase(audio)

        assertTrue(result is Resource.Error)
        val error = result as Resource.Error
        assertEquals("Offline", error.message)
        assertEquals(cause, error.throwable)
    }

    @Test
    fun `invoke rejects non-existent or empty audio files with Error`() = runTest {
        val empty = File("does-not-exist.m4a")

        val result = useCase(empty)

        assertTrue(result is Resource.Error)
        coVerify(exactly = 0) { repository.transcribeAudio(any()) }
    }
}
