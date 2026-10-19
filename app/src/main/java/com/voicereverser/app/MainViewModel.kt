package com.voicereverser.app

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Application = app
    private val main = Handler(Looper.getMainLooper())
    private val recorder = Recorder()
    private val player = Player()

    var current by mutableStateOf<AudioClip?>(null)
        private set
    var currentSource by mutableStateOf("")
        private set
    var reference by mutableStateOf<AudioClip?>(null)
        private set
    var lastFx by mutableStateOf<AudioClip?>(null)
        private set
    var lastFxLabel by mutableStateOf("")
        private set
    var isRecording by mutableStateOf(false)
        private set
    var recordMs by mutableStateOf(0L)
        private set
    var loading by mutableStateOf(false)
        private set
    var playingKey by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    var noteDraft by mutableStateOf("")

    val history = mutableStateListOf<HistoryItem>()
    val notes = mutableStateListOf<Note>()

    init {
        history.addAll(HistoryRepo.list(ctx))
        notes.addAll(NotesRepo.load(ctx))
    }

    fun say(m: String) {
        message = m
    }

    fun consumeMessage() {
        message = null
    }

    private fun noClip() {
        say("سجّل أو ارفع صوت أول")
    }

    // ---------- التسجيل ----------
    fun startRecording() {
        stopPlayback()
        val ok = recorder.start(
            onTick = { ms -> main.post { recordMs = ms } },
            onAutoStop = { main.post { stopRecording() } }
        )
        if (ok) {
            isRecording = true
            recordMs = 0L
        } else {
            say("تعذّر بدء التسجيل")
        }
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        val clip = recorder.stop()
        if (clip == null) {
            say("التسجيل فاضي")
            return
        }
        current = clip
        currentSource = "تسجيلك"
        lastFx = null
        saveClip(clip, "rec", true)
    }

    // ---------- رفع ملف ----------
    fun importFile(uri: Uri) {
        stopPlayback()
        loading = true
        Thread {
            var name: String? = null
            try {
                val cursor = ctx.contentResolver.query(
                    uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                )
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            name = cursor.getString(0)
                        }
                    } finally {
                        cursor.close()
                    }
                }
            } catch (e: Exception) {
            }
            val clip = AudioDecoder.decode(ctx, uri)
            val shownName = name ?: "ملف صوتي"
            main.post {
                loading = false
                if (clip == null || clip.samples.isEmpty()) {
                    say("ما قدرت أقرأ هذا الملف")
                } else {
                    current = clip
                    currentSource = shownName
                    lastFx = null
                }
            }
        }.start()
    }

    // ---------- التشغيل ----------
    /** يجهّز الصوت بخيط خلفي ثم يشغّله، وما يحجز الواجهة أبداً. */
    private fun playClips(key: String, build: () -> List<AudioClip>) {
        if (playingKey == key) {
            stopPlayback()
            return
        }
        stopPlayback()
        playingKey = key
        Thread {
            val clips = build()
            main.post {
                if (playingKey == key) {
                    if (clips.isEmpty()) {
                        playingKey = null
                        say("تعذّر التشغيل")
                    } else {
                        startPlayback(key, clips)
                    }
                }
            }
        }.start()
    }

    private fun startPlayback(key: String, clips: List<AudioClip>) {
        player.play(clips) {
            main.post { if (playingKey == key) playingKey = null }
        }
    }

    fun stopPlayback() {
        player.stop()
        playingKey = null
    }

    fun playNormal() {
        val c = current ?: return noClip()
        playClips("normal") { listOf(c) }
    }

    fun playReversed() {
        val c = current ?: return noClip()
        playClips("reversed") { listOf(c.reversed()) }
    }

    fun setReference() {
        val c = current ?: return noClip()
        reference = c
        say("تم تثبيت المرجع")
    }

    /** الأصل (المرجع) عادي، ثم محاولتك معكوسة. */
    fun compare() {
        val r = reference
        val c = current
        if (r == null) {
            say("ثبّت المرجع أول (الأغنية الأصلية)")
            return
        }
        if (c == null) {
            noClip()
            return
        }
        playClips("compare") { listOf(r, c.reversed()) }
    }

    // ---------- المؤثرات ----------
    fun playEffect(effect: VoiceEffect, alsoReverse: Boolean) {
        val c = current ?: return noClip()
        val key = "fx:" + effect.name
        if (playingKey == key) {
            stopPlayback()
            return
        }
        stopPlayback()
        playingKey = key
        Thread {
            var out = Effects.apply(c, effect)
            if (alsoReverse) out = out.reversed()
            val label = effect.label + if (alsoReverse) " + معكوس" else ""
            main.post {
                lastFx = out
                lastFxLabel = label
                if (playingKey == key) startPlayback(key, listOf(out))
            }
        }.start()
    }

    // ---------- الحفظ والمشاركة ----------
    private fun saveClip(clip: AudioClip, kind: String, silent: Boolean) {
        Thread {
            val item = HistoryRepo.save(ctx, clip, kind)
            main.post {
                history.add(0, item)
                if (!silent) say("تم الحفظ بالسجل")
            }
        }.start()
    }

    fun saveReversed() {
        val c = current ?: return noClip()
        saveClip(c.reversed(), "rev", false)
    }

    fun saveFx() {
        val c = lastFx ?: return
        saveClip(c, "fx", false)
    }

    fun prepareShareReversed(onReady: (File) -> Unit) {
        val c = current ?: return noClip()
        Thread {
            val f = HistoryRepo.cacheFile(ctx, "reversed_voice")
            WavIO.write(f, c.reversed())
            main.post { onReady(f) }
        }.start()
    }

    fun prepareShareFx(onReady: (File) -> Unit) {
        val c = lastFx ?: return
        Thread {
            val f = HistoryRepo.cacheFile(ctx, "effect_voice")
            WavIO.write(f, c)
            main.post { onReady(f) }
        }.start()
    }

    // ---------- السجل ----------
    fun playItem(item: HistoryItem, reversed: Boolean) {
        val key = "h:" + item.file.name + (if (reversed) ":r" else ":n")
        playClips(key) {
            val clip = WavIO.read(item.file)
            if (clip == null) emptyList() else listOf(if (reversed) clip.reversed() else clip)
        }
    }

    fun useItem(item: HistoryItem, asReference: Boolean) {
        Thread {
            val clip = WavIO.read(item.file)
            main.post {
                if (clip == null) {
                    say("تعذّر فتح الملف")
                } else if (asReference) {
                    reference = clip
                    say("تم تثبيت المرجع")
                } else {
                    current = clip
                    currentSource = "من السجل"
                    lastFx = null
                    say("صار هذا الصوت الحالي")
                }
            }
        }.start()
    }

    fun deleteItem(item: HistoryItem) {
        item.file.delete()
        history.remove(item)
    }

    fun clearHistory() {
        stopPlayback()
        HistoryRepo.clear(ctx)
        history.clear()
    }

    // ---------- الملاحظات ----------
    fun addNote(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        val now = System.currentTimeMillis()
        notes.add(0, Note(now, t, now))
        NotesRepo.save(ctx, notes.toList())
    }

    fun deleteNote(note: Note) {
        notes.remove(note)
        NotesRepo.save(ctx, notes.toList())
    }

    override fun onCleared() {
        player.stop()
        if (isRecording) recorder.stop()
        super.onCleared()
    }
}
