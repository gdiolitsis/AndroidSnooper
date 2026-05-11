package org.springframework.http.client

import com.prateekj.snooper.networksnooper.model.HttpCall
import org.springframework.http.HttpRequest
import java.io.IOException
import java.nio.charset.Charset

class SpringHttpRequestTransformer {

    @Throws(IOException::class)
    fun transform(
        httpRequest: HttpRequest,
        requestPayload: ByteArray,
        httpResponse: ClientHttpResponse
    ): HttpCall {

        val payload = try {

            String(
                requestPayload,
                Charset.forName("UTF-8")
            )

        } catch (_: Throwable) {
            ""
        }

        val responseBody = try {

            httpResponse.body
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: ""

        } catch (_: Throwable) {
            ""
        }

        return HttpCall.Builder()

            .withUrl(
                httpRequest.uri.toString()
            )

            .withPayload(
                payload
            )

            .withMethod(
                httpRequest.method.toString()
            )

            .withResponseBody(
                responseBody
            )

            .withStatusCode(
                httpResponse.rawStatusCode
            )

            .withStatusText(
                httpResponse.statusCode.reasonPhrase
            )

            .withRequestHeaders(
                httpRequest.headers
            )

            .withResponseHeaders(
                httpResponse.headers
            )

            .build()
    }

    @Throws(IOException::class)
    fun transform(
        httpRequest: HttpRequest,
        requestPayload: ByteArray,
        e: Exception
    ): HttpCall {

        val payload = try {

            String(
                requestPayload,
                Charset.forName("UTF-8")
            )

        } catch (_: Throwable) {
            ""
        }

        return HttpCall.Builder()

            .withUrl(
                httpRequest.uri.toString()
            )

            .withPayload(
                payload
            )

            .withMethod(
                httpRequest.method.toString()
            )

            .withRequestHeaders(
                httpRequest.headers
            )

            .withError(
                e.toString()
            )

            .build()
    }
}
