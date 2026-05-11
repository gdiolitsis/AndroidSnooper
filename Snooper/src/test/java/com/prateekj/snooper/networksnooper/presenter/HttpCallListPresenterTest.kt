package com.prateekj.snooper.networksnooper.presenter

import com.prateekj.snooper.networksnooper.database.SnooperRepo
import com.prateekj.snooper.networksnooper.model.HttpCall
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.networksnooper.views.HttpListView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class HttpCallListPresenterTest {

    private lateinit var view:
            HttpListView

    private lateinit var repo:
            SnooperRepo

    private lateinit var httpCallListPresenter:
            HttpCallListPresenter

    @Before
    fun setUp() {

        view =
            mockk(relaxed = true)

        repo =
            mockk(relaxed = true)

        httpCallListPresenter =
            HttpCallListPresenter(
                view,
                repo
            )
    }

    @Test
    fun shouldInitializeHttpCallRecordsList() {

        val httpCallRecords =
            mutableListOf(
                mockk<HttpCallRecord>(
                    relaxed = true
                )
            )

        every {
            repo.findAllSortByDateAfter(
                -1,
                20
            )
        } returns httpCallRecords

        httpCallListPresenter.init()

        verify(exactly = 1) {
            view.initHttpCallRecordList(
                httpCallRecords
            )
        }
    }

    @Test
    fun shouldShowNoCallsFoundMessage() {

        every {
            repo.findAllSortByDateAfter(
                -1,
                20
            )
        } returns mutableListOf()

        httpCallListPresenter.init()

        verify(exactly = 1) {
            view.renderNoCallsFoundView()
        }

        verify(exactly = 0) {
            view.initHttpCallRecordList(
                any()
            )
        }
    }

    @Test
    fun shouldQueryNextSetOfRecordOnNextPageCall() {

        val lastSetHttpCalls =
            mutableListOf(
                createCallWithId(1)
            )

        val nextSetHttpCalls =
            mutableListOf(
                createCallWithId(3),
                createCallWithId(2)
            )

        every {
            repo.findAllSortByDateAfter(
                -1,
                20
            )
        } returns mutableListOf(
            createCallWithId(5),
            createCallWithId(4)
        )

        httpCallListPresenter.init()

        every {
            repo.findAllSortByDateAfter(
                4,
                20
            )
        } returns nextSetHttpCalls

        httpCallListPresenter.onNextPageCall()

        verify(exactly = 1) {
            repo.findAllSortByDateAfter(
                4,
                20
            )
        }

        verify(exactly = 1) {
            view.appendRecordList(
                nextSetHttpCalls
            )
        }

        every {
            repo.findAllSortByDateAfter(
                2,
                20
            )
        } returns lastSetHttpCalls

        httpCallListPresenter.onNextPageCall()

        verify(exactly = 1) {
            repo.findAllSortByDateAfter(
                2,
                20
            )
        }

        verify(exactly = 1) {
            view.appendRecordList(
                lastSetHttpCalls
            )
        }

        every {
            repo.findAllSortByDateAfter(
                1,
                20
            )
        } returns mutableListOf()

        httpCallListPresenter.onNextPageCall()

        verify(exactly = 1) {
            repo.findAllSortByDateAfter(
                1,
                20
            )
        }

        verify(exactly = 1) {
            view.appendRecordList(
                match {
                    it.isEmpty()
                }
            )
        }
    }

    @Test
    fun shouldNotifyViewToNavigateToResponseBody() {

        val httpCall =
            mockk<HttpCallRecord>(
                relaxed = true
            )

        every {
            httpCall.id
        } returns 2L

        httpCallListPresenter.onClick(
            httpCall
        )

        verify(exactly = 1) {
            view.navigateToResponseBody(
                2L
            )
        }
    }

    @Test
    fun shouldNotifyViewToFinishIt() {

        httpCallListPresenter.onDoneClick()

        verify(exactly = 1) {
            view.finishView()
        }
    }

    @Test
    fun shouldDeleteTheRecordsAndUpdateUi() {

        httpCallListPresenter
            .confirmDeleteRecords()

        verify(exactly = 1) {
            repo.deleteAll()
        }

        verify(exactly = 1) {
            view.updateListViewAfterDelete()
        }
    }

    @Test
    fun shouldShowConfirmationDialogOnClickOfDeleteRecords() {

        httpCallListPresenter
            .onDeleteRecordsClicked()

        verify(exactly = 1) {
            view.showDeleteConfirmationDialog()
        }
    }

    @Test
    fun shouldAppendEmptyListWhenNoMorePagesExist() {

        every {
            repo.findAllSortByDateAfter(
                -1,
                20
            )
        } returns mutableListOf(
            createCallWithId(1)
        )

        httpCallListPresenter.init()

        every {
            repo.findAllSortByDateAfter(
                1,
                20
            )
        } returns mutableListOf()

        httpCallListPresenter.onNextPageCall()

        verify(exactly = 1) {
            view.appendRecordList(
                emptyList()
            )
        }
    }

    private fun createCallWithId(
        id: Int
    ): HttpCallRecord {

        return HttpCallRecord.from(
            HttpCall.Builder()
                .build()
        ).apply {

            this.id =
                id.toLong()
        }
    }
}
