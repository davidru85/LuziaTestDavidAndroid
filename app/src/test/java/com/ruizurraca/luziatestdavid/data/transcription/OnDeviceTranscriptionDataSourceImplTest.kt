package com.ruizurraca.luziatestdavid.data.transcription

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit-level coverage for [OnDeviceTranscriptionDataSourceImpl] is limited to the
 * SDK gate: ML Kit's GenAI Speech Recognition requires API 31+, so on older
 * devices we must short-circuit `isAvailable()` to `false` without ever calling
 * the recognizer. The actual ML Kit interaction (downloadable status, real
 * transcription) is verified manually on a Pixel device.
 *
 * Runs under Robolectric so `android.util.Log` (used for on-device diagnostics)
 * resolves to a no-op shadow instead of throwing "not mocked".
 */
@RunWith(RobolectricTestRunner::class)
class OnDeviceTranscriptionDataSourceImplTest {

    @Test
    fun `isAvailable returns false when API level is below 31`() = runTest {
        val ds = OnDeviceTranscriptionDataSourceImpl(
            context = mockk(relaxed = true),
            sdkInt = 30,
            m4aToWavConverter = mockk(relaxed = true)
        )

        assertFalse(ds.isAvailable())
    }
}
