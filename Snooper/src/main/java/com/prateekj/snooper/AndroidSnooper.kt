package com.prateekj.snooper

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.prateekj.snooper.infra.BackgroundManager
import com.prateekj.snooper.infra.CurrentActivityManager
import com.prateekj.snooper.networksnooper.activity.HttpCallListActivity
import com.prateekj.snooper.networksnooper.database.SnooperRepo
import com.prateekj.snooper.networksnooper.model.HttpCall
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import java.io.IOException

class AndroidSnooper private constructor() :
    BackgroundManager.Listener,
    SnooperShakeAction,
    CurrentActivityManager.Listener {

    private lateinit var context: Context

    private lateinit var shakeDetector: ShakeDetector

    @Volatile
    private var currentActivity: Activity? = null

    private lateinit var snooperRepo: SnooperRepo

    private lateinit var writeThread: HandlerThread

    private lateinit var writeHandler: Handler

    @Throws(IOException::class)
    fun record(
        httpCall: HttpCall
    ) {

        if (!::writeHandler.isInitialized) {
            return
        }

        writeHandler.post {

            try {

                snooperRepo.save(
                    HttpCallRecord.from(httpCall)
                )

            } catch (t: Throwable) {

                t.printStackTrace()
            }
        }
    }

    override fun onBecameForeground() {

        registerSensorListener()
    }

    override fun onBecameBackground() {

        unRegisterSensorListener()
    }

    override fun currentActivity(
        activity: Activity?
    ) {

        currentActivity = activity
    }

    override fun startSnooperFlow() {

        val activity =
            currentActivity ?: return

        val intent =
            Intent(
                context,
                HttpCallListActivity::class.java
            ).apply {

                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

        activity.startActivity(intent)
    }

    override fun endSnooperFlow() {

        val intent =
            Intent(
                ACTION_END_SNOOPER_FLOW
            )

        LocalBroadcastManager
            .getInstance(context)
            .sendBroadcast(intent)
    }

    private fun registerSensorListener() {

        val sensorManager =
            context.getSystemService(
                Context.SENSOR_SERVICE
            ) as? SensorManager
                ?: return

        val sensor =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            ) ?: return

        sensorManager.registerListener(
            shakeDetector,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    private fun unRegisterSensorListener() {

        val sensorManager =
            context.getSystemService(
                Context.SENSOR_SERVICE
            ) as? SensorManager
                ?: return

        sensorManager.unregisterListener(
            shakeDetector
        )
    }

    private fun release() {

        try {

            unRegisterSensorListener()

        } catch (_: Throwable) {
        }

        try {

            if (::writeThread.isInitialized) {

                writeThread.quitSafely()
            }

        } catch (_: Throwable) {
        }
    }

    companion object {

        const val ACTION_END_SNOOPER_FLOW =
            "com.snooper.END_SNOOPER_FLOW"

        @Volatile
        private var INSTANCE:
                AndroidSnooper? = null

        @JvmStatic
        fun init(
            application: Application
        ): AndroidSnooper {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: buildAndroidSnooper(
                    application
                ).also {

                    INSTANCE = it
                }
            }
        }

        private fun buildAndroidSnooper(
            application: Application
        ): AndroidSnooper {

            val androidSnooper =
                AndroidSnooper()

            androidSnooper.context =
                application.applicationContext

            androidSnooper.snooperRepo =
                SnooperRepo(
                    androidSnooper.context
                )

            androidSnooper.shakeDetector =
                ShakeDetector(
                    SnooperShakeListener(
                        androidSnooper
                    )
                )

            androidSnooper.writeThread =
                HandlerThread(
                    "AndroidSnooper:Writer"
                )

            androidSnooper.writeThread.start()

            androidSnooper.writeHandler =
                Handler(
                    androidSnooper
                        .writeThread
                        .looper
                )

            BackgroundManager
                .getInstance(application)
                .registerListener(androidSnooper)

            CurrentActivityManager
                .getInstance(application)
                .registerListener(androidSnooper)

            return androidSnooper
        }

        @JvmStatic
        val instance: AndroidSnooper

            get() {

                return INSTANCE
                    ?: throw RuntimeException(
                        "Android Snooper is not initialized yet"
                    )
            }
    }
}
