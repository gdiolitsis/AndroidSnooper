package com.prateekj.snooper.utils

import java.util.concurrent.TimeUnit

object EspressoUtil {

    private const val DEFAULT_WAIT_TIME =
        1500L

    @JvmOverloads
    fun waitFor(
        timeout: Long = DEFAULT_WAIT_TIME,
        condition: () -> Boolean
    ) {

        Logger.d(
            EspressoUtil::class.java.simpleName,
            "Started waiting for condition"
        )

        val endTime =
            System.currentTimeMillis() + timeout

        while (System.currentTimeMillis() < endTime) {

            if (condition()) {

                Logger.d(
                    EspressoUtil::class.java.simpleName,
                    "Condition satisfied"
                )

                return
            }

            sleep()
        }

        val message =
            "Timed out waiting for condition to be satisfied!"

        Logger.e(
            EspressoUtil::class.java.simpleName,
            message
        )

        throw RuntimeException(message)
    }

    private fun sleep() {

        try {

            TimeUnit.MILLISECONDS.sleep(100)

        } catch (e: InterruptedException) {

            Thread.currentThread().interrupt()

            Logger.e(
                EspressoUtil::class.java.simpleName,
                "Sleep interrupted",
                e
            )
        }
    }
}
