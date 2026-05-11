package com.prateekj.snooper.utils

import android.util.Log

object Logger {

    private const val SNOOPER_DEBUGGER_TAG =
        "AndroidSnooper"

    fun d(
        tag: String,
        message: String?
    ) {

        if (message.isNullOrBlank()) {
            return
        }

        Log.d(
            buildTag(tag),
            message
        )
    }

    fun e(
        tag: String,
        message: String?,
        exception: Throwable
    ) {

        Log.e(
            buildTag(tag),
            message.orEmpty(),
            exception
        )
    }

    fun e(
        tag: String,
        message: String?
    ) {

        Log.e(
            buildTag(tag),
            message.orEmpty()
        )
    }

    private fun buildTag(
        tag: String
    ): String {

        return "${SNOOPER_DEBUGGER_TAG}_$tag"
            .take(MAX_TAG_LENGTH)
    }

    private const val MAX_TAG_LENGTH =
        23
}
