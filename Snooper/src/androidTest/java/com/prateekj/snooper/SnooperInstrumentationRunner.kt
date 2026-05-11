package com.prateekj.snooper

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class SnooperInstrumentationRunner :
    AndroidJUnitRunner() {

    lateinit var application:
            Application

    @Throws(
        InstantiationException::class,
        IllegalAccessException::class,
        ClassNotFoundException::class
    )
    override fun newApplication(
        cl: ClassLoader,
        className: String,
        context: Context
    ): Application {

        return super.newApplication(
            cl,
            TestApplication::class.java.name,
            context
        )
    }

    override fun callApplicationOnCreate(
        app: Application
    ) {

        application = app

        AndroidSnooper.init(app)

        super.callApplicationOnCreate(app)
    }
}
