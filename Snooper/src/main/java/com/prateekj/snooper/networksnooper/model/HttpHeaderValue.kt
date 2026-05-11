package com.prateekj.snooper.networksnooper.model

data class HttpHeaderValue(

    var value: String = ""
) {

    private var id: Int = 0

    fun setId(
        id: Int
    ) {

        this.id = id
    }

    companion object {

        fun from(
            strings: List<String>
        ): List<HttpHeaderValue> {

            return strings.map {

                HttpHeaderValue(it)
            }
        }
    }
}
