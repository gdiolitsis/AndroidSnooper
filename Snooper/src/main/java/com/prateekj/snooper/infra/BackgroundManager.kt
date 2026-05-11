package com.prateekj.snooper.infra

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.prateekj.snooper.utils.Logger
import java.util.concurrent.CopyOnWriteArrayList

class BackgroundManager private constructor(
    application: Application
) : ActivityLifecycleCallbacks {

    private var isInBackground =
        true

    private val listeners =
        CopyOnWriteArrayList<Listener>()

    private val backgroundDelayHandler =
        Handler(Looper.getMainLooper())

    private var backgroundTransition:
            Runnable? = null

    interface Listener {

        fun onBecameForeground()

        fun onBecameBackground()
    }

    init {

        application.registerActivityLifecycleCallbacks(
            this
        )
    }

    fun registerListener(
        listener: Listener
    ) {

        if (!listeners.contains(listener)) {

            listeners.add(listener)
        }
    }

    fun unregisterListener(
        listener: Listener
    ) {

        listeners.remove(listener)
    }

    override fun onActivityResumed(
        activity: Activity
    ) {

        backgroundTransition?.let {

            backgroundDelayHandler.removeCallbacks(it)

            backgroundTransition = null
        }

        if (isInBackground) {

            isInBackground = false

            notifyOnBecameForeground()

            Logger.d(
                TAG,
                "Application went to foreground"
            )
        }
    }

    private fun notifyOnBecameForeground() {

        for (listener in listeners) {

            try {

                listener.onBecameForeground()

            } catch (e: Exception) {

                Logger.e(
                    TAG,
                    "Listener threw exception!",
                    e
                )
            }
        }
    }

    override fun onActivityPaused(
        activity: Activity
    ) {

        if (
            !isInBackground &&
            backgroundTransition == null
        ) {

            backgroundTransition =
                Runnable {

                    isInBackground = true

                    backgroundTransition = null

                    notifyOnBecameBackground()

                    Logger.d(
                        TAG,
                        "Application went to background"
                    )
                }

            backgroundDelayHandler.postDelayed(
                backgroundTransition!!,
                BACKGROUND_DELAY
            )
        }
    }

    private fun notifyOnBecameBackground() {

        for (listener in listeners) {

            try {

                listener.onBecameBackground()

            } catch (e: Exception) {

                Logger.e(
                    TAG,
                    "Listener threw exception!",
                    e
                )
            }
        }
    }

    override fun onActivityStopped(
        activity: Activity
    ) {
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {
    }

    override fun onActivityStarted(
        activity: Activity
    ) {
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) {
    }

    override fun onActivityDestroyed(
        activity: Activity
    ) {
    }

    companion object {

        private const val BACKGROUND_DELAY =
            500L

        private val TAG =
            BackgroundManager::class.java.simpleName

        @Volatile
        private var INSTANCE:
                BackgroundManager? = null

        fun getInstance(
            application: Application
        ): BackgroundManager {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: BackgroundManager(
                    application
                ).also {

                    INSTANCE = it
                }
            }
        }
    }
}
