package com.prateekj.snooper.networksnooper.activity

import com.prateekj.snooper.R

enum class HttpCallTab(
    val tabTitle: Int
) {

    RESPONSE(
        R.string.response
    ),

    REQUEST(
        R.string.request
    ),

    HEADERS(
        R.string.headers
    ),

    ERROR(
        R.string.error
    );

    companion object {

        fun defaultTabs():
                List<HttpCallTab> {

            return listOf(
                RESPONSE,
                REQUEST,
                HEADERS
            )
        }

        fun tabsWithError():
                List<HttpCallTab> {

            return listOf(
                RESPONSE,
                REQUEST,
                HEADERS,
                ERROR
            )
        }
    }
}
