package com.prateekj.snooper.networksnooper.helper

import android.content.res.Resources
import com.prateekj.snooper.R
import com.prateekj.snooper.formatter.ResponseFormatter
import com.prateekj.snooper.formatter.ResponseFormatterFactory
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.networksnooper.model.HttpHeader
import com.prateekj.snooper.networksnooper.model.HttpHeaderValue
import com.prateekj.snooper.utils.TestUtilities.getDate
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

class DataCopyHelperTest {

    private lateinit var formatterFactory:
            ResponseFormatterFactory

    private lateinit var httpCall:
            HttpCallRecord

    private lateinit var responseFormatter:
            ResponseFormatter

    private lateinit var dataCopyHelper:
            DataCopyHelper

    private lateinit var resources:
            Resources

    private val jsonContentTypeHeader:
            HttpHeader
        get() {

            return HttpHeader(
                name = "Content-Type",
                values = listOf(
                    HttpHeaderValue(
                        "application/json"
                    )
                )
            )
        }

    private val header:
            HttpHeader
        get() {

            return HttpHeader(
                name = "Header",
                values = listOf(
                    HttpHeaderValue(
                        "headerValue"
                    )
                )
            )
        }

    private val acceptLanguageHttpHeader:
            HttpHeader
        get() {

            return HttpHeader(
                name = "accept-language",
                values = listOf(
                    HttpHeaderValue(
                        "en-US,en"
                    ),
                    HttpHeaderValue(
                        "q=0.8,hi"
                    ),
                    HttpHeaderValue(
                        "q=0.6"
                    )
                )
            )
        }

    @Before
    fun setUp() {

        formatterFactory =
            mockk(relaxed = true)

        httpCall =
            mockk(relaxed = true)

        responseFormatter =
            mockk(relaxed = true)

        resources =
            mockk(relaxed = true)

        dataCopyHelper =
            DataCopyHelper(
                httpCall,
                formatterFactory,
                resources
            )

        mockStringResources()
    }

    @Test
    fun shouldCopyResponseWithoutFormattingIfContentHeadersNullInResponseData() {

        val responseBody =
            "response body"

        every {
            httpCall.getResponseHeader(
                "Content-Type"
            )
        } returns null

        every {
            httpCall.responseBody
        } returns responseBody

        val responseDataForCopy =
            dataCopyHelper
                .getResponseDataForCopy()

        assertThat(
            responseDataForCopy,
            `is`(responseBody)
        )

        verify(exactly = 0) {
            responseFormatter.format(
                any()
            )
        }
    }

    @Test
    fun shouldCopyResponseWithoutFormattingIfContentHeaderValuesIsMissingInResponseData() {

        val responseBody =
            "response body"

        val httpHeader =
            HttpHeader(
                values = emptyList()
            )

        every {
            httpCall.getResponseHeader(
                "Content-Type"
            )
        } returns httpHeader

        every {
            httpCall.responseBody
        } returns responseBody

        val responseDataForCopy =
            dataCopyHelper
                .getResponseDataForCopy()

        assertThat(
            responseDataForCopy,
            `is`(responseBody)
        )

        verify(exactly = 0) {
            responseFormatter.format(
                any()
            )
        }
    }

    @Test
    fun shouldCopyResponseWithFormattingIfContentHeadersPresentInResponseData() {

        val responseBody =
            "response body"

        val formattedResponseBody =
            "formatted response body"

        every {
            httpCall.getResponseHeader(
                "Content-Type"
            )
        } returns jsonContentTypeHeader

        every {
            httpCall.responseBody
        } returns responseBody

        every {
            formatterFactory.getFor(
                "application/json"
            )
        } returns responseFormatter

        every {
            responseFormatter.format(
                responseBody
            )
        } returns formattedResponseBody

        val responseDataForCopy =
            dataCopyHelper
                .getResponseDataForCopy()

        assertThat(
            responseDataForCopy,
            `is`(formattedResponseBody)
        )

        verify(exactly = 1) {
            responseFormatter.format(
                responseBody
            )
        }
    }

    @Test
    fun shouldCopyEmptyStringWhenResponseIsNotPresent() {

        every {
            httpCall.responseBody
        } returns null

        val responseDataForCopy =
            dataCopyHelper
                .getResponseDataForCopy()

        assertThat(
            responseDataForCopy,
            `is`("")
        )
    }

