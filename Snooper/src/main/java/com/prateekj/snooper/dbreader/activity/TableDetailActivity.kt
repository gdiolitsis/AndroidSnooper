package com.prateekj.snooper.dbreader.activity

import android.graphics.Typeface
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getDrawable
import com.prateekj.snooper.R
import com.prateekj.snooper.databinding.ActivityTableViewBinding
import com.prateekj.snooper.dbreader.DatabaseDataReader
import com.prateekj.snooper.dbreader.DatabaseReader
import com.prateekj.snooper.dbreader.activity.DatabaseDetailActivity.Companion.TABLE_NAME
import com.prateekj.snooper.dbreader.model.Table
import com.prateekj.snooper.dbreader.view.TableViewCallback
import com.prateekj.snooper.infra.BackgroundTaskExecutor
import com.prateekj.snooper.networksnooper.activity.SnooperBaseActivity

class TableDetailActivity :
    SnooperBaseActivity(),
    TableViewCallback {

    private lateinit var binding:
            ActivityTableViewBinding

    private lateinit var databaseReader:
            DatabaseReader

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityTableViewBinding.inflate(
                layoutInflater
            )

        setContentView(
            binding.root
        )

        initViews()

        val tableName =
            intent.getStringExtra(
                TABLE_NAME
            )

        val dbPath =
            intent.getStringExtra(
                DatabaseDetailActivity.DB_PATH
            )

        if (
            tableName.isNullOrBlank() ||
            dbPath.isNullOrBlank()
        ) {

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

        databaseReader.fetchTableContent(
            this,
            dbPath,
            tableName
        )
    }

    override fun onTableFetchStarted() {

        binding.embeddedLoader.visibility =
            VISIBLE
    }

    override fun onTableFetchCompleted(
        table: Table
    ) {

        binding.embeddedLoader.visibility =
            GONE

        updateView(table)
    }

    private fun initViews() {

        setSupportActionBar(
            binding.toolbar
        )

        supportActionBar
            ?.setDisplayHomeAsUpEnabled(true)
    }

    private fun updateView(
        table: Table
    ) {

        addTableColumnNames(table)

        addTableRowsToUi(table)
    }

    private fun addTableRowsToUi(
        table: Table
    ) {

        val rows =
            table.rows ?: emptyList()

        for (i in rows.indices) {

            binding.tableLayout.addView(
                addRowData(
                    rows[i].data,
                    i + 1
                )
            )
        }
    }

    private fun addTableColumnNames(
        table: Table
    ) {

        val columnRow =
            TableRow(this)

        val serialNoCell =
            getCellView(
                getString(
                    R.string.serial_number_column_heading
                )
            )

        serialNoCell.setTypeface(
            null,
            Typeface.BOLD
        )

        columnRow.addView(
            serialNoCell
        )

        val columns =
            table.columns ?: emptyList()

        for (column in columns) {

            val columnView =
                getCellView(column).apply {

                    setBackgroundColor(
                        ContextCompat.getColor(
                            this@TableDetailActivity,
                            R.color.snooper_grey
                        )
                    )

                    background =
                        getDrawable(
                            this@TableDetailActivity,
                            R.drawable.table_cell_background
                        )

                    setTypeface(
                        null,
                        Typeface.BOLD
                    )
                }

            columnRow.addView(
                columnView
            )
        }

        binding.tableLayout.addView(
            columnRow
        )
    }

    private fun addRowData(
        data: List<String>,
        serialNumber: Int
    ): TableRow {

        val row =
            TableRow(this)

        row.addView(
            getCellView(
                serialNumber.toString()
            )
        )

        for (cellValue in data) {

            row.addView(
                getCellView(
                    cellValue ?: ""
                )
            )
        }

        return row
    }

    private fun getCellView(
        cellValue: String
    ): TextView {

        return TextView(this).apply {

            setPadding(
                1,
                0,
                0,
                0
            )

            background =
                getDrawable(
                    this@TableDetailActivity,
                    R.drawable.table_cell_background
                )

            text =
                cellValue
        }
    }
}
