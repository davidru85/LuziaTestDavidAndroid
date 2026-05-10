package com.ruizurraca.luziatestdavid.data.transcription

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * Unit-level coverage for [OnDeviceTranscriptionDataSourceImpl] is limited to the
 * SDK gate: ML Kit's GenAI Speech Recognition requires API 31+, so on older
 * devices we must short-circuit `isAvailable()` to `false` without ever loading
 * ML Kit classes. The actual ML Kit interaction (downloadable status, real
 * transcription) is verified manually on a Pixel device — see TECHNICAL_SPEC
 * §Testing §Manual.
 */
class OnDeviceTranscriptionDataSourceImplTest {

    @Test
    fun `isAvailable returns false when API level is below 31`() = runTest {
        val ds = OnDeviceTranscriptionDataSourceImpl(
            context = mockk(relaxed = true),
            sdkInt = 30
        )

        assertFalse(ds.isAvailable())
    }
}
