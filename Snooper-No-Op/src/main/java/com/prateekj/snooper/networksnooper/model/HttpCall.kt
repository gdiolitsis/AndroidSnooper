package com.prateekj.snooper.networksnooper.model

import java.util.Date

data class HttpCall(

    val payload: String = "",

    val method: String = "",

    val url: String = "",

    val responseBody: String = "",

    val statusText: String = "",

    val statusCode: Int = -1,

    val date: Date = Date(),

    val requestHeaders: Map<String, List<String>> = emptyMap(),

    val responseHeaders: Map<String, List<String>> = emptyMap(),

    val error: String = ""

) {

    class Builder {

        private var payload: String = ""

        private var method: String = ""

        private var url: String = ""

        private var responseBody: String = ""

        private var statusText: String = ""

        private var statusCode: Int = -1

        private var date: Date = Date()

        private var requestHeaders:
                Map<String, List<String>> = emptyMap()

        private var responseHeaders:
                Map<String, List<String>> = emptyMap()

        private var error: String = ""

        fun withMethod(
            httpMethod: String
        ) = apply {

            this.method = httpMethod
        }

        fun withUrl(
            url: String
        ) = apply {

            this.url = url
        }

        fun withPayload(
            payload: String
        ) = apply {

            this.payload = payload
        }

        fun withResponseBody(
            responseBody: String
        ) = apply {

            this.responseBody = responseBody
        }

        fun withStatusText(
            statusText: String
        ) = apply {

            this.statusText = statusText
        }

        fun withStatusCode(
            rawStatusCode: Int
        ) = apply {

            this.statusCode = rawStatusCode
        }

        fun withRequestHeaders(
            headers: Map<String, List<String>>
        ) = apply {

            this.requestHeaders = headers
        }

        fun withResponseHeaders(
            headers: Map<String, List<String>>
        ) = apply {

            this.responseHeaders = headers
        }

        fun withError(
            error: String
        ) = apply {

            this.error = error
        }

        fun withDate(
            date: Date
        ) = apply {

            this.date = date
        }

        fun build(): HttpCall {

            return HttpCall(
                payload = payload,
                method = method,
                url = url,
                responseBody = responseBody,
                statusText = statusText,
                statusCode = statusCode,
                date = date,
                requestHeaders = requestHeaders,
                responseHeaders = responseHeaders,
                error = error
            )
        }
    }
}
