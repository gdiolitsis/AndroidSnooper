package com.prateekj.snooper.networksnooper.helper

import android.content.res.Resources
import com.prateekj.snooper.R
import com.prateekj.snooper.formatter.ResponseFormatterFactory
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.networksnooper.model.HttpHeader
import com.prateekj.snooper.networksnooper.model.HttpHeader.Companion.CONTENT_TYPE

class DataCopyHelper(
    private val httpCallRecord:
            HttpCallRecord,
    private val responseFormatterFactory:
            ResponseFormatterFactory,
    private val resources:
            Resources
) {

    fun getResponseDataForCopy():
            String {

        return getFormattedData(
            httpCallRecord.getResponseHeader(
                CONTENT_TYPE
            ),
            httpCallRecord.responseBody
        )
    }

    fun getRequestDataForCopy():
            String {

        return getFormattedData(
            httpCallRecord.getRequestHeader(
                CONTENT_TYPE
            ),
            httpCallRecord.payload
        )
    }

    fun getErrorsForCopy():
            String {

        return httpCallRecord.error.orEmpty()
    }

    fun getHeadersForCopy():
            String {

        return buildString {

            appendHeaders(
                httpCallRecord.requestHeaders,
                this,
                resources.getString(
                    R.string.request_headers
                )
            )

            appendHeaders(
                httpCallRecord.responseHeaders,
                this,
                resources.getString(
                    R.string.response_headers
                )
            )
        }
    }

    fun getHttpCallData():
            StringBuilder {

        return StringBuilder().apply {

            appendRequestData(this)
            appendResponseData(this)
        }
    }

    private fun appendRequestData(
        builder: StringBuilder
    ) {

        val formattedRequestData =
            getRequestDataForCopy()

        if (
            formattedRequestData.isNotEmpty()
        ) {

            val heading =
                resources.getString(
                    R.string.request_body_heading
                )

            builder.appendLine(heading)
            builder.appendLine(
                formattedRequestData
            )
        }

        appendHeaders(
            httpCallRecord.requestHeaders,
            builder,
            resources.getString(
                R.string.request_headers
            )
        )
    }

    private fun appendResponseData(
        builder: StringBuilder
    ) {

        val formattedResponseData =
            getResponseDataForCopy()

        if (
            formattedResponseData.isNotEmpty()
        ) {

            val heading =
                resources.getString(
                    R.string.response_body_heading
                )

            builder.appendLine(heading)
            builder.appendLine(
                formattedResponseData
            )
        }

        appendHeaders(
            httpCallRecord.responseHeaders,
            builder,
            resources.getString(
                R.string.response_headers
            )
        )
    }

    private fun appendHeaders(
        headers: List<HttpHeader>?,
        builder: StringBuilder,
        heading: String
    ) {

        if (
            headers.isNullOrEmpty()
        ) {
            return
        }

        builder.appendLine()
        builder.appendLine(heading)

        headers.forEach { header ->

            builder.appendLine(
                "${header.name}: ${
                    toHeaderValues(header)
                }"
            )
        }
    }

    private fun toHeaderValues(
        httpHeader: HttpHeader
    ): String {

        return httpHeader.values
            .joinToString(";") {
                it.value
            }
    }

    private fun getFormattedData(
        contentTypeHeader: HttpHeader?,
        dataToCopy: String?
    ): String {

        if (
            dataToCopy.isNullOrBlank()
        ) {
            return ""
        }

        val contentType =
            contentTypeHeader
                ?.values
                ?.firstOrNull()
                ?.value
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return dataToCopy

        val formatter =
            responseFormatterFactory.getFor(
                contentType
            )

        return formatter.format(
            dataToCopy
        )
    }
}
