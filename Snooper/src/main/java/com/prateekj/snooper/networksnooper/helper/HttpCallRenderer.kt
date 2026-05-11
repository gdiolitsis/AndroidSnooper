package com.prateekj.snooper.networksnooper.helper

import androidx.fragment.app.Fragment
import com.prateekj.snooper.networksnooper.activity.HttpCallTab
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.ERROR
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.HEADERS
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.REQUEST
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.RESPONSE
import com.prateekj.snooper.networksnooper.views.HttpCallView

class HttpCallRenderer(
    private val httpCallView:
            HttpCallView,
    private val hasError:
            Boolean
) {

    private val tabs:
            List<HttpCallTab> by lazy {

        if (hasError) {

            listOf(
                ERROR,
                REQUEST,
                HEADERS
            )

        } else {

            listOf(
                RESPONSE,
                REQUEST,
                HEADERS
            )
        }
    }

    fun getTabs():
            List<HttpCallTab> {

        return tabs
    }

    fun getFragment(
        position: Int
    ): Fragment {

        return when (
            tabs.getOrNull(position)
        ) {

            ERROR -> {

                httpCallView
                    .getExceptionFragment()
            }

            RESPONSE -> {

                httpCallView
                    .getResponseBodyFragment()
            }

            REQUEST -> {

                httpCallView
                    .getRequestBodyFragment()
            }

            HEADERS -> {

                httpCallView
                    .getHeadersFragment()
            }

            null -> {

                Fragment()
            }
        }
    }
}
