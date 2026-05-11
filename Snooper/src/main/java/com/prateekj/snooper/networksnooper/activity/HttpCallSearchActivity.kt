package com.prateekj.snooper.networksnooper.activity

import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.prateekj.snooper.R
import com.prateekj.snooper.customviews.DividerItemDecoration
import com.prateekj.snooper.databinding.ActivityHttpCallSearchBinding
import com.prateekj.snooper.infra.BackgroundTaskExecutor
import com.prateekj.snooper.networksnooper.activity.HttpCallActivity.Companion.HTTP_CALL_ID
import com.prateekj.snooper.networksnooper.adapter.HttpCallListAdapter
import com.prateekj.snooper.networksnooper.adapter.HttpCallListClickListener
import com.prateekj.snooper.networksnooper.database.SnooperRepo
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.networksnooper.presenter.HttpClassSearchPresenter
import com.prateekj.snooper.networksnooper.views.HttpCallSearchView

class HttpCallSearchActivity :
    SnooperBaseActivity(),
    HttpCallSearchView,
    HttpCallListClickListener,
    SearchView.OnQueryTextListener {

    private lateinit var binding:
            ActivityHttpCallSearchBinding

    private lateinit var httpCallListAdapter:
            HttpCallListAdapter

    private lateinit var httpClassSearchPresenter:
            HttpClassSearchPresenter

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityHttpCallSearchBinding.inflate(
                layoutInflater
            )

        setContentView(
            binding.root
        )

        setSupportActionBar(
            binding.toolbar
        )

        supportActionBar
            ?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()

        setupSearchView()

        httpClassSearchPresenter =
            HttpClassSearchPresenter(
                SnooperRepo(this),
                this,
                BackgroundTaskExecutor(this)
            )
    }

    private fun setupRecyclerView() {

        binding.list.apply {

            layoutManager =
                LinearLayoutManager(
                    this@HttpCallSearchActivity
                )

            itemAnimator =
                DefaultItemAnimator()

            addItemDecoration(
                DividerItemDecoration(
                    this@HttpCallSearchActivity,
                    DividerItemDecoration.VERTICAL,
                    R.drawable.grey_divider
                )
            )
        }

        httpCallListAdapter =
            HttpCallListAdapter(
                mutableListOf(),
                this
            )

        binding.list.adapter =
            httpCallListAdapter
    }

    private fun setupSearchView() {

        binding.searchView.apply {

            isIconified = false

            setOnQueryTextListener(
                this@HttpCallSearchActivity
            )
        }
    }

    override fun showResults(
        httpCallRecords: List<HttpCallRecord>
    ) {

        binding.embeddedLoader.visibility =
            GONE

        binding.noResultsFoundContainer.visibility =
            GONE

        binding.list.visibility =
            VISIBLE

        httpCallListAdapter.refreshData(
            httpCallRecords.toMutableList()
        )

        binding.list.post {

            httpCallListAdapter.notifyDataSetChanged()
        }
    }

    override fun showNoResultsFoundMessage(
        keyword: String
    ) {

        binding.embeddedLoader.visibility =
            GONE

        binding.list.visibility =
            GONE

        binding.noResultsFound.text =
            getString(
                R.string.no_results_found,
                keyword
            )

        binding.noResultsFoundContainer.visibility =
            VISIBLE
    }

    override fun hideSearchResultsView() {

        binding.embeddedLoader.visibility =
            GONE

        binding.list.visibility =
            GONE

        binding.noResultsFoundContainer.visibility =
            GONE
    }

    override fun showLoader() {

        binding.embeddedLoader.visibility =
            VISIBLE
    }

    override fun hideLoader() {

        binding.embeddedLoader.visibility =
            GONE
    }

    override fun onClick(
        httpCall: HttpCallRecord
    ) {

        val httpCallId =
            httpCall.id

        if (httpCallId <= 0L) {
            return
        }

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

        finish()
    }

    override fun onQueryTextSubmit(
        query: String?
    ): Boolean {

        val keyword =
            query
                ?.trim()
                .orEmpty()

        httpClassSearchPresenter.searchCalls(
            keyword
        )

        clearSearchFocus()

        return true
    }

    override fun onQueryTextChange(
        text: String?
    ): Boolean {

        val keyword =
            text
                ?.trim()
                .orEmpty()

        httpClassSearchPresenter.searchCalls(
            keyword
        )

        return true
    }

    private fun clearSearchFocus() {

        binding.searchView.clearFocus()
    }
}
