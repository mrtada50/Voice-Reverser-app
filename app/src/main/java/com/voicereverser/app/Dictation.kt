package com.voicereverser.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** إملاء صوتي يفضّل العمل أوفلاين عبر خدمة الكلام بالجهاز. */
class Dictation(private val context: Context, private val listener: Listener) {

    interface Listener {
        fun onPartial(t: String)
        fun onFinal(t: String)
        fun onError(message: String)
        fun onState(l: Boolean)
    }

    private var recognizer: SpeechRecognizer? = null

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError("خدمة التعرف على الكلام غير متوفرة بالجهاز")
            return
        }
        destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listener.onState(true)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                listener.onState(false)
                if (error != 5) listener.onError(errorText(error))
            }

            override fun onResults(results: Bundle?) {
                val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                listener.onFinal(t)
                listener.onState(false)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                listener.onPartial(t)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        r.startListening(intent)
    }

    fun stop() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun errorText(code: Int): String = when (code) {
        6 -> "ما سمعت صوت، جرّب مرة ثانية"
        7 -> "ما فهمت الكلام، جرّب مرة ثانية"
        1, 2, 12, 13 -> "الإملاء الأوفلاين يحتاج حزمة اللغة العربية: نزّلها من إعدادات خدمات جوجل للكلام"
        9 -> "ما اكو صلاحية للمايك"
        else -> "صار خطأ بالتعرف على الكلام (رمز $code)"
    }
}
