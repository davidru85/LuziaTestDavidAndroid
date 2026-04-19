package com.ruizurraca.luziatestdavid.domain.audio

import com.ruizurraca.luziatestdavid.domain.common.Resource
import java.io.File

interface AudioRecorder {

    suspend fun start(): Resource<Unit>

    suspend fun stop(): Resource<File>

    /**
     * Best-effort synchronous teardown for when the owning UI is going away while
     * a recording is still in flight (e.g. ViewModel `onCleared`). Implementations
     * release native resources and discard any in-flight temp file.
     * Safe to call from non-suspend contexts and safe to call repeatedly.
     */
    fun release()
}
