package com.prateekj.snooper.formatter

import com.prateekj.snooper.utils.TestUtilities.readFrom
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test

class PlainTextFormatterTest {

    @Test
    @Throws(Exception::class)
    fun shouldReturnFormattedPlainText() {

        val formatter =
            PlainTextFormatter()

        val rawString =
            readFrom(
                "person_details_formatted_response.txt"
            ).replace(
                "\n",
                "\r"
            )

        val formattedResponse =
            formatter.format(rawString)

        val expectedResponse =
            readFrom(
                "person_details_formatted_response.txt"
            )

        assertThat(
            formattedResponse,
            `is`(expectedResponse)
        )
    }
}
