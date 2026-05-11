package com.prateekj.snooper.dbreader.activity

import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.prateekj.snooper.databinding.ActivityDbReaderBinding
import com.prateekj.snooper.dbreader.DatabaseDataReader
import com.prateekj.snooper.dbreader.DatabaseReader
import com.prateekj.snooper.dbreader.adapter.DatabaseAdapter
import com.prateekj.snooper.dbreader.adapter.DbEventListener
import com.prateekj.snooper.dbreader.model.Database
import com.prateekj.snooper.dbreader.view.DbReaderCallback
import com.prateekj.snooper.infra.BackgroundTaskExecutor
import com.prateekj.snooper.networksnooper.activity.SnooperBaseActivity

class DatabaseListActivity :
    SnooperBaseActivity(),
    DbReaderCallback,
    DbEventListener {

    private lateinit var binding:
            ActivityDbReaderBinding

    private lateinit var adapter:
            DatabaseAdapter

    private lateinit var databaseReader:
            DatabaseReader

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityDbReaderBinding.inflate(
                layoutInflater
            )

        setContentView(
            binding.root
        )

        initViews()

        val backgroundTaskExecutor =
            BackgroundTaskExecutor(this)

        databaseReader =
            DatabaseReader(
                this,
                backgroundTaskExecutor,
                DatabaseDataReader()
            )

        databaseReader.fetchApplicationDatabases(
            this
        )
    }

    override fun onDbFetchStarted() {

        binding.embeddedLoader.visibility =
            VISIBLE
    }

    override fun onApplicationDbFetchCompleted(
        databases: List<Database>
    ) {

        binding.embeddedLoader.visibility =
            GONE

        adapter =
            DatabaseAdapter(
                databases,
                this
            )

        val layoutManager =
            LinearLayoutManager(this)

        binding.dbList.apply {

            this.layoutManager =
                layoutManager

            itemAnimator =
                DefaultItemAnimator()

            adapter =
                this@DatabaseListActivity.adapter
        }
    }

    override fun onDatabaseClick(
        db: Database
    ) {

        val dbViewActivity =
            Intent(
                this@DatabaseListActivity,
                DatabaseDetailActivity::class.java
            ).apply {

                putExtra(
                    DB_PATH,
                    db.path
                )

                putExtra(
                    DB_NAME,
                    db.name
                )
            }

        startActivity(
            dbViewActivity
        )
    }

    private fun initViews() {

        setSupportActionBar(
            binding.toolbar
        )

        supportActionBar
            ?.setDisplayHomeAsUpEnabled(false)
    }

    companion object {

        const val DB_PATH =
            "DB_PATH"

        const val DB_NAME =
            "DB_NAME"
    }
}
