package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Vibrator
import android.os.Build
import com.example.R
import java.util.Collections
import java.util.concurrent.Executors

class SoundHelper(context: Context) {
    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val executor = Executors.newSingleThreadExecutor()

    private var toneGenerator: ToneGenerator? = null

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()

    private val loadedSoundIds = Collections.synchronizedSet(mutableSetOf<Int>())
    private var applauseSoundId: Int = -1

    init {
        // Initialize ToneGenerator for zero-latency, highly stable system beep/tick tones
        try {
            // Using standard stream music so it respects volume slider perfectly
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
            }
        }

        // Load applause resource cleanly on a background thread
        executor.submit {
            try {
                applauseSoundId = soundPool.load(context.applicationContext, R.raw.applause_cheer, 1)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playTick() {
        try {
            // TONE_CDMA_PIP is a very clean, short, zero-latency tick/pip sound
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 80)
            vibrate(50)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playStrongBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
            vibrate(150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playDoubleBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 220)
            vibrate(250)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSetFinished() {
        try {
            // zero-latency, highly stable applause playback from SoundPool
            if (applauseSoundId != -1 && loadedSoundIds.contains(applauseSoundId)) {
                soundPool.play(applauseSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
            } else {
                playStrongBeep()
            }

            executor.submit {
                try {
                    vibrate(150)
                    Thread.sleep(250)
                    vibrate(150)
                    Thread.sleep(250)
                    vibrate(400)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(ms: Long) {
        try {
            vibrator?.vibrate(ms)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            executor.shutdown()
            toneGenerator?.release()
            soundPool.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
