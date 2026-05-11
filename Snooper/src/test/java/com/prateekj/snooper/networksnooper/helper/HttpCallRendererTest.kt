package com.prateekj.snooper.networksnooper.helper

import androidx.fragment.app.Fragment
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.ERROR
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.HEADERS
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.REQUEST
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.RESPONSE
import com.prateekj.snooper.networksnooper.views.HttpCallView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.sameInstance
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

class HttpCallRendererTest {

    private lateinit var httpCallView:
            HttpCallView

    private lateinit var httpCallRenderer:
            HttpCallRenderer

    @Before
    fun setUp() {

        httpCallView =
            mockk(relaxed = true)

        httpCallRenderer =
            HttpCallRenderer(
                httpCallView,
                false
            )
    }

    @Test
    fun shouldReturnResponseTypeFragment() {

        val expectedFragment =
            mockk<Fragment>()

        every {
            httpCallView
                .getResponseBodyFragment()
        } returns expectedFragment

        val fragment =
            httpCallRenderer
                .getFragment(0)

        assertThat(
            fragment,
            sameInstance(expectedFragment)
        )

        verify(exactly = 1) {
            httpCallView
                .getResponseBodyFragment()
        }
    }

    @Test
    fun shouldReturnErrorTypeFragment() {

        val expectedFragment =
            mockk<Fragment>()

        every {
            httpCallView
                .getExceptionFragment()
        } returns expectedFragment

        val renderer =
            HttpCallRenderer(
                httpCallView,
                true
            )

        val fragment =
            renderer.getFragment(0)

        assertThat(
            fragment,
            sameInstance(expectedFragment)
        )

        verify(exactly = 1) {
            httpCallView
                .getExceptionFragment()
        }
    }

    @Test
    fun shouldReturnRequestTypeFragment() {

        val expectedFragment =
            mockk<Fragment>()

        every {
            httpCallView
                .getRequestBodyFragment()
        } returns expectedFragment

        val fragment =
            httpCallRenderer
                .getFragment(1)

        assertThat(
            fragment,
            sameInstance(expectedFragment)
        )

        verify(exactly = 1) {
            httpCallView
                .getRequestBodyFragment()
        }
    }

    @Test
    fun shouldReturnHeaderTypeFragment() {

        val expectedFragment =
            mockk<Fragment>()

        every {
            httpCallView
                .getHeadersFragment()
        } returns expectedFragment

        val fragment =
            httpCallRenderer
                .getFragment(2)

        assertThat(
            fragment,
            sameInstance(expectedFragment)
        )

        verify(exactly = 1) {
            httpCallView
                .getHeadersFragment()
        }
    }

    @Test
    fun shouldReturnHttpCallTabs() {

        val tabs =
            httpCallRenderer
                .getTabs()

        assertThat(
            tabs.size,
            `is`(3)
        )

        assertThat(
            tabs[0],
            `is`(RESPONSE)
        )

        assertThat(
            tabs[1],
            `is`(REQUEST)
        )

        assertThat(
            tabs[2],
            `is`(HEADERS)
        )
    }

    @Test
    fun shouldReturnHttpCallTabsForErrorMode() {

        val renderer =
            HttpCallRenderer(
                httpCallView,
                true
            )

        val tabs =
            renderer.getTabs()

        assertThat(
            tabs.size,
            `is`(3)
        )

        assertThat(
            tabs[0],
            `is`(ERROR)
        )

        assertThat(
            tabs[1],
            `is`(REQUEST)
        )

        assertThat(
            tabs[2],
            `is`(HEADERS)
        )
    }
}
