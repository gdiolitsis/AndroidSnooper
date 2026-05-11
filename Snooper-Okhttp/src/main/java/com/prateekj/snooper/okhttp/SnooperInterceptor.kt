package com.prateekj.snooper.okhttp

import com.prateekj.snooper.AndroidSnooper
import com.prateekj.snooper.networksnooper.model.HttpCall
import com.prateekj.snooper.utils.Logger
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException

class SnooperInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(
        chain: Interceptor.Chain
    ): Response {

        val request = chain.request()

        val builder = HttpCall.Builder()

            .withUrl(request.url.toString())

            .withPayload(getRequestBody(request))

            .withMethod(request.method)

            .withRequestHeaders(
                headers(request.headers)
            )

        return try {

            val response =
                chain.proceed(request)

            val responseBodyString = try {

                response.body
                    ?.string()
                    ?: ""

            } catch (_: Throwable) {
                ""
            }

            val httpCall = builder

                .withResponseBody(
                    responseBodyString
                )

                .withStatusCode(
                    response.code
                )

                .withStatusText(
                    response.message
                )

                .withResponseHeaders(
                    headers(response.headers)
                )

                .build()

            try {
                AndroidSnooper.instance.record(httpCall)
            } catch (_: Throwable) {}

            response.newBuilder()

                .body(
                    responseBodyString.toResponseBody(
                        response.body?.contentType()
                    )
                )

                .build()

        } catch (e: Exception) {

            val httpCall = builder

                .withError(
                    e.toString()
                )

                .build()

            try {
                AndroidSnooper.instance.record(httpCall)
            } catch (_: Throwable) {}

            throw e
        }
    }

    private fun headers(
        headers: Headers
    ): Map<String, List<String>> {

        val extractedHeaders =
            HashMap<String, List<String>>()

        for (headerName in headers.names()) {

            extractedHeaders[headerName] =
                headers.values(headerName)
        }

        return extractedHeaders
    }

    private fun getRequestBody(
        request: Request
    ): String {

        return try {

            val body = request.body ?: return ""

            val buffer = Buffer()

            body.writeTo(buffer)

            buffer.readUtf8()

        } catch (e: IOException) {

            Logger.e(
                TAG,
                "Could not retrieve request body",
                e
            )

            ""
        }
    }

    companion object {

        private val TAG =
            SnooperInterceptor::class.java.simpleName
    }
}
