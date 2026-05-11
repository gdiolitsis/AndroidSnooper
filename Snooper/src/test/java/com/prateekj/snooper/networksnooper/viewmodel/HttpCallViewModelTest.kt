package com.prateekj.snooper.networksnooper.viewmodel

import android.view.View.GONE
import android.view.View.VISIBLE
import com.prateekj.snooper.R
import com.prateekj.snooper.networksnooper.model.HttpCall
import com.prateekj.snooper.networksnooper.model.HttpCall.Builder
import com.prateekj.snooper.networksnooper.model.HttpCallRecord.Companion.from
import com.prateekj.snooper.utils.TestUtils
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Arrays
import java.util.HashMap

class HttpCallViewModelTest {

    private lateinit var httpCall:
            HttpCall

    private lateinit var httpCallViewModel:
            HttpCallViewModel

    @Before
    fun setUp() {

        val url =
            "https://ajax.googleapis.com/ajax/services/search/web?v=1.0"

        httpCall =
            Builder()
                .withUrl(url)
                .withMethod("POST")
                .withStatusCode(200)
                .withStatusText("OK")
                .withResponseHeaders(
                    singleHeader()
                )
                .withRequestHeaders(
                    singleHeader()
                )
                .build()

        httpCall.date =
            TestUtils.getDate(
                2017,
                5,
                2,
                11,
                22,
                33
            )

        httpCallViewModel =
            HttpCallViewModel(
                from(httpCall)
            )
    }

    @Test
    fun getUrl() {

        assertTrue(
            httpCallViewModel.url ==
                    httpCall.url
        )
    }

    @Test
    fun getMethod() {

        assertTrue(
            httpCallViewModel.method ==
                    httpCall.method
        )
    }

    @Test
    fun getStatusCode() {

        assertThat(
            httpCallViewModel.statusCode,
            `is`("200")
        )
    }

    @Test
    fun getStatusText() {

        assertTrue(
            httpCallViewModel.statusText ==
                    httpCall.statusText
        )
    }

    @Test
    fun getTimeStamp() {

        assertTrue(
            httpCallViewModel.timeStamp ==
                    "06/02/2017 11:22:33"
        )
    }

    @Test
    fun getRequestHeaders() {

        assertThat(
            httpCallViewModel
                .requestHeaders[0]
                .name,
            `is`("header1")
        )
    }

    @Test
    fun getResponseHeaders() {

        assertThat(
            httpCallViewModel
                .responseHeaders[0]
                .name,
            `is`("header1")
        )
    }

    @Test
    fun shouldGetColorGreenWhenStatusCode2xx() {

        assertEquals(
            R.color.snooper_green.toLong(),
            httpCallViewModel
                .getStatusColor()
                .toLong()
        )
    }

    @Test
    fun shouldGetResponseHeaderVisibilityAsGone() {

        val viewModel =
            HttpCallViewModel(
                from(
                    Builder().build()
                )
            )

        assertThat(
            viewModel.responseHeaderVisibility,
            `is`(GONE)
        )
    }

    @Test
    fun shouldGetRequestHeaderVisibilityAsGone() {

        val viewModel =
            HttpCallViewModel(
                from(
                    Builder().build()
                )
            )

        assertThat(
            viewModel.requestHeaderVisibility,
            `is`(GONE)
        )
    }

    @Test
    fun shouldGetColorYellowWhenStatusCode3xx() {

        val httpCall =
            Builder()
                .withUrl("url 1")
                .withMethod("POST")
                .withStatusCode(302)
                .withStatusText("FAIL")
                .build()

        val viewModel =
            HttpCallViewModel(
                from(httpCall)
            )

        assertEquals(
            R.color.snooper_yellow.toLong(),
            viewModel
                .getStatusColor()
                .toLong()
        )
    }

    @Test
    fun shouldGetColorRedWhenStatusCode4xx() {

        val httpCall =
            Builder()
                .withUrl("url 1")
                .withMethod("POST")
                .withStatusCode(400)
                .withStatusText("FAIL")
                .build()

        val viewModel =
            HttpCallViewModel(
                from(httpCall)
            )

        assertEquals(
            R.color.snooper_red.toLong(),
            viewModel
                .getStatusColor()
                .toLong()
        )
    }

    @Test
    fun shouldReturnResponseInfoContainerVisibilityAsVisible() {

        assertThat(
            httpCallViewModel.responseInfoVisibility,
            `is`(VISIBLE)
        )
    }

    @Test
    fun shouldReturnResponseInfoContainerVisibilityAsGone() {

        val httpCall =
            Builder()
                .withError("error")
                .build()

        val viewModel =
            HttpCallViewModel(
                from(httpCall)
            )

        assertThat(
            viewModel.responseInfoVisibility,
            `is`(GONE)
        )
    }

    @Test
    fun shouldReturnFailedTextVisibilityAsGone() {

        assertThat(
            httpCallViewModel.failedTextVisibility,
            `is`(GONE)
        )
    }

    @Test
    fun shouldReturnFailedTextVisibilityAsVisible() {

        val httpCall =
            Builder()
                .withError("error")
                .build()

        val viewModel =
            HttpCallViewModel(
                from(httpCall)
            )

        assertThat(
            viewModel.failedTextVisibility,
            `is`(VISIBLE)
        )
    }

    @Test
    fun shouldGetResponseHeaderVisibilityAsVisible() {

        assertThat(
            httpCallViewModel.responseHeaderVisibility,
            `is`(VISIBLE)
        )
    }

    @Test
    fun shouldGetRequestHeaderVisibilityAsVisible() {

        assertThat(
            httpCallViewModel.requestHeaderVisibility,
            `is`(VISIBLE)
        )
    }

    private fun singleHeader():
            HashMap<String, List<String>> {

        return object :
            HashMap<String, List<String>>() {

            init {

                put(
                    "header1",
                    Arrays.asList(
                        "headerValue"
                    )
                )
            }
        }
    }
}
