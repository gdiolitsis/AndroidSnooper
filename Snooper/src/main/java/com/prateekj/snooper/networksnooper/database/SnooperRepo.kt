package com.prateekj.snooper.networksnooper.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.prateekj.snooper.database.SnooperDbHelper
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_DATE
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_ERROR
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_HEADER_ID
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_HEADER_NAME
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_HEADER_TYPE
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_HEADER_VALUE
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_HTTP_CALL_RECORD_ID
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_METHOD
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_PAYLOAD
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_RESPONSE_BODY
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_STATUSCODE
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_STATUSTEXT
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_URL
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.HEADER_TABLE_NAME
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.HEADER_VALUE_TABLE_NAME
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.HTTP_CALL_RECORD_GET_BY_ID
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.HTTP_CALL_RECORD_GET_NEXT_SORT_BY_DATE_WITH_SIZE
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.HTTP_CALL_RECORD_GET_SORT_BY_DATE
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.HTTP_CALL_RECORD_GET_SORT_BY_DATE_WITH_SIZE
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.HTTP_CALL_RECORD_SEARCH
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.HTTP_CALL_RECORD_TABLE_NAME
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.HTTP_HEADER_GET_BY_CALL_ID
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.HTTP_HEADER_VALUE_GET_BY_HEADER_ID
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.networksnooper.model.HttpHeader
import com.prateekj.snooper.networksnooper.model.HttpHeaderValue

