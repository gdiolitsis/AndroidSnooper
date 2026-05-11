package com.prateekj.snooper.networksnooper.model

class HttpHeader {

    var id: Int = 0

    var name: String? = null

    var values:
            List<HttpHeaderValue> =
        emptyList()

    constructor()

    constructor(
        name: String
    ) {
        this.name = name
    }

    companion object {

        const val CONTENT_TYPE =
            "Content-Type"

        fun from(
            headers: Map<String, List<String>>
        ): List<HttpHeader> {

            return headers.entries.map { entry ->

                HttpHeader(
                    entry.key
                ).apply {

                    values =
                        HttpHeaderValue.from(
                            entry.value
                        )
                }
            }
        }
    }
}
