package com.prateekj.snooper

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    private val onShakeListener: OnShakeListener
) : SensorEventListener {

    private var shakeTimestamp: Long = 0L

    override fun onSensorChanged(
        event: SensorEvent?
    ) {

        event ?: return

        if (event.values.size < 3) {
            return
        }

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX =
            x / SensorManager.GRAVITY_EARTH

        val gY =
            y / SensorManager.GRAVITY_EARTH

        val gZ =
            z / SensorManager.GRAVITY_EARTH

        val gForce =
            sqrt(
                (
                    gX * gX +
                    gY * gY +
                    gZ * gZ
                ).toDouble()
            ).toFloat()

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {

            val now =
                System.currentTimeMillis()

            if (shakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                return
            }

            shakeTimestamp = now

            onShakeListener.onShake()
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
        // no-op
    }

    companion object {

        private const val SHAKE_THRESHOLD_GRAVITY =
            2.7f

        private const val SHAKE_SLOP_TIME_MS =
            500L
    }
}
