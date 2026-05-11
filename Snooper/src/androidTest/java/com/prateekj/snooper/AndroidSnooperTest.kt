package com.prateekj.snooper

import androidx.test.platform.app.InstrumentationRegistry
import com.prateekj.snooper.networksnooper.database.SnooperRepo
import com.prateekj.snooper.networksnooper.model.HttpCall.Builder
import com.prateekj.snooper.rules.DataResetRule
import com.prateekj.snooper.utils.EspressoUtil.waitFor
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.sameInstance
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AndroidSnooperTest {

    private lateinit var androidSnooper: AndroidSnooper

    @get:Rule
    var dataResetRule =
        DataResetRule()

    @Before
    @Throws(Exception::class)
    fun setUp() {

        val instrumentation =
            InstrumentationRegistry.getInstrumentation()

        val application =
            instrumentation.targetContext.applicationContext
                as android.app.Application

        androidSnooper =
            AndroidSnooper.init(application)
    }

    @Test
    @Throws(Exception::class)
    fun shouldReturnSameInstanceOnEveryInit() {

        val instrumentation =
            InstrumentationRegistry.getInstrumentation()

        val application =
            instrumentation.targetContext.applicationContext
                as android.app.Application

        val newSnooper =
            AndroidSnooper.init(application)

        assertThat(
            newSnooper,
            sameInstance(androidSnooper)
        )
    }

    @Test
    @Throws(Exception::class)
    fun shouldSaveHttpCallViaSpringHttpRequestInterceptor() {

        val url =
            "https://ajax.googleapis.com/ajax/services/search/web?v=1.0"

        val responseBody =
            "responseBody"

        val requestBody =
            "requestBody"

        val instrumentation =
            InstrumentationRegistry.getInstrumentation()

        val snooperRepo =
            SnooperRepo(
                instrumentation.targetContext
            )

        val call = Builder()

            .withUrl(url)

            .withMethod("POST")

            .withPayload(requestBody)

            .withResponseBody(responseBody)

            .withStatusCode(200)

            .withStatusText("OK")

            .build()

        androidSnooper.record(call)

        waitFor {
            snooperRepo
                .findAllSortByDate()
                .isNotEmpty()
        }

        instrumentation.runOnMainSync {

            val httpCallRecords =
                snooperRepo.findAllSortByDate()

            assertThat(
                httpCallRecords.size,
                `is`(1)
            )

            val httpCallRecord =
                httpCallRecords[0]

            assertThat(
                httpCallRecord.url,
                `is`(url)
            )

            assertThat(
                httpCallRecord.payload,
                `is`(requestBody)
            )

            assertThat(
                httpCallRecord.method,
                `is`("POST")
            )

            assertThat(
                httpCallRecord.responseBody,
                `is`(responseBody)
            )

            assertThat(
                httpCallRecord.statusCode,
                `is`(200)
            )

            assertThat(
                httpCallRecord.statusText,
                `is`("OK")
            )
        }
    }
}
