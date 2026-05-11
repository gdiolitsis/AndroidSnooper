package org.springframework.http.client

import com.prateekj.snooper.AndroidSnooper
import org.springframework.http.HttpRequest
import java.io.IOException

class SnooperInterceptor : ClientHttpRequestInterceptor {

    @Throws(IOException::class)
    override fun intercept(
        request: HttpRequest,
        byteArray: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {

        val transformer =
            SpringHttpRequestTransformer()

        val snooper =
            AndroidSnooper.instance

        return try {

            val streamResponse =
                execution.execute(
                    request,
                    byteArray
                )

            val httpResponse =
                BufferingClientHttpResponseWrapper(
                    streamResponse
                )

            val call =
                transformer.transform(
                    request,
                    byteArray,
                    httpResponse
                )

            try {
                snooper.record(call)
            } catch (_: Throwable) {}

            httpResponse

        } catch (e: Exception) {

            try {

                val call =
                    transformer.transform(
                        request,
                        byteArray,
                        e
                    )

                snooper.record(call)

            } catch (_: Throwable) {}

            throw e
        }
    }
}
