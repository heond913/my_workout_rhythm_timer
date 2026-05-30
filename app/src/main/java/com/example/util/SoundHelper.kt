package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Vibrator
import com.example.R
import java.util.concurrent.Executors

class SoundHelper(context: Context) {
    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val executor = Executors.newSingleThreadExecutor()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()

    private var applauseSoundId: Int = -1
    private var isApplauseLoaded: Boolean = false

    init {
        try {
            soundPool.setOnLoadCompleteListener { _, sampleId, status ->
                if (sampleId == applauseSoundId && status == 0) {
                    isApplauseLoaded = true
                }
            }

            var fileIsValid = false
            try {
                context.resources.openRawResourceFd(R.raw.applause_cheer).use { fd ->
                    if (fd.length > 500) { // Highly reliable physical file length check inside APK
                        fileIsValid = true
                    }
                }
            } catch (e: Exception) {
                // If openRawResourceFd is not supported or fails, check stream directly
                try {
                    context.resources.openRawResource(R.raw.applause_cheer).use { stream ->
                        if (stream.available() > 200) {
                            fileIsValid = true
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            if (fileIsValid) {
                applauseSoundId = soundPool.load(context.applicationContext, R.raw.applause_cheer, 1)
            } else {
                applauseSoundId = -1
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playTone(frequencyHz: Double, durationMs: Int, volumeScale: Float = 1.0f) {
        executor.submit {
            try {
                val sampleRate = 44100
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val sample = ShortArray(numSamples)
                
                // Add rich harmonics to create a highly hearable, pleasant synth wave
                // standard phone speakers respond much better when sound energy is distributed over harmonics
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val angle = 2.0 * Math.PI * frequencyHz * t
                    
                    var wave = Math.sin(angle)
                    wave += 0.25 * Math.sin(2.0 * angle) // second harmonic for warmth
                    wave += 0.15 * Math.sin(3.0 * angle) // third harmonic for punch
                    
                    // Apply brief fade-in and fade-out to prevent speaker click/pop artifacts
                    val fadeRange = (sampleRate * 0.015).coerceAtMost(numSamples / 6.0) // ~15ms fade transition
                    val envelope = when {
                        i < fadeRange -> i / fadeRange
                        i > numSamples - fadeRange -> (numSamples - i) / fadeRange
                        else -> 1.0
                    }
                    
                    // High amplitude of 30500.0 (near max PCM range of 32767) scales up default volumes substantially
                    val amp = 30500.0 * volumeScale * envelope
                    sample[i] = (wave * amp).toInt().coerceIn(-32768, 32767).toShort()
                }

                // Explicitly associate to USAGE_MEDIA (music stream volume)
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(sample.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(sample, 0, sample.size)
                audioTrack.play()
                
                // Wait for the duration of tone and cleanly release handle resources
                Thread.sleep(durationMs.toLong() + 30)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playToneDirect(frequencyHz: Double, durationMs: Int, volumeScale: Float = 1.0f) {
        try {
            val sampleRate = 44100
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val sample = ShortArray(numSamples)
            
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val angle = 2.0 * Math.PI * frequencyHz * t
                
                var wave = Math.sin(angle)
                wave += 0.25 * Math.sin(2.0 * angle)
                wave += 0.15 * Math.sin(3.0 * angle)
                
                val fadeRange = (sampleRate * 0.015).coerceAtMost(numSamples / 6.0)
                val envelope = when {
                    i < fadeRange -> i / fadeRange
                    i > numSamples - fadeRange -> (numSamples - i) / fadeRange
                    else -> 1.0
                }
                
                val amp = 30500.0 * volumeScale * envelope
                sample[i] = (wave * amp).toInt().coerceIn(-32768, 32767).toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(sample.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(sample, 0, sample.size)
            audioTrack.play()
            
            Thread.sleep(durationMs.toLong() + 30)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playTick() {
        try {
            // A clear, high-pitched, loud rhythm cue sound
            playTone(1050.0, 110, 1.0f)
            vibrate(50)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun playStrongBeep() {
        try {
            // An exceptionally robust initial launch chime
            playTone(850.0, 260, 1.0f)
            vibrate(150)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun playSetFinished() {
        try {
            // Celebratory vibration feedback pattern to accompany the crowd cheer
            executor.submit {
                try {
                    vibrate(150)
                    Thread.sleep(250)
                    vibrate(150)
                    Thread.sleep(250)
                    vibrate(400)
                } catch (e: Exception) {
                    // Safe fallback
                }
            }

            // Zero-latency, resource-efficient playback of the custom hoot/applause cheer sound via SoundPool
            if (applauseSoundId != -1 && isApplauseLoaded) {
                soundPool.play(applauseSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
            } else {
                // FALLBACK: Extremely rich synthesized cheering and crowd applause simulation!
                executor.submit {
                    try {
                        val sampleRate = 44100
                        val durationMs = 3000 // 3 seconds of peak celebration
                        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                        val sample = ShortArray(numSamples)
                        
                        val random = java.util.Random(42) // Seeded for a robust, pleasant applaud character
                        
                        // Create multiple dynamic "clapping" crowd members
                        val numClappers = 18
                        val clapperIntervals = IntArray(numClappers) { i -> 150 + i * 14 } // varying tempo/speeds
                        val clapperPhases = IntArray(numClappers) { i -> random.nextInt(200) }
                        
                        var clapFilterState = 0.0 // Recursive low-pass filter state to smooth and warm up claps

                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            
                            // 1. --- Majestic Major Fanfare (Swells dynamically) ---
                            val fanfareSwell = if (t < 0.5) t / 0.5 else (3.0 - t).coerceIn(0.0, 1.0) / 2.5
                            var fanfareWave = 0.0
                            val chordTones = doubleArrayOf(349.23, 440.00, 523.25, 698.46)
                            for (freq in chordTones) {
                                val angle = 2.0 * Math.PI * freq * t
                                var tone = Math.sin(angle)
                                tone += 0.3 * Math.sin(2.0 * angle)
                                tone += 0.15 * Math.sin(3.0 * angle)
                                fanfareWave += tone
                            }
                            fanfareWave = (fanfareWave / chordTones.size) * fanfareSwell * 0.45
                            
                            // 2. --- Crowd Cheering/Hooting Vocal Effect ("Woohoo!") ---
                            var voiceWave = 0.0
                            val numVoices = 4
                            for (v in 0 until numVoices) {
                                val startDelay = v * 0.12
                                if (t > startDelay) {
                                    val vt = t - startDelay
                                    val voiceLen = 1.8
                                    if (vt < voiceLen) {
                                        val progress = vt / voiceLen
                                        val basePitch = 260.0 + v * 45.0
                                        val swoop = 280.0 * Math.sin(Math.PI * progress)
                                        val currentFreq = basePitch + swoop
                                        
                                        val angle = 2.0 * Math.PI * currentFreq * vt
                                        var voice = Math.sin(angle)
                                        val lfo = 1.0 + 0.25 * Math.sin(2.0 * Math.PI * 7.5 * vt)
                                        voice *= lfo
                                        val envelope = Math.sin(Math.PI * progress)
                                        voiceWave += voice * envelope * 0.18
                                    }
                                }
                            }
                            
                            // 3. --- Warm Crowd Clapping (Decaying palm pops) ---
                            var rawClapSampleList = 0.0
                            val msCurrent = (t * 1000).toInt()
                            for (c in 0 until numClappers) {
                                val interval = clapperIntervals[c]
                                val phase = clapperPhases[c]
                                val timeSinceLastClapMs = (msCurrent + phase) % interval
                                
                                // Each clap is modeled as a fast decaying impulse
                                if (timeSinceLastClapMs < 45) {
                                    val clapSec = timeSinceLastClapMs / 1000.0
                                    val noise = random.nextGaussian()
                                    val clapEnvelope = Math.exp(-115.0 * clapSec)
                                    rawClapSampleList += noise * clapEnvelope * 0.14
                                }
                            }
                            
                            // 1st-Order Recursive IIR Low-pass Filter (fc = ~1kHz at 44.1kHz sampling rate)
                            clapFilterState += 0.13 * (rawClapSampleList - clapFilterState)
                            val warmClapWave = clapFilterState
                            
                            // 4. --- Tape Saturation & Limiting (Zero Distortion Soft-clipper) ---
                            val rawMixedSignal = fanfareWave + voiceWave + warmClapWave
                            val saturatedSignal = Math.tanh(rawMixedSignal)
                            
                            val masterEnvelope = when {
                                t > 2.5 -> (3.0 - t) / 0.5
                                else -> 1.0
                            }.coerceIn(0.0, 1.0)
                            
                            val amp = 29500.0 * masterEnvelope
                            sample[i] = (saturatedSignal * amp).toInt().coerceIn(-32768, 32767).toShort()
                        }
                        
                        val audioTrack = AudioTrack.Builder()
                            .setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            )
                            .setAudioFormat(
                                AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(sampleRate)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build()
                            )
                            .setBufferSizeInBytes(sample.size * 2)
                            .setTransferMode(AudioTrack.MODE_STATIC)
                            .build()
                            
                        audioTrack.write(sample, 0, sample.size)
                        audioTrack.play()
                        
                        Thread.sleep(durationMs.toLong() + 50)
                        audioTrack.stop()
                        audioTrack.release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playDoubleBeep() {
        try {
            executor.submit {
                playToneDirect(880.0, 110, 1.0f)
                Thread.sleep(160)
                playToneDirect(880.0, 110, 1.0f)
            }
            vibrate(250)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(ms: Long) {
        try {
            vibrator?.vibrate(ms)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun release() {
        try {
            soundPool.release()
            executor.shutdown()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
