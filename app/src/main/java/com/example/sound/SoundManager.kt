package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var isMuted = false

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        return isMuted
    }

    fun isSoundMuted(): Boolean = isMuted

    fun playSound(event: String) {
        if (isMuted) return
        scope.launch {
            try {
                when (event) {
                    "coin" -> {
                        // High melodic dual chime (e.g. 1046Hz -> 1318Hz)
                        playTone(listOf(1046f, 1318f), durationMs = 65)
                        vibrate(20)
                    }
                    "fuel" -> {
                        // Uplifting triad chime
                        playTone(listOf(523f, 659f, 784f, 1046f), durationMs = 80)
                        vibrate(40)
                    }
                    "flip", "airtime" -> {
                        playTone(listOf(880f, 1174f, 1760f), durationMs = 90)
                        vibrate(60)
                    }
                    "hit" -> {
                        playNoise(durationMs = 90)
                        vibrate(70)
                    }
                    "crash" -> {
                        playNoise(durationMs = 280)
                        vibrate(220)
                    }
                    "win" -> {
                        playTone(listOf(523f, 659f, 784f, 1046f, 1318f), durationMs = 120)
                        vibrate(150)
                    }
                }
            } catch (_: Exception) {
                // Fail gracefully if audio system is occupied
            }
        }
    }

    fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(
                        durationMs.coerceAtLeast(10),
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun playTone(freqs: List<Float>, durationMs: Int) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val generatedSnd = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            var sampleVal = 0.0
            for (f in freqs) {
                sampleVal += sin(2.0 * PI * f * t)
            }
            sampleVal /= freqs.size
            // Fade out envelope to avoid click
            val envelope = (1.0 - (i.toDouble() / numSamples))
            generatedSnd[i] = (sampleVal * 16000 * envelope).toInt().toShort()
        }

        playRawPcm(generatedSnd, sampleRate)
    }

    private fun playNoise(durationMs: Int) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val generatedSnd = ShortArray(numSamples)

        var lastSample = 0.0
        for (i in 0 until numSamples) {
            val white = (Math.random() * 2.0 - 1.0)
            // Low-pass brownian noise for crash impact
            lastSample = (lastSample + (0.15 * white)) / 1.15
            val envelope = (1.0 - (i.toDouble() / numSamples))
            generatedSnd[i] = (lastSample * 22000 * envelope).toInt().toShort()
        }

        playRawPcm(generatedSnd, sampleRate)
    }

    private fun playRawPcm(samples: ShortArray, sampleRate: Int) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        scope.launch {
            kotlinx.coroutines.delay(samples.size * 1000L / sampleRate + 50)
            audioTrack.release()
        }
    }
}
