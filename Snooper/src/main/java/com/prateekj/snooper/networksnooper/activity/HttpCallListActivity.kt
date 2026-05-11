package com.prateekj.snooper.networksnooper.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.prateekj.snooper.R
import com.prateekj.snooper.customviews.DividerItemDecoration
import com.prateekj.snooper.customviews.NextPageRequestListener
import com.prateekj.snooper.databinding.ActivityHttpCallListBinding
import com.prateekj.snooper.networksnooper.activity.HttpCallActivity.Companion.HTTP_CALL_ID
import com.prateekj.snooper.networksnooper.adapter.HttpCallListAdapter
import com.prateekj.snooper.networksnooper.database.SnooperRepo
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.networksnooper.presenter.HttpCallListPresenter
import com.prateekj.snooper.networksnooper.presenter.HttpCallListPresenter.Companion.PAGE_SIZE
import com.prateekj.snooper.networksnooper.views.HttpListView

class HttpCallListActivity :
    SnooperBaseActivity(),
    HttpListView,
    NextPageRequestListener {

    private lateinit var binding:
            ActivityHttpCallListBinding

    private lateinit var presenter:
            HttpCallListPresenter

    private lateinit var httpCallListAdapter:
            HttpCallListAdapter

    private lateinit var repo:
            SnooperRepo

    private var allPagesLoaded =
        false

    private var noCallsFound =
        false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityHttpCallListBinding.inflate(
                layoutInflater
            )

        setContentView(
            binding.root
        )

        setSupportActionBar(
            binding.toolbar
        )

        repo =
            SnooperRepo(this)

        presenter =
            HttpCallListPresenter(
                this,
                repo
            )

        setupRecyclerView()

        presenter.init()
    }

    private fun setupRecyclerView() {

        binding.list.layoutManager =
            LinearLayoutManager(this)

        binding.list.itemAnimator =
            DefaultItemAnimator()

        binding.list.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL,
                R.drawable.grey_divider
            )
        )

        binding.list.setNextPageListener(this)
    }

    override fun onCreateOptionsMenu(
        menu: Menu
    ): Boolean {

        menuInflater.inflate(
            R.menu.http_call_list_menu,
            menu
        )

        menu.findItem(
            R.id.delete_records_menu
        )?.isVisible = !noCallsFound

        return true
    }

    override fun onOptionsItemSelected(
        item: MenuItem
    ): Boolean {

        return when (item.itemId) {

            R.id.done_menu -> {

                presenter.onDoneClick()

                true
            }

            R.id.delete_records_menu -> {

                presenter.onDeleteRecordsClicked()

                true
            }

            R.id.search -> {

                openSearchActivity()

                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun openSearchActivity() {

        startActivity(
            Intent(
                this,
                HttpCallSearchActivity::class.java
            )
        )
    }

    override fun navigateToResponseBody(
        httpCallId: Long
    ) {

        val intent =
            Intent(
                this,
                HttpCallActivity::class.java
            ).apply {

                putExtra(
                    HTTP_CALL_ID,
                    httpCallId
                )
            }

        startActivity(intent)

        overridePendingTransition(
            R.anim.in_from_right,
            R.anim.out_to_left
        )
    }

    override fun finishView() {

        finish()
    }

    override fun showDeleteConfirmationDialog() {

        val dialogClickListener =
            DialogInterface.OnClickListener { dialog, which ->

                when (which) {

                    DialogInterface.BUTTON_POSITIVE -> {

                        presenter.confirmDeleteRecords()
                    }

                    DialogInterface.BUTTON_NEGATIVE -> {

                        dialog.dismiss()
                    }
                }
            }

        AlertDialog.Builder(this)
            .setMessage(
                R.string.delete_records_dialog_text
            )
            .setPositiveButton(
                getString(
                    R.string.delete_records_dialog_confirmation
                ),
                dialogClickListener
            )
            .setNegativeButton(
                getString(
                    R.string.delete_records_dialog_cancellation
                ),
                dialogClickListener
            )
            .show()
    }

    override fun updateListViewAfterDelete() {

        val freshData =
            repo.findAllSortByDateAfter(
                -1,
                PAGE_SIZE
            )

        httpCallListAdapter.refreshData(
            freshData
        )

        httpCallListAdapter.notifyDataSetChanged()

        allPagesLoaded =
            freshData.size < PAGE_SIZE
    }

    override fun initHttpCallRecordList(
        httpCallRecords: List<HttpCallRecord>
    ) {

        httpCallListAdapter =
            HttpCallListAdapter(
                httpCallRecords.toMutableList(),
                presenter
            )

        checkIfAllPagesAreLoaded(
            httpCallRecords
        )

        binding.list.adapter =
            httpCallListAdapter
    }

    override fun appendRecordList(
        httpCallRecords: List<HttpCallRecord>
    ) {

        if (httpCallRecords.isEmpty()) {

            allPagesLoaded = true

            return
        }

        httpCallListAdapter.appendData(
            httpCallRecords
        )

        checkIfAllPagesAreLoaded(
            httpCallRecords
        )

        binding.list.post {

            httpCallListAdapter.notifyDataSetChanged()
        }
    }

    override fun renderNoCallsFoundView() {

        noCallsFound = true

        invalidateOptionsMenu()

        binding.httpCallListContainer.visibility =
            GONE

        binding.noCallsFoundContainer.visibility =
            VISIBLE
    }

    override fun requestNextPage() {

        presenter.onNextPageCall()
    }

    override fun areAllPagesLoaded():
            Boolean {

        return allPagesLoaded
    }

    private fun checkIfAllPagesAreLoaded(
        httpCallRecords: List<HttpCallRecord>
    ) {

        allPagesLoaded =
            httpCallRecords.size < PAGE_SIZE
    }
}
