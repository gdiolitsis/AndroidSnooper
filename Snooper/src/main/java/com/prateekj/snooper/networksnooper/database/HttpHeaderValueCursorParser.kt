package com.prateekj.snooper.networksnooper.database

import android.database.Cursor
import android.provider.BaseColumns._ID
import com.prateekj.snooper.database.CursorParser
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_HEADER_VALUE
import com.prateekj.snooper.networksnooper.model.HttpHeaderValue

class HttpHeaderValueCursorParser :
    CursorParser<HttpHeaderValue> {

    override fun parse(
        cursor: Cursor
    ): HttpHeaderValue {

        val idIndex =
            cursor.getColumnIndexOrThrow(
                _ID
            )

        val valueIndex =
            cursor.getColumnIndexOrThrow(
                COLUMN_HEADER_VALUE
            )

        return HttpHeaderValue().apply {

            setId(
                cursor.getInt(idIndex)
            )

            value =
                cursor.getString(valueIndex)
        }
    }
}
