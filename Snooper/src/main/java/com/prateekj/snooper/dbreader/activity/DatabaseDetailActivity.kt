package com.prateekj.snooper.dbreader.activity

import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.prateekj.snooper.databinding.ActivityDbViewBinding
import com.prateekj.snooper.dbreader.DatabaseDataReader
import com.prateekj.snooper.dbreader.DatabaseReader
import com.prateekj.snooper.dbreader.activity.DatabaseListActivity.Companion.DB_NAME
import com.prateekj.snooper.dbreader.adapter.TableAdapter
import com.prateekj.snooper.dbreader.adapter.TableEventListener
import com.prateekj.snooper.dbreader.model.Database
import com.prateekj.snooper.dbreader.view.DbViewCallback
import com.prateekj.snooper.infra.BackgroundTaskExecutor
import com.prateekj.snooper.networksnooper.activity.SnooperBaseActivity

class DatabaseDetailActivity :
    SnooperBaseActivity(),
    DbViewCallback,
    TableEventListener {

    private lateinit var binding: ActivityDbViewBinding

    private lateinit var databaseReader: DatabaseReader

    private var dbPath: String =
        ""

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityDbViewBinding.inflate(layoutInflater)

        setContentView(
            binding.root
        )

        initViews()

        dbPath =
            intent.getStringExtra(
                DatabaseListActivity.DB_PATH
            ) ?: ""

        val dbName =
            intent.getStringExtra(
                DB_NAME
            ) ?: ""

        if (dbPath.isBlank()) {

            finish()

            return
        }

        val backgroundTaskExecutor =
            BackgroundTaskExecutor(this)

        databaseReader =
            DatabaseReader(
                this,
                backgroundTaskExecutor,
                DatabaseDataReader()
            )

        databaseReader.fetchDbContent(
            this,
            dbPath,
            dbName
        )
    }

    override fun onDbFetchStarted() {

        binding.embeddedLoader.visibility =
            VISIBLE
    }

    override fun onDbFetchCompleted(
        databases: Database
    ) {

        binding.embeddedLoader.visibility =
            GONE

        updateDbView(databases)
    }

    private fun updateDbView(
        database: Database
    ) {

        binding.dbName.text =
            database.name ?: ""

        binding.dbVersion.text =
            database.version.toString()

        updateTableList(
            database.tables ?: emptyList()
        )
    }

    private fun updateTableList(
        tables: List<String>
    ) {

        val tableAdapter =
            TableAdapter(
                tables,
                this
            )

        val layoutManager =
            LinearLayoutManager(this)

        binding.tableList.apply {

            this.layoutManager =
                layoutManager

            itemAnimator =
                DefaultItemAnimator()

            adapter =
                tableAdapter
        }
    }

    private fun initViews() {

        setSupportActionBar(binding.toolbar)

        supportActionBar
            ?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onTableClick(
        table: String
    ) {

        val dbViewActivity =
            Intent(
                this,
                TableDetailActivity::class.java
            ).apply {

                putExtra(
                    TABLE_NAME,
                    table
                )

                putExtra(
                    DB_PATH,
                    dbPath
                )
            }

        startActivity(dbViewActivity)
    }

    companion object {

        const val TABLE_NAME =
            "TABLE_NAME"

        const val DB_PATH =
            "DB_PATH"
    }
}
