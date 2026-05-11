package com.prateekj.snooper.infra

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.prateekj.snooper.utils.Logger
import java.util.concurrent.CopyOnWriteArrayList

class CurrentActivityManager private constructor(
    application: Application
) : ActivityLifecycleCallbacks {

    private val listeners =
        CopyOnWriteArrayList<Listener>()

    @Volatile
    private var currentActivity:
            Activity? = null

    interface Listener {

        fun currentActivity(
            activity: Activity?
        )
    }

    init {

        application.registerActivityLifecycleCallbacks(
            this
        )
    }

    fun registerListener(
        listener: Listener
    ) {

        if (
            !listeners.contains(listener)
        ) {

            listeners.add(listener)
        }

        listener.currentActivity(
            currentActivity
        )
    }

    fun unregisterListener(
        listener: Listener
    ) {

        listeners.remove(listener)
    }

    override fun onActivityResumed(
        activity: Activity
    ) {

        currentActivity = activity

        notifyListeners(activity)
    }

    override fun onActivityPaused(
        activity: Activity
    ) {

        if (currentActivity === activity) {

            currentActivity = null

            notifyListeners(null)
        }
    }

    private fun notifyListeners(
        activity: Activity?
    ) {

        for (listener in listeners) {

            try {

                listener.currentActivity(
                    activity
                )

            } catch (e: Exception) {

                Logger.e(
                    TAG,
                    "Listener error",
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

        if (currentActivity === activity) {

            currentActivity = null
        }
    }

    companion object {

        private val TAG =
            CurrentActivityManager::class.java.simpleName

        @Volatile
        private var INSTANCE:
                CurrentActivityManager? = null

        fun getInstance(
            application: Application
        ): CurrentActivityManager {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: CurrentActivityManager(
                    application
                ).also {

                    INSTANCE = it
                }
            }
        }
    }
}
