package org.springframework.http.client

import com.prateekj.snooper.AndroidSnooper
import com.prateekj.snooper.networksnooper.model.HttpCall
import org.springframework.http.HttpRequest
import java.io.IOException
import java.nio.charset.Charset

class SnooperInterceptor : ClientHttpRequestInterceptor {

    @Throws(IOException::class)
    override fun intercept(
        httpRequest: HttpRequest,
        bytes: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {

        val payload = try {

            String(
                bytes,
                Charset.forName("UTF-8")
            )

        } catch (_: Throwable) {
            ""
        }

        val builder = HttpCall.Builder()

            .withMethod(
                httpRequest.method.toString()
            )

            .withUrl(
                httpRequest.uri.toString()
            )

            .withPayload(
                payload
            )

            .withRequestHeaders(
                httpRequest.headers
            )

        return try {

            val response =
                execution.execute(
                    httpRequest,
                    bytes
                )

            val responseBody = try {

                response.body
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

            } catch (_: Throwable) {
                ""
            }

            val statusCode = try {

                response.statusCode.value()

            } catch (_: Throwable) {

                -1
            }

            val httpCall = builder

                .withResponseBody(
                    responseBody
                )

                .withStatusCode(
                    statusCode
                )

                .withStatusText(
                    response.statusText
                )

                .withResponseHeaders(
                    response.headers
                )

                .build()

            try {

                AndroidSnooper
                    .instance
                    .record(httpCall)

            } catch (_: Throwable) {
            }

            response

        } catch (e: Exception) {

            val httpCall = builder

                .withError(
                    e.toString()
                )

                .build()

            try {

                AndroidSnooper
                    .instance
                    .record(httpCall)

            } catch (_: Throwable) {
            }

            throw e
        }
    }
}
