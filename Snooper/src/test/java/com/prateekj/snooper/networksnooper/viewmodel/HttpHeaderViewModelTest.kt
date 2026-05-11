package com.prateekj.snooper.networksnooper.viewmodel

import com.prateekj.snooper.networksnooper.model.HttpHeader
import com.prateekj.snooper.networksnooper.model.HttpHeaderValue
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

class HttpHeaderViewModelTest {

    private lateinit var httpHeaderViewModel:
            HttpHeaderViewModel

    @Before
    fun setUp() {

        val httpHeader =
            HttpHeader(
                "accept-language"
            )

        val value1 =
            HttpHeaderValue(
                "en-US,en"
            )

        val value2 =
            HttpHeaderValue(
                "q=0.8,hi"
            )

        val value3 =
            HttpHeaderValue(
                "q=0.6"
            )

        httpHeader.values =
            listOf(
                value1,
                value2,
                value3
            )

        httpHeaderViewModel =
            HttpHeaderViewModel(
                httpHeader
            )
    }

    @Test
    fun shouldReturnHeaderName() {

        assertThat(
            httpHeaderViewModel.headerName(),
            `is`(
                "accept-language"
            )
        )
    }

    @Test
    fun shouldReturnHeaderValues() {

        assertThat(
            httpHeaderViewModel.headerValues(),
            `is`(
                "en-US,en;q=0.8,hi;q=0.6"
            )
        )
    }
}
