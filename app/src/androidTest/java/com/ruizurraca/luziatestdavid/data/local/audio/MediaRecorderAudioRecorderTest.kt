package com.ruizurraca.luziatestdavid.data.local.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruizurraca.luziatestdavid.domain.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Minimal instrumented coverage for [MediaRecorderAudioRecorder].
 *
 * The happy path (actual audio capture) is covered manually in Phase 7 because it
 * requires RECORD_AUDIO permission and a real microphone. This test only verifies
 * the lifecycle guard: `stop()` without a prior `start()` must fail cleanly.
 */
@RunWith(AndroidJUnit4::class)
class MediaRecorderAudioRecorderTest {

    @Test
    fun stopWithoutStartReturnsError() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = MediaRecorderAudioRecorder(context, Dispatchers.Main)

        val result = recorder.stop()

        assertTrue(
            "Expected Resource.Error when stopping without a prior start, got $result",
            result is Resource.Error
        )
    }
}
