package com.ruizurraca.luziatestdavid.data.local.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.ruizurraca.luziatestdavid.di.qualifier.MainDispatcher
import com.ruizurraca.luziatestdavid.domain.audio.AudioRecorder
import com.ruizurraca.luziatestdavid.domain.common.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRecorderAudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher
) : AudioRecorder {

    private var currentRecorder: MediaRecorder? = null
    private var currentFile: File? = null

    override suspend fun start(): Resource<Unit> = withContext(mainDispatcher) {
        if (currentRecorder != null) {
            return@withContext Resource.Error("Recording already in progress.")
        }
        try {
            val file = createOutputFile()
            val recorder = newRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            currentRecorder = recorder
            currentFile = file
            Resource.Success(Unit)
        } catch (e: Throwable) {
            releaseQuietly()
            Resource.Error(e.message ?: "Failed to start recording.", e)
        }
    }

    override suspend fun stop(): Resource<File> = withContext(mainDispatcher) {
        val recorder = currentRecorder
            ?: return@withContext Resource.Error("No active recording.")
        val file = currentFile
            ?: return@withContext Resource.Error("No output file for recording.")
        try {
            recorder.stop()
            recorder.release()
            currentRecorder = null
            currentFile = null
            Resource.Success(file)
        } catch (e: Throwable) {
            releaseQuietly()
            Resource.Error(e.message ?: "Failed to stop recording.", e)
        }
    }

    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    private fun createOutputFile(): File {
        val dir = File(context.cacheDir, "audio").apply { mkdirs() }
        return File(dir, "luzia_${System.currentTimeMillis()}.m4a")
    }

    private fun releaseQuietly() {
        try {
            currentRecorder?.release()
        } catch (_: Throwable) {
            // Best-effort cleanup; swallow secondary failures.
        }
        currentRecorder = null
        currentFile = null
    }
}
