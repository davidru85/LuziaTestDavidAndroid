package com.ruizurraca.luziatestdavid.data.transcription

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decodes an AAC-in-MPEG-4 file (the format produced by [android.media.MediaRecorder]
 * with `OutputFormat.MPEG_4` + `AudioEncoder.AAC`) to a 16-bit PCM WAV file in
 * the cache directory.
 *
 * Why: ML Kit GenAI Speech Recognition's [com.google.mlkit.genai.common.audio.AudioSource.fromPfd]
 * reads the file as raw PCM. Feeding it AAC bytes results in
 * `ERROR_TYPE_NO_SPEECH_DETECTED` because the engine interprets compressed
 * audio as silence. Decoding to PCM and wrapping in a WAV header gives the
 * recognizer the format it expects.
 *
 * The converter preserves the source sample rate and channel count — ML Kit
 * basic mode appears to accept the common 44.1 kHz / 48 kHz mono outputs that
 * MediaRecorder produces. If a future device emits a rate the recognizer
 * rejects, add a resampling pass here.
 */
@Singleton
class M4aToWavConverter @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    fun convertToWav(input: File): File {
        val output = File(context.cacheDir, "transcribe_${System.currentTimeMillis()}.wav")
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(input.absolutePath)
            val trackIndex = pickAudioTrack(extractor)
                ?: error("No audio track in ${input.absolutePath}")
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            Log.d(TAG, "convertToWav: in=${input.length()}B mime=$mime sr=$sampleRate ch=$channels")

            val pcm = decodeToPcm(extractor, format, mime)
            writeWav(output, pcm, sampleRate, channels)
            Log.d(TAG, "convertToWav: out=${output.absolutePath} size=${output.length()}B pcmBytes=${pcm.size}")
            return output
        } finally {
            extractor.release()
        }
    }

    private fun pickAudioTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return null
    }

    private fun decodeToPcm(extractor: MediaExtractor, format: MediaFormat, mime: String): ByteArray {
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()
        val pcm = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        try {
            while (!outputDone) {
                if (!inputDone) {
                    val inputIdx = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIdx >= 0) {
                        val buf = codec.getInputBuffer(inputIdx)!!
                        val n = extractor.readSampleData(buf, 0)
                        if (n < 0) {
                            codec.queueInputBuffer(
                                inputIdx, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inputIdx, 0, n, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outputIdx = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                if (outputIdx >= 0) {
                    if (info.size > 0) {
                        val buf = codec.getOutputBuffer(outputIdx)!!
                        val chunk = ByteArray(info.size)
                        buf.position(info.offset)
                        buf.limit(info.offset + info.size)
                        buf.get(chunk)
                        pcm.write(chunk)
                    }
                    codec.releaseOutputBuffer(outputIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
        }
        return pcm.toByteArray()
    }

    private fun writeWav(out: File, pcm: ByteArray, sampleRate: Int, channels: Int) {
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + pcm.size)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcm.size)
        }
        out.outputStream().use {
            it.write(header.array())
            it.write(pcm)
        }
    }

    private companion object {
        const val TAG = "OnDeviceTranscribe"
        const val TIMEOUT_US = 10_000L
    }
}