class SnooperRepo(
    context: Context
) {

    private val dbWriteHelper =
        SnooperDbHelper.create(context)

    private val dbReadHelper =
        SnooperDbHelper.create(context)

    fun save(
        httpCallRecord: HttpCallRecord
    ): Long {

        val database =
            dbWriteHelper.writableDatabase

        return try {

            database.beginTransaction()

            val values =
                ContentValues().apply {

                    put(
                        COLUMN_URL,
                        httpCallRecord.url
                    )

                    put(
                        COLUMN_PAYLOAD,
                        httpCallRecord.payload
                    )

                    put(
                        COLUMN_RESPONSE_BODY,
                        httpCallRecord.responseBody
                    )

                    put(
                        COLUMN_METHOD,
                        httpCallRecord.method
                    )

                    put(
                        COLUMN_STATUSCODE,
                        httpCallRecord.statusCode
                    )

                    put(
                        COLUMN_STATUSTEXT,
                        httpCallRecord.statusText
                    )

                    put(
                        COLUMN_DATE,
                        httpCallRecord.date?.time
                    )

                    put(
                        COLUMN_ERROR,
                        httpCallRecord.error
                    )
                }

            val httpCallRecordId =
                database.insertOrThrow(
                    HTTP_CALL_RECORD_TABLE_NAME,
                    null,
                    values
                )

            saveHeaders(
                database,
                httpCallRecordId,
                httpCallRecord.requestHeaders
                    ?: emptyList(),
                REQUEST_HEADER_TYPE
            )

            saveHeaders(
                database,
                httpCallRecordId,
                httpCallRecord.responseHeaders
                    ?: emptyList(),
                RESPONSE_HEADER_TYPE
            )

            database.setTransactionSuccessful()

            httpCallRecordId

        } finally {

            database.endTransaction()

            database.close()
        }
    }

    fun findAllSortByDate():
            List<HttpCallRecord> {

        val database =
            dbReadHelper.readableDatabase

        return try {

            val records =
                mutableListOf<HttpCallRecord>()

            val parser =
                HttpCallRecordCursorParser()

            database.rawQuery(
                HTTP_CALL_RECORD_GET_SORT_BY_DATE,
                null
            ).use { cursor ->

                while (cursor.moveToNext()) {

                    records.add(
                        parser.parse(cursor)
                    )
                }
            }

            records

        } finally {

            database.close()
        }
    }

    fun searchHttpRecord(
        text: String
    ): List<HttpCallRecord> {

        val database =
            dbReadHelper.readableDatabase

        return try {

            val records =
                mutableListOf<HttpCallRecord>()

            val parser =
                HttpCallRecordCursorParser()

            val likeText =
                "%$text%"

            database.rawQuery(
                HTTP_CALL_RECORD_SEARCH,
                arrayOf(
                    likeText,
                    likeText,
                    likeText,
                    likeText
                )
            ).use { cursor ->

                while (cursor.moveToNext()) {

                    records.add(
                        parser.parse(cursor)
                    )
                }
            }

            records

        } finally {

            database.close()
        }
    }

    fun findAllSortByDateAfter(
        id: Long,
        pageSize: Int
    ): MutableList<HttpCallRecord> {

        val database =
            dbReadHelper.readableDatabase

        return try {

            val records =
                mutableListOf<HttpCallRecord>()

            val parser =
                HttpCallRecordCursorParser()

            val cursor: Cursor =
                if (id == -1L) {

                    database.rawQuery(
                        HTTP_CALL_RECORD_GET_SORT_BY_DATE_WITH_SIZE,
                        arrayOf(
                            pageSize.toString()
                        )
                    )

                } else {

                    database.rawQuery(
                        HTTP_CALL_RECORD_GET_NEXT_SORT_BY_DATE_WITH_SIZE,
                        arrayOf(
                            id.toString(),
                            pageSize.toString()
                        )
                    )
                }

            cursor.use {

                while (it.moveToNext()) {

                    records.add(
                        parser.parse(it)
                    )
                }
            }

            records

        } finally {

            database.close()
        }
    }

    fun findById(
        id: Long
    ): HttpCallRecord {

        val database =
            dbReadHelper.readableDatabase

        return try {

            val parser =
                HttpCallRecordCursorParser()

            var record:
                    HttpCallRecord? = null

            database.rawQuery(
                HTTP_CALL_RECORD_GET_BY_ID,
                arrayOf(id.toString())
            ).use { cursor ->

                if (cursor.moveToFirst()) {

                    record =
                        parser.parse(cursor).apply {

                            requestHeaders =
                                findHeader(
                                    database,
                                    this.id,
                                    REQUEST_HEADER_TYPE
                                )

                            responseHeaders =
                                findHeader(
                                    database,
                                    this.id,
                                    RESPONSE_HEADER_TYPE
                                )
                        }
                }
            }

            requireNotNull(record) {
                "No HttpCallRecord found for id=$id"
            }

        } finally {

            database.close()
        }
    }

    fun deleteAll() {

        val database =
            dbWriteHelper.writableDatabase

        try {

            database.beginTransaction()

            database.delete(
                HTTP_CALL_RECORD_TABLE_NAME,
                null,
                null
            )

            database.setTransactionSuccessful()

        } finally {

            database.endTransaction()

            database.close()
        }
    }

    private fun findHeader(
        database: SQLiteDatabase,
        callId: Long,
        headerType: String
    ): List<HttpHeader> {

        val headers =
            mutableListOf<HttpHeader>()

        val parser =
            HttpHeaderCursorParser()

        database.rawQuery(
            HTTP_HEADER_GET_BY_CALL_ID,
            arrayOf(
                callId.toString(),
                headerType
            )
        ).use { cursor ->

            while (cursor.moveToNext()) {

                val header =
                    parser.parse(cursor)

                header.values =
                    findHeaderValue(
                        database,
                        header.id
                    )

                headers.add(header)
            }
        }

        return headers
    }

    private fun findHeaderValue(
        database: SQLiteDatabase,
        headerId: Int
    ): List<HttpHeaderValue> {

        val values =
            mutableListOf<HttpHeaderValue>()

        val parser =
            HttpHeaderValueCursorParser()

        database.rawQuery(
            HTTP_HEADER_VALUE_GET_BY_HEADER_ID,
            arrayOf(
                headerId.toString()
            )
        ).use { cursor ->

            while (cursor.moveToNext()) {

                values.add(
                    parser.parse(cursor)
                )
            }
        }

        return values
    }

    private fun saveHeaders(
        database: SQLiteDatabase,
        httpCallRecordId: Long,
        headers: List<HttpHeader>,
        headerType: String
    ) {

        headers.forEach { header ->

            saveHeader(
                database,
                header,
                httpCallRecordId,
                headerType
            )
        }
    }

    private fun saveHeader(
        database: SQLiteDatabase,
        httpHeader: HttpHeader,
        httpCallRecordId: Long,
        headerType: String
    ) {

        val values =
            ContentValues().apply {

                put(
                    COLUMN_HEADER_NAME,
                    httpHeader.name
                )

                put(
                    COLUMN_HEADER_TYPE,
                    headerType
                )

                put(
                    COLUMN_HTTP_CALL_RECORD_ID,
                    httpCallRecordId
                )
            }

        val headerId =
            database.insertOrThrow(
                HEADER_TABLE_NAME,
                null,
                values
            )

        httpHeader.values.forEach {

            saveHeaderValue(
                database,
                it,
                headerId
            )
        }
    }

    private fun saveHeaderValue(
        database: SQLiteDatabase,
        value: HttpHeaderValue,
        headerId: Long
    ) {

        val values =
            ContentValues().apply {

                put(
                    COLUMN_HEADER_VALUE,
                    value.value
                )

                put(
                    COLUMN_HEADER_ID,
                    headerId
                )
            }

        database.insertOrThrow(
            HEADER_VALUE_TABLE_NAME,
            null,
            values
        )
    }

    companion object {

        private const val REQUEST_HEADER_TYPE =
            "req"

        private const val RESPONSE_HEADER_TYPE =
            "res"
    }
}
