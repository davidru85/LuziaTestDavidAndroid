package com.ruizurraca.luziatestdavid.domain.usecase

import com.ruizurraca.luziatestdavid.domain.common.AppError
import com.ruizurraca.luziatestdavid.domain.common.Resource
import com.ruizurraca.luziatestdavid.domain.repository.ChatRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TranscribeAudioUseCaseTest {

    private val repository: ChatRepository = mockk()
    private val useCase = TranscribeAudioUseCase(repository)

    @TempDir
    lateinit var tempDir: File

    private fun audioFile(name: String = "sample.m4a"): File =
        File(tempDir, name).apply { writeBytes(byteArrayOf(0x1, 0x2, 0x3)) }

    @Test
    fun `invoke delegates to repository and forwards Success`() = runTest {
        val audio = audioFile()
        coEvery { repository.transcribeAudio(audio) } returns Resource.Success("hello world")

        val result = useCase(audio)

        assertTrue(result is Resource.Success)
        assertEquals("hello world", (result as Resource.Success).data)
        coVerify(exactly = 1) { repository.transcribeAudio(audio) }
    }

    @Test
    fun `invoke forwards Error from repository unchanged`() = runTest {
        val audio = audioFile()
        val cause = RuntimeException("network")
        coEvery { repository.transcribeAudio(audio) } returns Resource.Error("Offline", cause)

        val result = useCase(audio)

        assertTrue(result is Resource.Error)
        val error = result as Resource.Error
        assertEquals("Offline", error.message)
        assertEquals(cause, error.throwable)
    }

    @Test
    fun `invoke rejects non-existent audio file with EmptyAudioFile AppError and skips repository`() = runTest {
        val missing = File(tempDir, "does-not-exist.m4a")

        val result = useCase(missing)

        assertTrue(result is Resource.Error)
        assertEquals(AppError.EmptyAudioFile, (result as Resource.Error).error)
        coVerify(exactly = 0) { repository.transcribeAudio(any()) }
    }

    @Test
    fun `invoke rejects empty audio file with EmptyAudioFile AppError and skips repository`() = runTest {
        val empty = File(tempDir, "empty.m4a").apply { createNewFile() }

        val result = useCase(empty)

        assertTrue(result is Resource.Error)
        assertEquals(AppError.EmptyAudioFile, (result as Resource.Error).error)
        coVerify(exactly = 0) { repository.transcribeAudio(any()) }
    }
}
