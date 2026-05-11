package com.prateekj.snooper.networksnooper.model

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

class HttpCallRecordTest {

    private lateinit var httpCallRecord:
            HttpCallRecord

    @Before
    fun setUp() {

        val url =
            "https://ajax.googleapis.com/ajax/services/search/web?v=1.0"

        val responseBody =
            "responseBody"

        val requestBody =
            "requestBody"

        httpCallRecord =
            HttpCallRecord.from(
                HttpCall.Builder()
                    .withUrl(url)
                    .withMethod("POST")
                    .withPayload(requestBody)
                    .withResponseBody(responseBody)
                    .withStatusCode(200)
                    .withStatusText("OK")
                    .withRequestHeaders(
                        getRequestHeaders()
                    )
                    .withResponseHeaders(
                        getResponseHeaders()
                    )
                    .build()
            )

        assertNotNull(
            httpCallRecord.date
        )
    }

    @Test
    fun shouldReturnRequestHeaderByGivenName() {

        val requestHeader =
            httpCallRecord.getRequestHeader(
                "User-Agent"
            )

        assertNotNull(
            requestHeader
        )

        assertThat(
            requestHeader!!
                .values[0]
                .value,
            `is`("Android Browser")
        )
    }

    @Test
    fun shouldReturnRequestHeaderByGivenNameByIgnoringCase() {

        val requestHeader =
            httpCallRecord.getRequestHeader(
                "USER-AGENT"
            )

        assertNotNull(
            requestHeader
        )

        assertThat(
            requestHeader!!
                .values[0]
                .value,
            `is`("Android Browser")
        )
    }

    @Test
    fun shouldReturnNullWhenHeaderByGivenNameNotFound() {

        val requestHeader =
            httpCallRecord.getRequestHeader(
                "Invalid Name"
            )

        assertNull(
            requestHeader
        )
    }

    @Test
    fun shouldReturnResponseHeaderByGivenName() {

        val responseHeader =
            httpCallRecord.getResponseHeader(
                "date"
            )

        assertNotNull(
            responseHeader
        )

        assertThat(
            responseHeader!!
                .values[0]
                .value,
            `is`(
                "Thu, 02 Mar 2017 13:03:11 GMT"
            )
        )
    }

    @Test
    fun shouldReturnResponseHeaderByGivenNameByIgnoringCase() {

        val responseHeader =
            httpCallRecord.getResponseHeader(
                "DATE"
            )

        assertNotNull(
            responseHeader
        )

        assertThat(
            responseHeader!!
                .values[0]
                .value,
            `is`(
                "Thu, 02 Mar 2017 13:03:11 GMT"
            )
        )
    }

    @Test
    fun shouldReturnNullWhenResponseHeaderByGivenNameNotFound() {

        val responseHeader =
            httpCallRecord.getResponseHeader(
                "Invalid Name"
            )

        assertNull(
            responseHeader
        )
    }

    @Test
    fun shouldReturnFalseWhenErrorIsNull() {

        httpCallRecord.error =
            null

        assertThat(
            httpCallRecord.hasError(),
            `is`(false)
        )
    }

    @Test
    fun shouldReturnTrueWhenErrorExists() {

        httpCallRecord.error =
            "Network Error"

        assertThat(
            httpCallRecord.hasError(),
            `is`(true)
        )
    }

    private fun getResponseHeaders():
            Map<String, List<String>> {

        val xssProtectionHeader =
            listOf(
                "1",
                "mode=block"
            )

        val dateHeader =
            listOf(
                "Thu, 02 Mar 2017 13:03:11 GMT"
            )

        return mapOf(
            "x-xss-protection" to
                xssProtectionHeader,

            "date" to
                dateHeader
        )
    }

    private fun getRequestHeaders():
            Map<String, List<String>> {

        val cacheControlHeader =
            listOf(
                "public",
                "max-age=86400",
                "no-transform"
            )

        val userAgentHeader =
            listOf(
                "Android Browser"
            )

        return mapOf(
            "User-Agent" to
                userAgentHeader,

            "cache-control" to
                cacheControlHeader
        )
    }
}
