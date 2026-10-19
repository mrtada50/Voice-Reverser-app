package com.voicereverser.app

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaRecorder
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/** مقطع صوتي: عينات PCM 16-bit أحادية (mono). */
class AudioClip(val samples: ShortArray, val sampleRate: Int) {
    val durationMs: Long
        get() = if (sampleRate > 0) samples.size * 1000L / sampleRate else 0L

    fun reversed(): AudioClip {
        val copy = samples.copyOf()
        copy.reverse()
        return AudioClip(copy, sampleRate)
    }
}

/** قراءة وكتابة ملفات WAV (16-bit mono) الخاصة بالتطبيق. */
object WavIO {
    fun write(file: File, clip: AudioClip) {
        val dataLen = clip.samples.size * 2
        val bb = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray())
        bb.putInt(36 + dataLen)
        bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray())
        bb.putInt(16)
        bb.putShort(1.toShort())
        bb.putShort(1.toShort())
        bb.putInt(clip.sampleRate)
        bb.putInt(clip.sampleRate * 2)
        bb.putShort(2.toShort())
        bb.putShort(16.toShort())
        bb.put("data".toByteArray())
        bb.putInt(dataLen)
        for (s in clip.samples) bb.putShort(s)
        file.writeBytes(bb.array())
    }

    fun read(file: File): AudioClip? {
        return try {
            val bytes = file.readBytes()
            if (bytes.size < 44) return null
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val rate = bb.getInt(24)
            val n = (bytes.size - 44) / 2
            val out = ShortArray(n)
            bb.position(44)
            bb.asShortBuffer().get(out)
            AudioClip(out, rate)
        } catch (e: Exception) {
            null
        }
    }
}

/** تسجيل من المايك. */
class Recorder {
    val sampleRate = 44100
    private val maxMs = 120_000L
    private var record: AudioRecord? = null
    @Volatile
    private var running = false
    private var thread: Thread? = null
    private val chunks = ArrayList<ShortArray>()

    @SuppressLint("MissingPermission")
    fun start(onTick: (Long) -> Unit, onAutoStop: () -> Unit): Boolean {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) return false
        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2
            )
        } catch (e: Exception) {
            return false
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return false
        }
        synchronized(chunks) { chunks.clear() }
        record = rec
        running = true
        rec.startRecording()
        thread = Thread {
            val buf = ShortArray(minBuf)
            var total = 0L
            while (running) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) {
                    synchronized(chunks) { chunks.add(buf.copyOf(n)) }
                    total += n
                    val ms = total * 1000 / sampleRate
                    onTick(ms)
                    if (ms >= maxMs) {
                        running = false
                        onAutoStop()
                    }
                } else if (n < 0) {
                    break
                }
            }
        }.also { it.start() }
        return true
    }

    fun stop(): AudioClip? {
        running = false
        try {
            thread?.join()
        } catch (e: InterruptedException) {
        }
        thread = null
        val rec = record
        if (rec != null) {
            try {
                rec.stop()
            } catch (e: Exception) {
            }
            rec.release()
        }
        record = null
        val snapshot = synchronized(chunks) {
            val copy = ArrayList(chunks)
            chunks.clear()
            copy
        }
        var total = 0
        for (c in snapshot) total += c.size
        if (total == 0) return null
        val out = ShortArray(total)
        var pos = 0
        for (c in snapshot) {
            c.copyInto(out, pos)
            pos += c.size
        }
        return AudioClip(out, sampleRate)
    }
}

/** تشغيل مقاطع بالتسلسل. */
class Player {
    @Volatile
    private var stopFlag = false
    @Volatile
    private var gen = 0
    private var thread: Thread? = null

    fun play(clips: List<AudioClip>, onDone: () -> Unit) {
        stop()
        stopFlag = false
        val myGen = gen
        thread = Thread {
            try {
                for (clip in clips) {
                    if (stopFlag) break
                    playOne(clip)
                }
            } catch (e: Exception) {
            } finally {
                if (myGen == gen) onDone()
            }
        }.also { it.start() }
    }

    fun stop() {
        gen++
        stopFlag = true
        val t = thread
        if (t != null && t !== Thread.currentThread()) {
            try {
                t.join(1500)
            } catch (e: InterruptedException) {
            }
        }
        thread = null
    }

    private fun playOne(clip: AudioClip) {
        val minBuf = AudioTrack.getMinBufferSize(
            clip.sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(clip.sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, 8192))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()
        var pos = 0
        val chunk = 4096
        while (pos < clip.samples.size && !stopFlag) {
            val n = minOf(chunk, clip.samples.size - pos)
            val w = track.write(clip.samples, pos, n)
            if (w <= 0) break
            pos += w
        }
        if (!stopFlag) {
            track.stop()
            val deadline = System.currentTimeMillis() + 3000
            while (!stopFlag &&
                track.playbackHeadPosition < clip.samples.size &&
                System.currentTimeMillis() < deadline
            ) {
                Thread.sleep(20)
            }
        } else {
            track.pause()
            track.flush()
        }
        track.release()
    }
}

