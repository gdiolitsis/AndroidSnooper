package com.prateekj.snooper.networksnooper.helper

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.prateekj.snooper.networksnooper.activity.HttpCallTab
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.ERROR
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.HEADERS
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.REQUEST
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.RESPONSE
import com.prateekj.snooper.networksnooper.fragment.HttpCallFragment
import com.prateekj.snooper.networksnooper.fragment.HttpHeadersFragment
import com.prateekj.snooper.networksnooper.views.HttpCallView

class HttpCallRenderer(
    private val httpCallView: HttpCallView,
    private val errorMode: Boolean
) {

    private val tabs: List<HttpCallTab> =
        if (errorMode) {
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

    fun getFragment(
        position: Int
    ): Fragment {

        return when (tabs[position]) {

            RESPONSE -> {
                httpCallView.getResponseBodyFragment()
            }

            ERROR -> {
                httpCallView.getExceptionFragment()
            }

            REQUEST -> {
                httpCallView.getRequestBodyFragment()
            }

            else -> {
                httpCallView.getHeadersFragment()
            }
        }
    }

    fun tabs():
            List<HttpCallTab> {

        return tabs
    }
}
