package com.prateekj.snooper.utils

import androidx.test.platform.app.InstrumentationRegistry
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TestUtilities {

    @Throws(IOException::class)
    fun readFileAsStream(
        assetFileName: String
    ): InputStream {

        return InstrumentationRegistry
            .getInstrumentation()
            .context
            .assets
            .open(assetFileName)
    }

    @Throws(IOException::class)
    fun readFrom(
        assetFileName: String
    ): String {

        val inputStream =
            readFileAsStream(assetFileName)

        val bufferedReader =
            BufferedReader(
                InputStreamReader(inputStream)
            )

        return bufferedReader.use {
            it.readText()
        }
    }

    fun getDate(
        year: Int,
        month: Int,
        day: Int
    ): Date {

        val instance =
            Calendar.getInstance(
                Locale.US
            )

        instance.set(
            year,
            month,
            day,
            0,
            0,
            0
        )

        instance.set(
            Calendar.MILLISECOND,
            0
        )

        return instance.time
    }

    fun getDate(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        seconds: Int
    ): Date {

        val instance =
            Calendar.getInstance(
                Locale.US
            )

        instance.set(
            year,
            month,
            day,
            hour,
            minute,
            seconds
        )

        instance.set(
            Calendar.MILLISECOND,
            0
        )

        return instance.time
    }

    fun getCalendar(
        date: Date
    ): Calendar {

        return Calendar.getInstance(
            Locale.US
        ).apply {

            time = date
        }
    }
}