    @Test
    fun shouldCopyRequestWithoutFormattingIfContentHeaderIsNullInRequestData() {

        val requestBody =
            "request body"

        every {
            httpCall.getRequestHeader(
                "Content-Type"
            )
        } returns null

        every {
            httpCall.payload
        } returns requestBody

        val requestDataForCopy =
            dataCopyHelper
                .getRequestDataForCopy()

        assertThat(
            requestDataForCopy,
            `is`(requestBody)
        )

        verify(exactly = 0) {
            responseFormatter.format(
                any()
            )
        }
    }

    @Test
    fun shouldCopyRequestWithoutFormattingIfContentHeaderValuesMissingInRequestData() {

        val requestBody =
            "request body"

        val httpHeader =
            HttpHeader(
                values = emptyList()
            )

        every {
            httpCall.getRequestHeader(
                "Content-Type"
            )
        } returns httpHeader

        every {
            httpCall.payload
        } returns requestBody

        val requestDataForCopy =
            dataCopyHelper
                .getRequestDataForCopy()

        assertThat(
            requestDataForCopy,
            `is`(requestBody)
        )

        verify(exactly = 0) {
            responseFormatter.format(
                any()
            )
        }
    }

    @Test
    fun shouldCopyRequestResponseHeadersPresent() {

        val httpHeader =
            acceptLanguageHttpHeader

        every {
            httpCall.requestHeaders
        } returns listOf(
            httpHeader,
            jsonContentTypeHeader
        )

        every {
            httpCall.responseHeaders
        } returns listOf(
            httpHeader,
            header
        )

        val copiedHeaders =
            dataCopyHelper
                .getHeadersForCopy()

        assertThat(
            copiedHeaders,
            `is`(
                "\nRequest Headers\n" +
                    "accept-language: en-US,en;q=0.8,hi;q=0.6\n" +
                    "Content-Type: application/json\n" +
                    "\nResponse Headers\n" +
                    "accept-language: en-US,en;q=0.8,hi;q=0.6\n" +
                    "Header: headerValue\n"
            )
        )
    }

    @Test
    fun shouldCopyOnlyRequestHeadersPresentIfResponseHeadersMissing() {

        val httpHeader =
            acceptLanguageHttpHeader

        every {
            httpCall.requestHeaders
        } returns listOf(
            httpHeader,
            jsonContentTypeHeader
        )

        every {
            httpCall.responseHeaders
        } returns emptyList()

        val copiedHeaders =
            dataCopyHelper
                .getHeadersForCopy()

        assertThat(
            copiedHeaders,
            `is`(
                "\nRequest Headers\n" +
                    "accept-language: en-US,en;q=0.8,hi;q=0.6\n" +
                    "Content-Type: application/json\n"
            )
        )
    }

    @Test
    fun shouldCopyOnlyResponseHeadersPresentIfRequestHeadersMissing() {

        val httpHeader =
            acceptLanguageHttpHeader

        every {
            httpCall.requestHeaders
        } returns emptyList()

        every {
            httpCall.responseHeaders
        } returns listOf(
            httpHeader,
            header
        )

        val copiedHeaders =
            dataCopyHelper
                .getHeadersForCopy()

        assertThat(
            copiedHeaders,
            `is`(
                "\nResponse Headers\n" +
                    "accept-language: en-US,en;q=0.8,hi;q=0.6\n" +
                    "Header: headerValue\n"
            )
        )
    }

    @Test
    fun shouldCopyEmptyStringWhenRequestIsNotPresent() {

        every {
            httpCall.payload
        } returns null

        val requestDataForCopy =
            dataCopyHelper
                .getRequestDataForCopy()

        assertThat(
            requestDataForCopy,
            `is`("")
        )
    }

    @Test
    fun shouldAskViewToCopyTheError() {

        val error =
            "error"

        every {
            httpCall.error
        } returns error

        val errorsForCopy =
            dataCopyHelper
                .getErrorsForCopy()

        assertThat(
            errorsForCopy,
            `is`(error)
        )
    }

