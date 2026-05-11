package com.prateekj.snooper.networksnooper.database

import android.database.Cursor
import android.provider.BaseColumns._ID
import com.prateekj.snooper.database.CursorParser
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.COLUMN_HEADER_NAME
import com.prateekj.snooper.networksnooper.model.HttpHeader

class HttpHeaderCursorParser :
    CursorParser<HttpHeader> {

    override fun parse(
        cursor: Cursor
    ): HttpHeader {

        val idIndex =
            cursor.getColumnIndexOrThrow(
                _ID
            )

        val nameIndex =
            cursor.getColumnIndexOrThrow(
                COLUMN_HEADER_NAME
            )

        return HttpHeader().apply {

            id =
                cursor.getInt(idIndex)

            name =
                cursor.getString(nameIndex)
        }
    }
}
