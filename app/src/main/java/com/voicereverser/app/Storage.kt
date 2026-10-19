package com.voicereverser.app

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class HistoryItem(
    val file: File,
    val kind: String,
    val timestamp: Long,
    val sampleRate: Int,
    val durationMs: Long
)

object HistoryRepo {
    private fun dir(ctx: Context): File = File(ctx.filesDir, "history").apply { mkdirs() }

    fun save(ctx: Context, clip: AudioClip, kind: String): HistoryItem {
        val ts = System.currentTimeMillis()
        val f = File(dir(ctx), "${ts}_${kind}_${clip.sampleRate}.wav")
        WavIO.write(f, clip)
        return item(f) ?: HistoryItem(f, kind, ts, clip.sampleRate, clip.durationMs)
    }

    fun list(ctx: Context): List<HistoryItem> {
        val files = dir(ctx).listFiles() ?: return emptyList()
        return files.mapNotNull { item(it) }.sortedByDescending { it.timestamp }
    }

    private fun item(f: File): HistoryItem? {
        val p = f.nameWithoutExtension.split("_")
        if (p.size != 3) return null
        val ts = p[0].toLongOrNull() ?: return null
        val rate = p[2].toIntOrNull() ?: return null
        if (rate <= 0) return null
        val dur = maxOf(0L, (f.length() - 44) / 2 * 1000 / rate)
        return HistoryItem(f, p[1], ts, rate, dur)
    }

    fun cacheFile(ctx: Context, name: String): File {
        val d = File(ctx.cacheDir, "share").apply { mkdirs() }
        return File(d, "$name.wav")
    }

    fun clear(ctx: Context) {
        dir(ctx).listFiles()?.forEach { it.delete() }
    }
}

class Note(val id: Long, val text: String, val ts: Long)

object NotesRepo {
    private fun file(ctx: Context) = File(ctx.filesDir, "notes.json")

    fun load(ctx: Context): List<Note> {
        val f = file(ctx)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Note(o.getLong("id"), o.getString("text"), o.getLong("ts"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(ctx: Context, notes: List<Note>) {
        val arr = JSONArray()
        for (n in notes) {
            arr.put(JSONObject().put("id", n.id).put("text", n.text).put("ts", n.ts))
        }
        file(ctx).writeText(arr.toString())
    }
}

object ShareUtil {
    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الصوت"))
    }
}
