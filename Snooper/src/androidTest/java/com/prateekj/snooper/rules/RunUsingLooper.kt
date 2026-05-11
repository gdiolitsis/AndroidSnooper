package com.prateekj.snooper.rules

import android.os.Looper
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RunUsingLooper : TestRule {

    override fun apply(
        base: Statement,
        description: Description
    ): Statement {

        return object : Statement() {

            @Throws(Throwable::class)
            override fun evaluate() {

                val hasLooper =
                    Looper.myLooper() != null

                try {

                    if (!hasLooper) {

                        Looper.prepare()
                    }

                    base.evaluate()

                } finally {

                    if (!hasLooper) {

                        Looper.myLooper()
                            ?.quitSafely()
                    }
                }
            }
        }
    }
}
