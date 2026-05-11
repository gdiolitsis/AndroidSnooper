package com.prateekj.snooper.networksnooper.database

import android.database.Cursor
import android.provider.BaseColumns._ID
import com.prateekj.snooper.database.CursorParser
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_DATE
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_ERROR
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_METHOD
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_PAYLOAD
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_RESPONSE_BODY
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_STATUSCODE
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_STATUSTEXT
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_URL
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import java.util.Date

class HttpCallRecordCursorParser :
    CursorParser<HttpCallRecord> {

    override fun parse(
        cursor: Cursor
    ): HttpCallRecord {

        val idIndex =
            cursor.getColumnIndexOrThrow(_ID)

        val urlIndex =
            cursor.getColumnIndexOrThrow(
                COLUMN_URL
            )

        val payloadIndex =
            cursor.getColumnIndexOrThrow(
                COLUMN_PAYLOAD
            )

        val responseBodyIndex =
            cursor.getColumnIndexOrThrow(
                COLUMN_RESPONSE_BODY
            )

        val methodIndex =
            cursor.getColumnIndexOrThrow(
                COLUMN_METHOD
            )

        val statusCodeIndex =
            cursor.getColumnIndexOrThrow(
                COLUMN_STATUSCODE
            )

        val statusTextIndex =
            cursor.getColumnIndexOrThrow(
                COLUMN_STATUSTEXT
            )

        val dateIndex =
            cursor.getColumnIndexOrThrow(
                COLUMN_DATE
            )

        val errorIndex =
            cursor.getColumnIndexOrThrow(
                COLUMN_ERROR
            )

        return HttpCallRecord().apply {

            id =
                cursor.getLong(idIndex)

            url =
                cursor.getString(urlIndex)

            payload =
                cursor.getString(payloadIndex)

            responseBody =
                cursor.getString(
                    responseBodyIndex
                )

            method =
                cursor.getString(methodIndex)

            statusCode =
                cursor.getInt(
                    statusCodeIndex
                )

            statusText =
                cursor.getString(
                    statusTextIndex
                )

            date =
                Date(
                    cursor.getLong(dateIndex)
                )

            error =
                cursor.getString(errorIndex)
        }
    }
}
