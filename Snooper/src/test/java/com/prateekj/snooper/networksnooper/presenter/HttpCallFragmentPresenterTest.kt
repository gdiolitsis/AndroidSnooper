package com.prateekj.snooper.networksnooper.presenter

import com.prateekj.snooper.formatter.ResponseFormatter
import com.prateekj.snooper.formatter.ResponseFormatterFactory
import com.prateekj.snooper.infra.BackgroundTask
import com.prateekj.snooper.infra.BackgroundTaskExecutor
import com.prateekj.snooper.networksnooper.activity.HttpCallActivity.Companion.ERROR_MODE
import com.prateekj.snooper.networksnooper.activity.HttpCallActivity.Companion.REQUEST_MODE
import com.prateekj.snooper.networksnooper.activity.HttpCallActivity.Companion.RESPONSE_MODE
import com.prateekj.snooper.networksnooper.database.SnooperRepo
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.networksnooper.model.HttpHeader
import com.prateekj.snooper.networksnooper.model.HttpHeaderValue
import com.prateekj.snooper.networksnooper.viewmodel.HttpBodyViewModel
import com.prateekj.snooper.networksnooper.views.HttpCallBodyView
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

class HttpCallFragmentPresenterTest {

    private lateinit var presenter:
            HttpCallFragmentPresenter

    private lateinit var repo:
            SnooperRepo

    private lateinit var viewModel:
            HttpBodyViewModel

    private lateinit var httpCallRecord:
            HttpCallRecord

    private lateinit var factory:
            ResponseFormatterFactory

    private lateinit var responseFormatter:
            ResponseFormatter

    private lateinit var responseBody:
            String

    private lateinit var requestPayload:
            String

    private lateinit var error:
            String

    private lateinit var formattedBody:
            String

    private lateinit var mockExecutor:
            BackgroundTaskExecutor

    private lateinit var httpCallBodyView:
            HttpCallBodyView

    private val jsonContentTypeHeader:
            HttpHeader
        get() {

            return HttpHeader(
                name = "Content-Type",
                values = listOf(
                    HttpHeaderValue(
                        "application/json"
                    )
                )
            )
        }

    private val xmlContentTypeHeader:
            HttpHeader
        get() {

            return HttpHeader(
                name = "Content-Type",
                values = listOf(
                    HttpHeaderValue(
                        "application/xml"
                    )
                )
            )
        }

    @Before
    fun setUp() {

        responseBody =
            "response body"

        requestPayload =
            "payload"

        error =
            "error"

        formattedBody =
            "formatted body"

        httpCallRecord =
            mockk(relaxed = true)

        repo =
            mockk(relaxed = true)

        viewModel =
            mockk(relaxed = true)

        factory =
            mockk(relaxed = true)

        mockExecutor =
            mockk(relaxed = true)

        httpCallBodyView =
            mockk(relaxed = true)

        presenter =
            HttpCallFragmentPresenter(
                repo,
                HTTP_CALL_ID,
                httpCallBodyView,
                factory,
                mockExecutor
            )

        responseFormatter =
            mockk(relaxed = true)

        every {
            httpCallRecord.responseBody
        } returns responseBody

        every {
            httpCallRecord.payload
        } returns requestPayload

        every {
            httpCallRecord.error
        } returns error

        every {
            repo.findById(
                HTTP_CALL_ID
            )
        } returns httpCallRecord

        every {
            factory.getFor(
                any()
            )
        } returns responseFormatter

        every {
            responseFormatter.format(
                any()
            )
        } returns formattedBody
    }

    @Test
    fun shouldInitializeWithJsonFormatterForResponseMode() {

        every {
            httpCallRecord.getResponseHeader(
                "Content-Type"
            )
        } returns jsonContentTypeHeader

        resolveBackgroundTask()

        presenter.init(
            viewModel,
            RESPONSE_MODE
        )

        verify(exactly = 1) {
            factory.getFor(
                "application/json"
            )
        }

        verify(exactly = 1) {
            responseFormatter.format(
                responseBody
            )
        }

        verify(exactly = 1) {
            viewModel.init(
                formattedBody
            )
        }
    }

    @Test
    fun shouldInitializeWithXmlFormatterForResponseMode() {

        every {
            httpCallRecord.getResponseHeader(
                "Content-Type"
            )
        } returns xmlContentTypeHeader

        resolveBackgroundTask()

        presenter.init(
            viewModel,
            RESPONSE_MODE
        )

        verify(exactly = 1) {
            factory.getFor(
                "application/xml"
            )
        }

        verify(exactly = 1) {
            responseFormatter.format(
                responseBody
            )
        }

        verify(exactly = 1) {
            viewModel.init(
                formattedBody
            )
        }
    }

    @Test
    fun shouldInitializeWithErrorContent() {

        every {
            httpCallRecord.getResponseHeader(
                "Content-Type"
            )
        } returns null

        resolveBackgroundTask()

        presenter.init(
            viewModel,
            ERROR_MODE
        )

        verify(exactly = 1) {
            viewModel.init(
                error
            )
        }
    }

