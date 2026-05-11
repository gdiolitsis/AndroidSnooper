package com.prateekj.snooper.networksnooper.viewmodel

import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.annotation.ColorRes
import com.prateekj.snooper.R
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.networksnooper.model.HttpHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale.US

class HttpCallViewModel(
    private val httpCall:
            HttpCallRecord
) {

    val url: String =
        httpCall.url.orEmpty()

    val method: String =
        httpCall.method.orEmpty()

    val statusCode: String =
        if (httpCall.statusCode > 0) {
            httpCall.statusCode.toString()
        } else {
            "-"
        }

    val statusText: String =
        httpCall.statusText.orEmpty()

    val requestHeaders:
            List<HttpHeader> =
        httpCall.requestHeaders
            ?: emptyList()

    val responseHeaders:
            List<HttpHeader> =
        httpCall.responseHeaders
            ?: emptyList()

    val timeStamp: String
        get() {

            val dateFormat =
                SimpleDateFormat(
                    TIMESTAMP_FORMAT,
                    US
                )

            val safeDate =
                httpCall.date
                    ?: Date(0)

            return dateFormat.format(
                safeDate
            )
        }

    val responseInfoVisibility: Int
        get() {

            return if (
                httpCall.hasError()
            ) {

                GONE

            } else {

                VISIBLE
            }
        }

    val failedTextVisibility: Int
        get() {

            return if (
                httpCall.hasError()
            ) {

                VISIBLE

            } else {

                GONE
            }
        }

    val responseHeaderVisibility: Int
        get() {

            return if (
                hasHeaders(
                    responseHeaders
                )
            ) {

                VISIBLE

            } else {

                GONE
            }
        }

    val requestHeaderVisibility: Int
        get() {

            return if (
                hasHeaders(
                    requestHeaders
                )
            ) {

                VISIBLE

            } else {

                GONE
            }
        }

    @ColorRes
    fun getStatusColor():
            Int {

        val code =
            httpCall.statusCode

        return when {

            code in
                    RANGE_START_HTTP_OK..
                    RANGE_END_HTTP_OK -> {

                R.color.snooper_green
            }

            code in
                    RANGE_START_HTTP_REDIRECTION..
                    RANGE_END_HTTP_REDIRECTION -> {

                R.color.snooper_yellow
            }

            else -> {

                R.color.snooper_red
            }
        }
    }

    private fun hasHeaders(
        headers: List<HttpHeader>
    ): Boolean {

        return headers.isNotEmpty()
    }

    companion object {

        private const val TIMESTAMP_FORMAT =
            "MM/dd/yyyy HH:mm:ss"

        private const val RANGE_START_HTTP_OK =
            200

        private const val RANGE_END_HTTP_OK =
            299

        private const val RANGE_START_HTTP_REDIRECTION =
            300

        private const val RANGE_END_HTTP_REDIRECTION =
            399
    }
}
