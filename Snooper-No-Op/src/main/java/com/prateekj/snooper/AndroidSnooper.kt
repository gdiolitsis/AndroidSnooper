package com.prateekj.snooper

import android.app.Application
import com.prateekj.snooper.networksnooper.model.HttpCall
import java.io.IOException

class AndroidSnooper private constructor(
    private val application: Application
) {

    @Throws(IOException::class)
    fun record(httpCall: HttpCall) {

        // future persistence / inspector hook
    }

    companion object {

        @Volatile
        private var INSTANCE: AndroidSnooper? = null

        fun init(
            application: Application
        ): AndroidSnooper {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: AndroidSnooper(
                    application.applicationContext as Application
                ).also {

                    INSTANCE = it
                }
            }
        }

        val instance: AndroidSnooper
            get() {

                return INSTANCE
                    ?: throw IllegalStateException(
                        "AndroidSnooper has not been initialized"
                    )
            }
    }
}
