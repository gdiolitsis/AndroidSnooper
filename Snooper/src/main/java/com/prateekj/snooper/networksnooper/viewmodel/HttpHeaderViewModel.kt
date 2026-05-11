package com.prateekj.snooper.networksnooper.viewmodel

import com.prateekj.snooper.networksnooper.model.HttpHeader

class HttpHeaderViewModel(
    private val httpHeader: HttpHeader
) {

    fun headerName():
            String {

        return httpHeader.name
            .orEmpty()
    }

    fun headerValues():
            String {

        return httpHeader.values
            .joinToString(";") {

                it.value
            }
    }
}
