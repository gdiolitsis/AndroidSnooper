package com.prateekj.snooper.networksnooper.viewmodel

data class HttpBodyViewModel(

    var formattedBody: String = ""
) {

    fun init(
        formattedBody: String
    ) {

        this.formattedBody =
            formattedBody
    }
}
