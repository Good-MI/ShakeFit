package com.example.shakefit.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * FeedbackManager: Mengatur feedback getar dan bunyi sistem (F-12b)
 * menggunakan Vibrator/VibratorManager dan ToneGenerator bawaan Android
 * tanpa file audio eksternal atau internet.
 */
class FeedbackManager(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (_: Exception) {
            // Safety handling jika hardware audio tidak mendukung ToneGenerator
        }
    }

    /** Countdown 5..1: Getar dan bunyi bip singkat */
    fun onCountdownTick(soundOn: Boolean, vibrationOn: Boolean) {
        if (soundOn) playTone(ToneGenerator.TONE_PROP_BEEP, 70)
        if (vibrationOn) vibrate(40L)
    }

    /** Countdown 0 (Mulai): Bip konfirmasi dan getar sedikit lebih panjang */
    fun onCountdownGo(soundOn: Boolean, vibrationOn: Boolean) {
        if (soundOn) playTone(ToneGenerator.TONE_PROP_ACK, 120)
        if (vibrationOn) vibrate(80L)
    }

    /** 1 Rep terdeteksi */
    fun onRepCounted(soundOn: Boolean, vibrationOn: Boolean) {
        if (soundOn) playTone(ToneGenerator.TONE_PROP_BEEP, 60)
        if (vibrationOn) vibrate(50L)
    }

    /** Target tercapai atau sesi selesai: Pola getar ganda dan nada ACK */
    fun onTargetOrFinished(soundOn: Boolean, vibrationOn: Boolean) {
        if (soundOn) playTone(ToneGenerator.TONE_PROP_ACK, 250)
        if (vibrationOn) {
            vibratePattern(longArrayOf(0, 100, 80, 200))
        }
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        try {
            toneGenerator?.startTone(toneType, durationMs)
        } catch (_: Exception) {
            // Ignore error
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(durationMs)
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore error
        }
    }

    private fun vibratePattern(timings: LongArray) {
        try {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createWaveform(timings, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(timings, -1)
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore error
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {
        }
    }
}
