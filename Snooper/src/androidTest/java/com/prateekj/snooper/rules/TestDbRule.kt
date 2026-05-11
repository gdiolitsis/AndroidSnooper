package com.prateekj.snooper.rules

import android.content.ContextWrapper
import androidx.annotation.RawRes
import androidx.test.platform.app.InstrumentationRegistry
import com.prateekj.snooper.utils.Logger
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TestDbRule(
    @param:RawRes
    private val dbRawResourceId: Int,
    private val dbName: String
) : TestRule {

    val dbDirectory: String
        get() {

            val context =
                InstrumentationRegistry
                    .getInstrumentation()
                    .targetContext

            val cw =
                ContextWrapper(context)

            val destPath =
                cw.filesDir.path

            return destPath.substring(
                0,
                destPath.lastIndexOf("/")
            ) + "/databases"
        }

    override fun apply(
        base: Statement,
        description: Description
    ): Statement {

        return object : Statement() {

            @Throws(Throwable::class)
            override fun evaluate() {

                try {

                    copyDataBase(dbName)

                    base.evaluate()

                } finally {

                    InstrumentationRegistry
                        .getInstrumentation()
                        .targetContext
                        .deleteDatabase(dbName)
                }
            }
        }
    }

    private fun copyDataBase(
        finalDbName: String
    ) {

        val applicationContext =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        val dbDir =
            File(dbDirectory)

        if (!dbDir.exists()) {

            dbDir.mkdirs()
        }

        Logger.d(
            TAG,
            "New database is being copied to device!"
        )

        try {

            val inputStream =
                applicationContext
                    .resources
                    .openRawResource(
                        dbRawResourceId
                    )

            val outputFile =
                File(
                    dbDir,
                    finalDbName
                )

            if (outputFile.exists()) {

                outputFile.delete()
            }

            val outputStream =
                FileOutputStream(outputFile)

            inputStream.use { input ->

                outputStream.use { output ->

                    input.copyTo(output)

                    output.flush()
                }
            }

            Logger.d(
                TAG,
                "New database has been copied to device!"
            )

        } catch (e: IOException) {

            Logger.e(
                TAG,
                e.message,
                e
            )

            throw RuntimeException(
                "Failed to copy test database",
                e
            )
        }
    }

    companion object {

        private const val TAG =
            "TestDbRule"
    }
}
