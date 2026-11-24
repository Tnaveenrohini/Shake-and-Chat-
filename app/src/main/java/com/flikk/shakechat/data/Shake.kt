package com.flikk.shakechat.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.sqrt

/** Phone ni gattiga 2 saarlu oopithe (1 sec lopu) onShake() call avutundi. */
class ShakeDetector(context: Context, private val onShake: () -> Unit) : SensorEventListener {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accel: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastPeak = 0L
    private var peaks = 0
    private var lastShake = 0L

    val available: Boolean get() = accel != null

    fun start() {
        accel?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() = sm.unregisterListener(this)

    override fun onSensorChanged(e: SensorEvent) {
        val gx = e.values[0] / SensorManager.GRAVITY_EARTH
        val gy = e.values[1] / SensorManager.GRAVITY_EARTH
        val gz = e.values[2] / SensorManager.GRAVITY_EARTH
        val g = sqrt(gx * gx + gy * gy + gz * gz)
        if (g < 2.3f) return
        val now = System.currentTimeMillis()
        if (now - lastPeak < 120) return // same shake
        peaks = if (now - lastPeak > 1000) 1 else peaks + 1
        lastPeak = now
        if (peaks >= 2 && now - lastShake > 1500) {
            lastShake = now
            peaks = 0
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

fun vibrate(context: Context, ms: Long = 60) {
    runCatching {
        val v: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

fun vibrateMatch(context: Context) {
    runCatching {
        val v: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 80, 60, 160), -1))
    }
}