    @Test
    fun shouldInitializeWithJsonFormatterForRequestMode() {

        every {
            httpCallRecord.getRequestHeader(
                "Content-Type"
            )
        } returns jsonContentTypeHeader

        resolveBackgroundTask()

        presenter.init(
            viewModel,
            REQUEST_MODE
        )

        verify(exactly = 1) {
            factory.getFor(
                "application/json"
            )
        }

        verify(exactly = 1) {
            responseFormatter.format(
                requestPayload
            )
        }

        verify(exactly = 1) {
            viewModel.init(
                formattedBody
            )
        }
    }

    @Test
    fun shouldInitializeWithXmlFormatterForRequestMode() {

        every {
            httpCallRecord.getRequestHeader(
                "Content-Type"
            )
        } returns xmlContentTypeHeader

        resolveBackgroundTask()

        presenter.init(
            viewModel,
            REQUEST_MODE
        )

        verify(exactly = 1) {
            factory.getFor(
                "application/xml"
            )
        }

        verify(exactly = 1) {
            responseFormatter.format(
                requestPayload
            )
        }

        verify(exactly = 1) {
            viewModel.init(
                formattedBody
            )
        }
    }

    @Test
    fun shouldUsePlainTextFormatterWhenContentTypeHeaderNotFound() {

        every {
            httpCallRecord.getRequestHeader(
                "Content-Type"
            )
        } returns null

        resolveBackgroundTask()

        presenter.init(
            viewModel,
            REQUEST_MODE
        )

        verify {
            factory wasNot Called
        }

        verify(exactly = 1) {
            viewModel.init(
                requestPayload
            )
        }
    }

    @Test
    fun shouldNotifyViewOnFormattingDone() {

        every {
            httpCallRecord.getRequestHeader(
                "Content-Type"
            )
        } returns xmlContentTypeHeader

        resolveBackgroundTask()

        presenter.init(
            viewModel,
            REQUEST_MODE
        )

        verify(exactly = 1) {
            viewModel.init(
                any()
            )
        }

        verify(exactly = 1) {
            httpCallBodyView
                .onFormattingDone()
        }
    }

    @Test
    fun shouldReturnBoundsToHighlight() {

        every {
            responseFormatter.format(
                any()
            )
        } returns "ABC0124abc"

        every {
            httpCallRecord.getRequestHeader(
                "Content-Type"
            )
        } returns jsonContentTypeHeader

        resolveBackgroundTask()

        presenter.init(
            viewModel,
            REQUEST_MODE
        )

        presenter.searchInBody(
            "abc"
        )

        verify(exactly = 1) {
            httpCallBodyView
                .removeOldHighlightedSpans()
        }

        verify {
            httpCallBodyView.highlightBounds(
                match { item ->

                    val firstBound =
                        item[0]

                    assertThat(
                        firstBound.left,
                        `is`(0)
                    )

                    assertThat(
                        firstBound.right,
                        `is`(3)
                    )

                    val secondBound =
                        item[1]

                    assertThat(
                        secondBound.left,
                        `is`(7)
                    )

                    assertThat(
                        secondBound.right,
                        `is`(10)
                    )

                    true
                }
            )
        }
    }

    @Test
    fun shouldNotHighlightSpansWhenPatternIsEmpty() {

        every {
            responseFormatter.format(
                any()
            )
        } returns "ABC0124abc"

        every {
            httpCallRecord.getRequestHeader(
                "Content-Type"
            )
        } returns jsonContentTypeHeader

        resolveBackgroundTask()

        presenter.init(
            viewModel,
            REQUEST_MODE
        )

        presenter.searchInBody(
            ""
        )

        verify(exactly = 1) {
            httpCallBodyView
                .removeOldHighlightedSpans()
        }

        verify(exactly = 0) {
            httpCallBodyView
                .highlightBounds(any())
        }
    }

    @Test
    fun shouldNotHighlightSpansWhenPatternNotFound() {

        every {
            responseFormatter.format(
                any()
            )
        } returns "ABC0124abc"

        every {
            httpCallRecord.getRequestHeader(
                "Content-Type"
            )
        } returns jsonContentTypeHeader

        resolveBackgroundTask()

        presenter.init(
            viewModel,
            REQUEST_MODE
        )

        presenter.searchInBody(
            "789"
        )

        verify(exactly = 1) {
            httpCallBodyView
                .removeOldHighlightedSpans()
        }

        verify(exactly = 0) {
            httpCallBodyView
                .highlightBounds(any())
        }
    }

    @Test
    fun shouldHandleNullBodyAsEmptyString() {

        every {
            httpCallRecord.responseBody
        } returns null

        every {
            httpCallRecord.getResponseHeader(
                "Content-Type"
            )
        } returns null

        resolveBackgroundTask()

        presenter.init(
            viewModel,
            RESPONSE_MODE
        )

        verify(exactly = 1) {
            viewModel.init("")
        }
    }

    private fun resolveBackgroundTask() {

        every {
            mockExecutor.execute(
                any<BackgroundTask<String>>()
            )
        } answers {

            val backgroundTask =
                firstArg<
                    BackgroundTask<String>
                >()

            backgroundTask.onResult(
                backgroundTask.onExecute()
            )
        }
    }

    companion object {

        const val HTTP_CALL_ID =
            5L
    }
}
