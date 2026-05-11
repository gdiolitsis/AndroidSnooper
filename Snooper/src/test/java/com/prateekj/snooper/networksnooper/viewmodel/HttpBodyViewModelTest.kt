package com.prateekj.snooper.networksnooper.viewmodel

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test

class HttpBodyViewModelTest {

    @Test
    fun shouldReturnFormattedResponseBodyUsingFormatter() {

        val formattedResponseBody =
            "formatted payload"

        val httpBodyViewModel =
            HttpBodyViewModel()

        httpBodyViewModel.init(
            formattedResponseBody
        )

        val actualFormattedPayload =
            httpBodyViewModel.formattedBody

        assertThat(
            actualFormattedPayload,
            `is`(formattedResponseBody)
        )
    }
}