/** فك ضغط أي ملف صوتي (mp3, m4a, wav, ...) إلى PCM أحادي. */
object AudioDecoder {
    fun decode(context: Context, uri: Uri, maxSeconds: Int = 300): AudioClip? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    format = f
                    break
                }
            }
            if (trackIndex < 0 || format == null) return null
            extractor.selectTrack(trackIndex)
            val mimeType = format.getString(MediaFormat.KEY_MIME) ?: return null
            val c = MediaCodec.createDecoderByType(mimeType)
            codec = c
            c.configure(format, null, null, 0)
            c.start()

            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val info = MediaCodec.BufferInfo()
            var out = ShortArray(sampleRate * 10)
            var outLen = 0
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = c.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val inBuf = c.getInputBuffer(inIdx)!!
                        val size = extractor.readSampleData(inBuf, 0)
                        if (size < 0) {
                            c.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            c.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = c.dequeueOutputBuffer(info, 10_000)
                if (outIdx >= 0) {
                    val outBuf = c.getOutputBuffer(outIdx)!!
                    outBuf.position(info.offset)
                    outBuf.limit(info.offset + info.size)
                    val sb = outBuf.order(ByteOrder.nativeOrder()).asShortBuffer()
                    val count = sb.remaining()
                    val ch = if (channels > 0) channels else 1
                    val frames = count / ch
                    val tmp = ShortArray(count)
                    sb.get(tmp)
                    if (outLen + frames > out.size) {
                        out = out.copyOf(maxOf(out.size * 2, outLen + frames))
                    }
                    for (f in 0 until frames) {
                        var sum = 0
                        for (k in 0 until ch) sum += tmp[f * ch + k].toInt()
                        out[outLen++] = (sum / ch).toShort()
                    }
                    c.releaseOutputBuffer(outIdx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
                    if (outLen > sampleRate * maxSeconds) outputDone = true
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val nf = c.outputFormat
                    sampleRate = nf.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = nf.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
            }
            c.stop()
            val limit = sampleRate * maxSeconds
            return AudioClip(out.copyOf(minOf(outLen, limit)), sampleRate)
        } catch (e: Exception) {
            return null
        } finally {
            try {
                codec?.release()
            } catch (e: Exception) {
            }
            extractor.release()
        }
    }
}

enum class VoiceEffect(val label: String) {
    CHILD("طفل"),
    DEEP("عميق"),
    ROBOT("روبوت"),
    ALIEN("فضائي"),
    ECHO("صدى"),
    FAST("سريع"),
    SLOW("بطيء")
}

/** مؤثرات صوتية بسيطة بالكامل أوفلاين. */
object Effects {
    fun apply(clip: AudioClip, effect: VoiceEffect): AudioClip = when (effect) {
        VoiceEffect.CHILD -> resample(clip, 1.45)
        VoiceEffect.DEEP -> resample(clip, 0.72)
        VoiceEffect.FAST -> resample(clip, 1.6)
        VoiceEffect.SLOW -> resample(clip, 0.6)
        VoiceEffect.ROBOT -> ringMod(clip, 60.0)
        VoiceEffect.ALIEN -> ringMod(resample(clip, 1.25), 220.0)
        VoiceEffect.ECHO -> echo(clip)
    }

    private fun clamp(v: Int): Short =
        v.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

    private fun resample(clip: AudioClip, factor: Double): AudioClip {
        val size = clip.samples.size
        if (size == 0) return clip
        val n = (size / factor).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val pos = i * factor
            val i0 = pos.toInt()
            val frac = pos - i0
            val a = clip.samples[i0].toInt()
            val b = clip.samples[minOf(i0 + 1, size - 1)].toInt()
            out[i] = (a + (b - a) * frac).toInt().toShort()
        }
        return AudioClip(out, clip.sampleRate)
    }

    private fun ringMod(clip: AudioClip, freq: Double): AudioClip {
        val out = ShortArray(clip.samples.size)
        val w = 2.0 * PI * freq / clip.sampleRate
        for (i in out.indices) {
            out[i] = clamp((clip.samples[i] * sin(w * i) * 1.4).toInt())
        }
        return AudioClip(out, clip.sampleRate)
    }

    private fun echo(clip: AudioClip): AudioClip {
        val delay = clip.sampleRate * 280 / 1000
        val repeats = 3
        val outLen = clip.samples.size + delay * repeats
        val acc = IntArray(outLen)
        var gain = 1.0
        for (r in 0..repeats) {
            val off = delay * r
            for (i in clip.samples.indices) {
                acc[off + i] += (clip.samples[i] * gain).toInt()
            }
            gain *= 0.5
        }
        return AudioClip(ShortArray(outLen) { clamp(acc[it]) }, clip.sampleRate)
    }
}
