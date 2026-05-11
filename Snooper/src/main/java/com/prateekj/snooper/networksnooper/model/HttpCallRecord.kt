package com.prateekj.snooper.networksnooper.model

import java.util.Date

class HttpCallRecord {

    var id: Long = 0

    var url: String? = null

    var payload: String? = null

    var method: String? = null

    var responseBody: String? = null

    var statusText: String? = null

    var statusCode: Int = 0

    var date: Date? = null

    var error: String? = null

    var requestHeaders:
            List<HttpHeader>? = null

    var responseHeaders:
            List<HttpHeader>? = null

    fun getResponseHeader(
        name: String
    ): HttpHeader? {

        return filterFromCollection(
            name,
            responseHeaders
        )
    }

    fun getRequestHeader(
        name: String
    ): HttpHeader? {

        return filterFromCollection(
            name,
            requestHeaders
        )
    }

    private fun filterFromCollection(
        name: String,
        collection: List<HttpHeader>?
    ): HttpHeader? {

        return collection?.firstOrNull {

            it.name.equals(
                name,
                ignoreCase = true
            )
        }
    }

    fun hasError():
            Boolean {

        return error != null
    }

    companion object {

        fun from(
            httpCall: HttpCall
        ): HttpCallRecord {

            return HttpCallRecord().apply {

                url =
                    httpCall.url

                payload =
                    httpCall.payload

                method =
                    httpCall.method

                responseBody =
                    httpCall.responseBody

                statusText =
                    httpCall.statusText

                statusCode =
                    httpCall.statusCode

                date =
                    httpCall.date

                error =
                    httpCall.error

                requestHeaders =
                    HttpHeader.from(
                        httpCall.requestHeaders
                    )

                responseHeaders =
                    HttpHeader.from(
                        httpCall.responseHeaders
                    )
            }
        }
    }
}
