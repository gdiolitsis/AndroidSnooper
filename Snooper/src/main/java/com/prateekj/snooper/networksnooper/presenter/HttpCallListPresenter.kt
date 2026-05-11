package com.prateekj.snooper.networksnooper.presenter

import com.prateekj.snooper.networksnooper.adapter.HttpCallListClickListener
import com.prateekj.snooper.networksnooper.database.SnooperRepo
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.networksnooper.views.HttpListView

class HttpCallListPresenter(
    private val httpListView:
            HttpListView,
    private val snooperRepo:
            SnooperRepo
) : HttpCallListClickListener {

    private var lastCallId =
        -1L

    fun init() {

        val httpCallRecords =
            snooperRepo.findAllSortByDateAfter(
                -1,
                PAGE_SIZE
            )

        if (
            httpCallRecords.isEmpty()
        ) {

            httpListView
                .renderNoCallsFoundView()

            return
        }

        updateLastCallId(
            httpCallRecords
        )

        httpListView
            .initHttpCallRecordList(
                httpCallRecords
            )
    }

    fun onNextPageCall() {

        if (lastCallId < 0L) {
            return
        }

        val httpCallRecords =
            snooperRepo.findAllSortByDateAfter(
                lastCallId,
                PAGE_SIZE
            )

        updateLastCallId(
            httpCallRecords
        )

        httpListView
            .appendRecordList(
                httpCallRecords
            )
    }

    override fun onClick(
        httpCall: HttpCallRecord
    ) {

        if (httpCall.id <= 0L) {
            return
        }

        httpListView
            .navigateToResponseBody(
                httpCall.id
            )
    }

    fun onDoneClick() {

        httpListView
            .finishView()
    }

    fun onDeleteRecordsClicked() {

        httpListView
            .showDeleteConfirmationDialog()
    }

    fun confirmDeleteRecords() {

        snooperRepo.deleteAll()

        lastCallId = -1L

        httpListView
            .updateListViewAfterDelete()
    }

    private fun updateLastCallId(
        httpCallRecords:
                List<HttpCallRecord>
    ) {

        if (
            httpCallRecords.isNotEmpty()
        ) {

            lastCallId =
                httpCallRecords.last().id
        }
    }

    companion object {

        const val PAGE_SIZE =
            20
    }
}
