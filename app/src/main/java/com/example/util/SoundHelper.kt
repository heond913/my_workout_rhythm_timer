package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Vibrator
import com.example.R
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import java.util.concurrent.Executors

class SoundHelper(context: Context) {
    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val executor = Executors.newSingleThreadExecutor()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()

    private val loadedSoundIds = Collections.synchronizedSet(mutableSetOf<Int>())

    private var tickSoundId: Int = -1
    private var strongBeepSoundId: Int = -1
    private var doubleBeepSoundId: Int = -1
    private var applauseSoundId: Int = -1

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
            }
        }

        // Generate files and load them into SoundPool on a background thread
        executor.submit {
            try {
                // 1. Generate and load tick sound
                val tickFile = generateWavFile(context, 1050.0, 110, 1.0f, "timer_tick.wav")
                if (tickFile.exists()) {
                    tickSoundId = soundPool.load(tickFile.absolutePath, 1)
                }

                // 2. Generate and load strong beep sound
                val strongBeepFile = generateWavFile(context, 850.0, 260, 1.0f, "timer_strong_beep.wav")
                if (strongBeepFile.exists()) {
                    strongBeepSoundId = soundPool.load(strongBeepFile.absolutePath, 1)
                }

                // 3. Generate and load double beep sound
                val doubleBeepFile = generateDoubleBeepWav(context, "timer_double_beep.wav")
                if (doubleBeepFile.exists()) {
                    doubleBeepSoundId = soundPool.load(doubleBeepFile.absolutePath, 1)
                }

                // 4. Load applause resource cleanly
                try {
                    applauseSoundId = soundPool.load(context.applicationContext, R.raw.applause_cheer, 1)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun writeShortLe(dos: DataOutputStream, value: Int) {
        dos.write(value and 0xFF)
        dos.write((value shr 8) and 0xFF)
    }

    private fun writeIntLe(dos: DataOutputStream, value: Int) {
        dos.write(value and 0xFF)
        dos.write((value shr 8) and 0xFF)
        dos.write((value shr 16) and 0xFF)
        dos.write((value shr 24) and 0xFF)
    }

    /**
     * Synthesizes and writes a standard click-free 16-bit Mono PCM WAV file containing custom synth tone.
     */
    private fun generateWavFile(
        context: Context,
        frequencyHz: Double,
        durationMs: Int,
        volumeScale: Float,
        fileName: String
    ): File {
        val file = File(context.cacheDir, fileName)
        try {
            val sampleRate = 44100
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val rawDataSize = numSamples * 2 // 16-bit mono = 2 bytes per sample

            FileOutputStream(file).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    DataOutputStream(bos).use { dos ->
                        // 1. RIFF Chunk Descriptor
                        dos.writeBytes("RIFF")
                        writeIntLe(dos, 36 + rawDataSize)
                        dos.writeBytes("WAVE")

                        // 2. Format Chunk
                        dos.writeBytes("fmt ")
                        writeIntLe(dos, 16) // subchunk size
                        writeShortLe(dos, 1) // linear PCM representation
                        writeShortLe(dos, 1) // mono channel count
                        writeIntLe(dos, sampleRate)
                        writeIntLe(dos, sampleRate * 2) // byte rate
                        writeShortLe(dos, 2) // block align
                        writeShortLe(dos, 16) // bits per sample

                        // 3. Data Chunk
                        dos.writeBytes("data")
                        writeIntLe(dos, rawDataSize)

                        // Write PCM wave energy
                        val maxAmp = 30500.0 * volumeScale
                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            val angle = 2.0 * Math.PI * frequencyHz * t

                            // Synthesizer with rich harmonics for punch on small phone speakers
                            var wave = Math.sin(angle)
                            wave += 0.25 * Math.sin(2.0 * angle)
                            wave += 0.15 * Math.sin(3.0 * angle)

                            // Minimize pop artifacts by utilizing dynamic fade-in/fade-out
                            val fadeRange = (sampleRate * 0.015).coerceAtMost(numSamples / 6.0)
                            val envelope = when {
                                i < fadeRange -> i / fadeRange
                                i > numSamples - fadeRange -> (numSamples - i) / fadeRange
                                else -> 1.0
                            }

                            val sampleVal = (wave * maxAmp * envelope).toInt().coerceIn(-32768, 32767)
                            writeShortLe(dos, sampleVal)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return file
    }

    /**
     * Synthesizes and writes two clicks with a gap in intermediate space to form double-beep cue.
     */
    private fun generateDoubleBeepWav(context: Context, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        try {
            val sampleRate = 44100
            val duration1 = 110
            val silence = 160
            val duration2 = 110
            val totalMs = duration1 + silence + duration2
            val numSamples = (sampleRate * (totalMs / 1000.0)).toInt()
            val rawDataSize = numSamples * 2

            val samples1 = (sampleRate * (duration1 / 1000.0)).toInt()
            val samplesSilence = (sampleRate * (silence / 1000.0)).toInt()

            FileOutputStream(file).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    DataOutputStream(bos).use { dos ->
                        dos.writeBytes("RIFF")
                        writeIntLe(dos, 36 + rawDataSize)
                        dos.writeBytes("WAVE")

                        dos.writeBytes("fmt ")
                        writeIntLe(dos, 16)
                        writeShortLe(dos, 1)
                        writeShortLe(dos, 1)
                        writeIntLe(dos, sampleRate)
                        writeIntLe(dos, sampleRate * 2)
                        writeShortLe(dos, 2)
                        writeShortLe(dos, 16)

                        dos.writeBytes("data")
                        writeIntLe(dos, rawDataSize)

                        val maxAmp = 30500.0
                        val frequencyHz = 880.0

                        for (i in 0 until numSamples) {
                            val sampleVal: Int = when {
                                i < samples1 -> {
                                    val t = i.toDouble() / sampleRate
                                    val angle = 2.0 * Math.PI * frequencyHz * t
                                    var wave = Math.sin(angle) + 0.2 * Math.sin(2.0 * angle)
                                    val fadeRange = (sampleRate * 0.015).coerceAtMost(samples1 / 6.0)
                                    val envelope = when {
                                        i < fadeRange -> i / fadeRange
                                        i > samples1 - fadeRange -> (samples1 - i) / fadeRange
                                        else -> 1.0
                                    }
                                    (wave * maxAmp * envelope).toInt().coerceIn(-32768, 32767)
                                }
                                i < samples1 + samplesSilence -> {
                                    0
                                }
                                else -> {
                                    val localI = i - (samples1 + samplesSilence)
                                    val t = localI.toDouble() / sampleRate
                                    val angle = 2.0 * Math.PI * frequencyHz * t
                                    var wave = Math.sin(angle) + 0.2 * Math.sin(2.0 * angle)
                                    val samples2 = numSamples - (samples1 + samplesSilence)
                                    val fadeRange = (sampleRate * 0.015).coerceAtMost(samples2 / 6.0)
                                    val envelope = when {
                                        localI < fadeRange -> localI / fadeRange
                                        localI > samples2 - fadeRange -> (samples2 - localI) / fadeRange
                                        else -> 1.0
                                    }
                                    (wave * maxAmp * envelope).toInt().coerceIn(-32768, 32767)
                                }
                            }
                            writeShortLe(dos, sampleVal)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return file
    }

    private fun playSound(soundId: Int) {
        if (soundId != -1 && loadedSoundIds.contains(soundId)) {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    fun playTick() {
        try {
            playSound(tickSoundId)
            vibrate(50)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playStrongBeep() {
        try {
            playSound(strongBeepSoundId)
            vibrate(150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playDoubleBeep() {
        try {
            playSound(doubleBeepSoundId)
            vibrate(250)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSetFinished() {
        try {
            // zero-latency, highly stable sound playback
            if (applauseSoundId != -1 && loadedSoundIds.contains(applauseSoundId)) {
                soundPool.play(applauseSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
            } else {
                // simple feedback if loaded incorrectly
                playSound(strongBeepSoundId)
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
            soundPool.release()
            executor.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
