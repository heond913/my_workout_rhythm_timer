package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsHelper(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingSpeakText: Pair<String, Int>? = null

    init {
        try {
            val resolvedContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.createAttributionContext("timer")
            } else {
                context.applicationContext
            }
            tts = TextToSpeech(resolvedContext, this)
        } catch (e: Exception) {
            Log.e("TtsHelper", "Failed to initialize TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Get currently active app/device locale dynamically to sync voice with language settings
            val currentLocale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            } ?: Locale.getDefault()

            val result = tts?.setLanguage(currentLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsHelper", "Active locale $currentLocale is not supported or resources are missing, fallback to English or Korean.")
                tts?.setLanguage(Locale.ENGLISH)
            }
            
            isInitialized = true
            // Sightly higher pitch and normal/fast speech rate for energy and active workout tempo
            tts?.setPitch(1.05f)
            tts?.setSpeechRate(1.1f)

            // Match STREAM_MUSIC (Media) volume controls
            try {
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
            } catch (e: Exception) {
                Log.e("TtsHelper", "Failed to set AudioAttributes for TTS", e)
            }

            pendingSpeakText?.let { (text, mode) ->
                speak(text, mode)
                pendingSpeakText = null
            }
        } else {
            Log.e("TtsHelper", "TTS Initialization failed with status: $status")
        }
    }

    /**
     * Converts an integer count into numbers based on current active locale (Korean: "하나", English: "one")
     */
    fun getNumberWord(count: Int): String {
        if (count <= 0) return ""
        val currentLocale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        } ?: Locale.getDefault()

        return if (currentLocale.language == "ko") {
            getKoreanNumberWord(count)
        } else {
            when (count) {
                1 -> "one"
                2 -> "two"
                3 -> "three"
                4 -> "four"
                5 -> "five"
                6 -> "six"
                7 -> "seven"
                8 -> "eight"
                9 -> "nine"
                10 -> "ten"
                else -> "$count"
            }
        }
    }

    /**
     * Converts an integer count into native Korean numbers (e.g., 1 -> "하나", 2 -> "둘", etc.)
     */
    fun getKoreanNumberWord(count: Int): String {
        if (count <= 0) return ""
        val nativeKoreanUnits = listOf("", "하나", "둘", "셋", "넷", "다섯", "여섯", "일곱", "여덟", "아홉")
        val nativeKoreanTens = listOf("", "열", "스물", "서른", "마흔", "쉰", "예순", "일흔", "여든", "아흔")
        
        return when {
            count < 10 -> nativeKoreanUnits[count]
            count < 100 -> {
                val ten = count / 10
                val unit = count % 10
                nativeKoreanTens[ten] + nativeKoreanUnits[unit]
            }
            else -> "$count"
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (isInitialized && tts != null) {
            try {
                val params = android.os.Bundle()
                // Play at max dynamic volume parameter for maximum loudness
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                tts?.speak(text, queueMode, params, "WorkoutTtsId_${System.currentTimeMillis()}")
            } catch (e: Exception) {
                Log.e("TtsHelper", "Error speaking text: ${e.message}")
            }
        } else {
            pendingSpeakText = Pair(text, queueMode)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
