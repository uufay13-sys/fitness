package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class AuraVoiceHelper(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit
) {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    var isVoiceEnabled: Boolean = true

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.US)
                    isTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                            result != TextToSpeech.LANG_NOT_SUPPORTED
                    tts?.setPitch(1.05f)
                    tts?.setSpeechRate(1.0f)
                }
            }
        } catch (e: Exception) {
            Log.e("AuraVoiceHelper", "TTS init failed", e)
        }
    }

    fun speak(text: String) {
        if (!isVoiceEnabled || !isTtsReady) return
        try {
            // Strip markdown, asterisks, brackets from speech
            val cleanSpeech = text
                .replace(Regex("\\[.*?\\]"), "")
                .replace(Regex("[*#_`•]"), "")
                .trim()

            if (cleanSpeech.isNotEmpty()) {
                tts?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, "aura_utterance")
            }
        } catch (e: Exception) {
            Log.e("AuraVoiceHelper", "Speak error", e)
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onListeningStateChanged(false)
            return
        }

        try {
            stopSpeaking()
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        onListeningStateChanged(true)
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        onListeningStateChanged(false)
                    }
                    override fun onError(error: Int) {
                        onListeningStateChanged(false)
                    }
                    override fun onResults(results: Bundle?) {
                        onListeningStateChanged(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onSpeechResult(matches[0])
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("AuraVoiceHelper", "SpeechRecognizer start error", e)
            onListeningStateChanged(false)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            onListeningStateChanged(false)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // ignore
        }
    }
}
