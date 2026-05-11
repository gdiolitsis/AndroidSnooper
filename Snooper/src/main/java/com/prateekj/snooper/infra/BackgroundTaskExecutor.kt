package com.prateekj.snooper.infra

import android.app.Activity
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

class BackgroundTaskExecutor(
    activity: Activity
) {

    private val activityRef =
        WeakReference(activity)

    private val executor =
        Executors.newCachedThreadPool()

    fun <E> execute(
        backgroundTask: BackgroundTask<E>
    ) {

        executor.execute {

            val result =
                backgroundTask.onExecute()

            sendResult(
                result,
                backgroundTask
            )
        }
    }

    private fun <E> sendResult(
        result: E,
        backgroundTask: BackgroundTask<E>
    ) {

        val activity =
            activityRef.get()
                ?: return

        if (
            activity.isFinishing ||
            activity.isDestroyed
        ) {

            return
        }

        activity.runOnUiThread {

            backgroundTask.onResult(result)
        }
    }
}
