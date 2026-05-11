package com.prateekj.snooper.infra

import android.app.Activity
import android.os.Build
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BackgroundTaskExecutor(
    activity: Activity
) {

    private val activityRef =
        WeakReference(activity)

    private val executor:
            ExecutorService =
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

        val isDestroyed =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.JELLY_BEAN_MR1
            ) {

                activity.isDestroyed

            } else {

                false
            }

        if (
            activity.isFinishing ||
            isDestroyed
        ) {

            return
        }

        activity.runOnUiThread {

            backgroundTask.onResult(
                result
            )
        }
    }
}
