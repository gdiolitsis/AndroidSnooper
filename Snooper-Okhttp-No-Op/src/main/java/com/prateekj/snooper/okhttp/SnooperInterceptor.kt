package com.prateekj.snooper.okhttp

import com.prateekj.snooper.AndroidSnooper
import com.prateekj.snooper.networksnooper.model.HttpCall
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.nio.charset.Charset

class SnooperInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(
        chain: Interceptor.Chain
    ): Response {

        val request = chain.request()

        val requestBody = try {

            request.body?.let {

                val buffer = Buffer()

                it.writeTo(buffer)

                buffer.readString(
                    Charset.forName("UTF-8")
                )
            } ?: ""

        } catch (_: Throwable) {
            ""
        }

        val startNs = System.nanoTime()

        return try {

            val response =
                chain.proceed(request)

            val tookMs =
                (System.nanoTime() - startNs) / 1_000_000

            val responseBody = try {

                response.peekBody(
                    Long.MAX_VALUE
                ).string()

            } catch (_: Throwable) {
                ""
            }

            val httpCall = HttpCall.Builder()

                .withMethod(request.method)

                .withUrl(request.url.toString())

                .withPayload(requestBody)

                .withResponseBody(responseBody)

                .withStatusCode(response.code)

                .withStatusText(
                    "${response.code} (${tookMs}ms)"
                )

                .withRequestHeaders(
                    request.headers.toMultimap()
                )

                .withResponseHeaders(
                    response.headers.toMultimap()
                )

                .build()

            try {
                AndroidSnooper.instance.record(httpCall)
            } catch (_: Throwable) {}

            response

        } catch (e: Exception) {

            val httpCall = HttpCall.Builder()

                .withMethod(request.method)

                .withUrl(request.url.toString())

                .withPayload(requestBody)

                .withError(
                    e.message ?: "Unknown error"
                )

                .build()

            try {
                AndroidSnooper.instance.record(httpCall)
            } catch (_: Throwable) {}

            throw e
        }
    }
}
