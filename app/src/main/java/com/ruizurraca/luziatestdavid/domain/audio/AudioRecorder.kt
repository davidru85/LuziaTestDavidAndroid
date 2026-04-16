package com.ruizurraca.luziatestdavid.domain.audio

import com.ruizurraca.luziatestdavid.domain.common.Resource
import java.io.File

interface AudioRecorder {

    suspend fun start(): Resource<Unit>

    suspend fun stop(): Resource<File>
}
