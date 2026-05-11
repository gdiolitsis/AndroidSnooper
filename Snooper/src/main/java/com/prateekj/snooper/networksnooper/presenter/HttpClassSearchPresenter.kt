package com.prateekj.snooper.networksnooper.presenter

import com.prateekj.snooper.infra.BackgroundTask
import com.prateekj.snooper.infra.BackgroundTaskExecutor
import com.prateekj.snooper.networksnooper.database.SnooperRepo
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.networksnooper.views.HttpCallSearchView

class HttpClassSearchPresenter(
    private val snooperRepo:
            SnooperRepo,
    private val httpCallSearchView:
            HttpCallSearchView,
    private val taskExecutor:
            BackgroundTaskExecutor
) {

    @Volatile
    private var latestQuery =
        ""

    fun searchCalls(
        text: String
    ) {

        val query =
            text.trim()

        latestQuery =
            query

        if (
            query.isEmpty()
        ) {

            httpCallSearchView
                .hideLoader()

            httpCallSearchView
                .hideSearchResultsView()

            return
        }

        httpCallSearchView
            .hideSearchResultsView()

        httpCallSearchView
            .showLoader()

        taskExecutor.execute(
            searchHttpCallTask(query)
        )
    }

    private fun searchHttpCallTask(
        text: String
    ): BackgroundTask<List<HttpCallRecord>> {

        return object :
            BackgroundTask<List<HttpCallRecord>> {

            override fun onExecute():
                    List<HttpCallRecord> {

                return snooperRepo
                    .searchHttpRecord(
                        text
                    )
            }

            override fun onResult(
                result: List<HttpCallRecord>
            ) {

                // Ignore stale async results
                if (text != latestQuery) {
                    return
                }

                if (
                    result.isEmpty()
                ) {

                    showNoResultsFoundMessage(
                        text
                    )

                    return
                }

                showResults(
                    result
                )
            }
        }
    }

    private fun showNoResultsFoundMessage(
        text: String
    ) {

        httpCallSearchView
            .hideLoader()

        httpCallSearchView
            .showNoResultsFoundMessage(
                text
            )
    }

    private fun showResults(
        result: List<HttpCallRecord>
    ) {

        httpCallSearchView
            .hideLoader()

        httpCallSearchView
            .showResults(
                result
            )
    }
}
