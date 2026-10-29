package com.quietringtones.trim

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Sonido ya decodificado a PCM de 16 bits, guardado en un archivo temporal para no llenar la memoria. */
private data class Pcm(val sampleRate: Int, val channels: Int, val frames: Long)

/**
 * Recorta un audio: decodifica la parte elegida y la guarda como AAC (.m4a). Si este móvil no puede
 * codificar AAC, la deja como WAV sin comprimir, más grande pero siempre válido como tono.
 */
class AudioTrimmer(private val context: Context) {

    fun durationMs(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    }

    fun trim(uri: Uri, startMs: Long, lengthMs: Long, workDir: File): File {
        workDir.mkdirs()
        workDir.listFiles()?.forEach { it.delete() }
        val pcmFile = File(workDir, "audio.pcm")
        try {
            val pcm = decode(uri, startMs, lengthMs, pcmFile)
            val aac = File(workDir, "trim.m4a")
            return try {
                encodeAac(pcmFile, pcm, aac)
            } catch (e: Exception) {
                aac.delete()
                writeWav(pcmFile, pcm, File(workDir, "trim.wav"))
            }
        } finally {
            pcmFile.delete()
        }
    }

    // --- Decodificar --------------------------------------------------------------

    private fun decode(uri: Uri, startMs: Long, lengthMs: Long, pcmFile: File): Pcm {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        val track = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: error("No audio track")
        extractor.selectTrack(track)
        val inFormat = extractor.getTrackFormat(track)
        val mime = inFormat.getString(MediaFormat.KEY_MIME)!!
        var sampleRate = inFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = inFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val startUs = startMs * 1000
        val endUs = (startMs + lengthMs) * 1000
        // Se empieza en un punto de sincronización anterior y se descartan las muestras sobrantes con precisión.
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(inFormat, null, null, 0)
        codec.start()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var done = false
        var written = 0L
        try {
            pcmFile.outputStream().buffered().use { out ->
                while (!done) {
                    if (!inputDone) {
                        val index = codec.dequeueInputBuffer(10_000)
                        if (index >= 0) {
                            val buffer = codec.getInputBuffer(index)!!
                            val size = extractor.readSampleData(buffer, 0)
                            val time = extractor.sampleTime
                            if (size < 0 || time > endUs) {
                                codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(index, 0, size, time, 0)
                                extractor.advance()
                            }
                        }
                    }
                    val output = codec.dequeueOutputBuffer(info, 10_000)
                    when {
                        output == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val format = codec.outputFormat
                            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            if (format.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                                format.getInteger(MediaFormat.KEY_PCM_ENCODING) != android.media.AudioFormat.ENCODING_PCM_16BIT
                            ) error("Unsupported audio encoding")
                        }
                        output >= 0 -> {
                            val frameBytes = 2 * channels
                            val frames = info.size / frameBytes
                            val wanted = lengthMs * sampleRate / 1000
                            var from = 0
                            if (info.presentationTimeUs < startUs) {
                                from = ((startUs - info.presentationTimeUs) * sampleRate / 1_000_000).toInt().coerceAtMost(frames)
                            }
                            val to = minOf(frames.toLong(), from + (wanted - written)).toInt()
                            if (to > from) {
                                val buffer = codec.getOutputBuffer(output)!!
                                val bytes = ByteArray((to - from) * frameBytes)
                                buffer.position(info.offset + from * frameBytes)
                                buffer.get(bytes)
                                out.write(bytes)
                                written += to - from
                            }
                            codec.releaseOutputBuffer(output, false)
                            if (written >= wanted || info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) done = true
                        }
                    }
                }
            }
        } finally {
            runCatching { codec.stop() }
            codec.release()
            extractor.release()
        }
        check(written > 0) { "Nothing was decoded" }
        return Pcm(sampleRate, channels, written)
    }

    // --- Codificar AAC ------------------------------------------------------------

    private fun encodeAac(pcmFile: File, pcm: Pcm, out: File): File {
        require(pcm.channels in 1..2) { "AAC here supports mono or stereo" }
        val frameBytes = 2 * pcm.channels
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, pcm.sampleRate, pcm.channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 64_000 * pcm.channels)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var track = -1
        var muxerStarted = false
        val info = MediaCodec.BufferInfo()
        val chunk = ByteArray(8 * 1024 - (8 * 1024 % frameBytes))
        var inputDone = false
        var done = false
        var framesFed = 0L
        pcmFile.inputStream().buffered().use { input ->
            try {
                while (!done) {
                    if (!inputDone) {
                        val index = codec.dequeueInputBuffer(10_000)
                        if (index >= 0) {
                            val buffer = codec.getInputBuffer(index)!!
                            buffer.clear()
                            val max = minOf(chunk.size, (buffer.capacity() / frameBytes) * frameBytes)
                            val read = readFully(input, chunk, max)
                            val time = framesFed * 1_000_000 / pcm.sampleRate
                            if (read <= 0) {
                                codec.queueInputBuffer(index, 0, 0, time, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                buffer.put(chunk, 0, read)
                                codec.queueInputBuffer(index, 0, read, time, 0)
                                framesFed += read / frameBytes
                            }
                        }
                    }
                    val output = codec.dequeueOutputBuffer(info, 10_000)
                    when {
                        output == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            track = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        output >= 0 -> {
                            val buffer = codec.getOutputBuffer(output)!!
                            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                            if (info.size > 0 && muxerStarted) {
                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                muxer.writeSampleData(track, buffer, info)
                            }
                            codec.releaseOutputBuffer(output, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) done = true
                        }
                    }
                }
            } finally {
                runCatching { codec.stop() }
                codec.release()
                runCatching { if (muxerStarted) muxer.stop() }
                muxer.release()
            }
        }
        check(muxerStarted && out.length() > 0) { "The encoder produced nothing" }
        return out
    }

    /** Lee hasta [length] bytes sin quedarse a medias: un fotograma partido deformaría el audio. */
    private fun readFully(input: InputStream, buffer: ByteArray, length: Int): Int {
        var total = 0
        while (total < length) {
            val read = input.read(buffer, total, length - total)
            if (read < 0) break
            total += read
        }
        return total
    }

    // --- WAV de reserva -----------------------------------------------------------

    private fun writeWav(pcmFile: File, pcm: Pcm, out: File): File {
        val dataBytes = pcmFile.length()
        out.outputStream().buffered().use { stream ->
            stream.write(wavHeader(pcm.sampleRate, pcm.channels, dataBytes))
            pcmFile.inputStream().use { it.copyTo(stream as OutputStream) }
        }
        return out
    }

    companion object {
        /** Cabecera WAV de 44 bytes para PCM de 16 bits. */
        fun wavHeader(sampleRate: Int, channels: Int, dataBytes: Long): ByteArray {
            val byteRate = sampleRate * channels * 2
            return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray())
                putInt((36 + dataBytes).toInt())
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16)
                putShort(1)
                putShort(channels.toShort())
                putInt(sampleRate)
                putInt(byteRate)
                putShort((channels * 2).toShort())
                putShort(16)
                put("data".toByteArray())
                putInt(dataBytes.toInt())
            }.array()
        }
    }
}
