package com.prateekj.snooper.dbreader

import android.database.sqlite.SQLiteDatabase
import com.prateekj.snooper.dbreader.model.Database
import com.prateekj.snooper.dbreader.model.Row
import com.prateekj.snooper.dbreader.model.Table

class DatabaseDataReader {

    fun getData(
        database: SQLiteDatabase
    ): Database {

        val databaseData =
            Database().apply {

                path = database.path
                version = database.version
            }

        val tableNames =
            mutableListOf<String>()

        database.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table'",
            null
        ).use { cursor ->

            if (cursor.moveToFirst()) {

                val nameColumnIndex =
                    cursor.getColumnIndexOrThrow(
                        "name"
                    )

                do {

                    tableNames.add(
                        cursor.getString(
                            nameColumnIndex
                        )
                    )

                } while (cursor.moveToNext())
            }
        }

        databaseData.tables =
            tableNames

        return databaseData
    }

    fun getTableData(
        database: SQLiteDatabase,
        tableName: String
    ): Table {

        val table =
            Table()

        val rows =
            mutableListOf<Row>()

        database.rawQuery(
            "SELECT * FROM $tableName",
            null
        ).use { cursor ->

            table.columns =
                cursor.columnNames.toList()

            if (cursor.moveToFirst()) {

                val columnCount =
                    cursor.columnCount

                do {

                    val data =
                        mutableListOf<String>()

                    for (i in 0 until columnCount) {

                        data.add(
                            cursor.getString(i)
                                ?: ""
                        )
                    }

                    rows.add(
                        Row(data)
                    )

                } while (cursor.moveToNext())
            }
        }

        table.name =
            tableName

        table.rows =
            rows

        return table
    }
}
