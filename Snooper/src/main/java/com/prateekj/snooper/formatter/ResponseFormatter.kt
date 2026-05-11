package com.prateekj.snooper.formatter

fun interface ResponseFormatter {

    fun format(
        response: String
    ): String
}
