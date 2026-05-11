package com.prateekj.snooper.networksnooper.presenter

import com.prateekj.snooper.infra.BackgroundTask
import com.prateekj.snooper.infra.BackgroundTaskExecutor
import com.prateekj.snooper.networksnooper.database.SnooperRepo
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.networksnooper.views.HttpCallSearchView
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class HttpClassSearchPresenterTest {

    private lateinit var mockExecutor:
            BackgroundTaskExecutor

    private lateinit var httpCallSearchView:
            HttpCallSearchView

    private lateinit var repo:
            SnooperRepo

    private lateinit var searchPresenter:
            HttpClassSearchPresenter

    @Before
    fun setUp() {

        mockExecutor =
            mockk(relaxed = true)

        httpCallSearchView =
            mockk(relaxed = true)

        repo =
            mockk(relaxed = true)

        searchPresenter =
            HttpClassSearchPresenter(
                repo,
                httpCallSearchView,
                mockExecutor
            )
    }

    @Test
    fun shouldShowSearchedHttpCallRecordsOnView() {

        val httpCallRecordList =
            listOf(
                mockk<HttpCallRecord>(
                    relaxed = true
                )
            )

        resolveBackgroundTask()

        every {
            repo.searchHttpRecord(
                "url"
            )
        } returns httpCallRecordList

        searchPresenter.searchCalls(
            "url"
        )

        verify(exactly = 1) {
            repo.searchHttpRecord(
                "url"
            )
        }

        verify(exactly = 1) {
            httpCallSearchView
                .hideSearchResultsView()
        }

        verify(exactly = 1) {
            httpCallSearchView
                .showLoader()
        }

        verify(exactly = 1) {
            httpCallSearchView
                .hideLoader()
        }

        verify(exactly = 1) {
            httpCallSearchView
                .showResults(
                    refEq(
                        httpCallRecordList
                    )
                )
        }
    }

    @Test
    fun shouldShowNoResultsFoundMessage() {

        val httpCallRecordList =
            emptyList<HttpCallRecord>()

        resolveBackgroundTask()

        every {
            repo.searchHttpRecord(
                "url"
            )
        } returns httpCallRecordList

        searchPresenter.searchCalls(
            "url"
        )

        verify(exactly = 1) {
            repo.searchHttpRecord(
                "url"
            )
        }

        verify(exactly = 1) {
            httpCallSearchView
                .hideSearchResultsView()
        }

        verify(exactly = 1) {
            httpCallSearchView
                .showLoader()
        }

        verify(exactly = 1) {
            httpCallSearchView
                .hideLoader()
        }

        verify(exactly = 1) {
            httpCallSearchView
                .showNoResultsFoundMessage(
                    "url"
                )
        }
    }

    @Test
    fun shouldShowEmptyResultsWhenTextIsEmpty() {

        searchPresenter.searchCalls(
            ""
        )

        verify(exactly = 0) {
            repo.searchHttpRecord(
                any()
            )
        }

        verify(exactly = 1) {
            httpCallSearchView
                .hideLoader()
        }

        verify(exactly = 1) {
            httpCallSearchView
                .showResults(
                    match {
                        it.isEmpty()
                    }
                )
        }

        confirmVerified(
            httpCallSearchView
        )
    }

    private fun resolveBackgroundTask() {

        every {
            mockExecutor.execute(
                any<
                    BackgroundTask<
                        List<HttpCallRecord>
                    >
                >()
            )
        } answers {

            val backgroundTask =
                firstArg<
                    BackgroundTask<
                        List<HttpCallRecord>
                    >
                >()

            backgroundTask.onResult(
                backgroundTask.onExecute()
            )
        }
    }
}
