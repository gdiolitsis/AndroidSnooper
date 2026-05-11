package com.prateekj.snooper.networksnooper.model

data class HttpHeader(

    var id: Int = 0,

    var name: String? = null,

    var values:
            List<HttpHeaderValue> = emptyList()
) {

    companion object {

        const val CONTENT_TYPE =
            "Content-Type"

        fun from(
            headers: Map<String, List<String>>
        ): List<HttpHeader> {

            return headers.entries.map { entry ->

                HttpHeader(
                    name = entry.key,
                    values = HttpHeaderValue.from(
                        entry.value
                    )
                )
            }
        }
    }
}