    @Test
    fun shouldShareRequestResponseData() {

        val requestBody =
            "request body"

        val formatRequestBody =
            "format Request body"

        val responseBody =
            "response body"

        val formatResponseBody =
            "format Response body"

        val httpHeader =
            acceptLanguageHttpHeader

        every {
            httpCall.requestHeaders
        } returns listOf(
            httpHeader,
            jsonContentTypeHeader
        )

        every {
            httpCall.getRequestHeader(
                "Content-Type"
            )
        } returns jsonContentTypeHeader

        every {
            httpCall.payload
        } returns requestBody

        every {
            httpCall.responseHeaders
        } returns listOf(
            httpHeader,
            header
        )

        every {
            httpCall.getResponseHeader(
                "Content-Type"
            )
        } returns jsonContentTypeHeader

        every {
            httpCall.responseBody
        } returns responseBody

        every {
            httpCall.date
        } returns getDate(
            2017,
            4,
            12,
            1,
            2,
            3
        )

        every {
            formatterFactory.getFor(
                "application/json"
            )
        } returns responseFormatter

        every {
            responseFormatter.format(
                requestBody
            )
        } returns formatRequestBody

        every {
            responseFormatter.format(
                responseBody
            )
        } returns formatResponseBody

        val httpCallData =
            dataCopyHelper
                .getHttpCallData()

        assertThat(
            httpCallData.toString(),
            `is`(
                "Request Body\n" +
                    "format Request body\n" +
                    "Request Headers\n" +
                    "accept-language: en-US,en;q=0.8,hi;q=0.6\n" +
                    "Content-Type: application/json\n" +
                    "Response Body\n" +
                    "format Response body\n" +
                    "Response Headers\n" +
                    "accept-language: en-US,en;q=0.8,hi;q=0.6\n" +
                    "Header: headerValue\n"
            )
        )

        verify(exactly = 1) {
            responseFormatter.format(
                requestBody
            )
        }

        verify(exactly = 1) {
            responseFormatter.format(
                responseBody
            )
        }
    }

    @Test
    fun shouldShareResponseDataOnlyIfRequestDataEmpty() {

        val responseBody =
            "response body"

        val formatResponseBody =
            "format Response body"

        every {
            httpCall.getResponseHeader(
                "Content-Type"
            )
        } returns jsonContentTypeHeader

        every {
            httpCall.responseBody
        } returns responseBody

        every {
            httpCall.requestHeaders
        } returns null

        every {
            httpCall.responseHeaders
        } returns null

        every {
            formatterFactory.getFor(
                "application/json"
            )
        } returns responseFormatter

        every {
            responseFormatter.format(
                responseBody
            )
        } returns formatResponseBody

        val httpCallData =
            dataCopyHelper
                .getHttpCallData()

        assertThat(
            httpCallData.toString(),
            `is`(
                "Response Body\n" +
                    "format Response body"
            )
        )

        verify(exactly = 1) {
            responseFormatter.format(
                responseBody
            )
        }
    }

    @Test
    fun shouldShareRequestDataOnlyIfResponseDataEmpty() {

        val requestBody =
            "request body"

        val formatRequestBody =
            "format Request body"

        every {
            httpCall.getRequestHeader(
                "Content-Type"
            )
        } returns jsonContentTypeHeader

        every {
            httpCall.payload
        } returns requestBody

        every {
            httpCall.responseBody
        } returns null

        every {
            httpCall.requestHeaders
        } returns null

        every {
            httpCall.responseHeaders
        } returns null

        every {
            formatterFactory.getFor(
                "application/json"
            )
        } returns responseFormatter

        every {
            responseFormatter.format(
                requestBody
            )
        } returns formatRequestBody

        val httpCallData =
            dataCopyHelper
                .getHttpCallData()

        assertThat(
            httpCallData.toString(),
            `is`(
                "Request Body\n" +
                    "format Request body"
            )
        )

        verify(exactly = 1) {
            responseFormatter.format(
                requestBody
            )
        }
    }

    private fun mockStringResources() {

        every {
            resources.getString(
                R.string.request_body_heading
            )
        } returns "Request Body"

        every {
            resources.getString(
                R.string.request_headers
            )
        } returns "Request Headers"

        every {
            resources.getString(
                R.string.response_body_heading
            )
        } returns "Response Body"

        every {
            resources.getString(
                R.string.response_headers
            )
        } returns "Response Headers"
    }
}
