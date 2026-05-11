package com.prateekj.snooper.networksnooper.database

import android.provider.BaseColumns

class HttpCallRecordContract :
    BaseColumns {

    companion object {

        // =====================================================
        // TABLES
        // =====================================================

        const val HTTP_CALL_RECORD_TABLE_NAME =
            "http_calls"

        const val HEADER_TABLE_NAME =
            "header"

        const val HEADER_VALUE_TABLE_NAME =
            "header_value"

        // =====================================================
        // HTTP CALL COLUMNS
        // =====================================================

        internal const val COLUMN_URL =
            "url"

        internal const val COLUMN_PAYLOAD =
            "payload"

        internal const val COLUMN_METHOD =
            "method"

        internal const val COLUMN_RESPONSE_BODY =
            "responseBody"

        internal const val COLUMN_STATUSTEXT =
            "statusText"

        internal const val COLUMN_STATUSCODE =
            "statusCode"

        internal const val COLUMN_DATE =
            "date"

        internal const val COLUMN_ERROR =
            "error"

        // =====================================================
        // HEADER COLUMNS
        // =====================================================

        internal const val COLUMN_HEADER_NAME =
            "name"

        internal const val COLUMN_HEADER_TYPE =
            "type"

        internal const val COLUMN_HTTP_CALL_RECORD_ID =
            "record_id"

        // =====================================================
        // HEADER VALUE COLUMNS
        // =====================================================

        internal const val COLUMN_HEADER_VALUE =
            "value"

        internal const val COLUMN_HEADER_ID =
            "header_id"

        // =====================================================
        // QUERIES
        // =====================================================

        internal const val HTTP_CALL_RECORD_GET_SORT_BY_DATE =
            """
            SELECT *
            FROM http_calls
            ORDER BY date DESC
            """

        internal const val HTTP_CALL_RECORD_SEARCH =
            """
            SELECT *
            FROM http_calls
            WHERE url LIKE ?
               OR payload LIKE ?
               OR responseBody LIKE ?
               OR error LIKE ?
            ORDER BY date DESC
            """

        const val HTTP_CALL_RECORD_GET_SORT_BY_DATE_WITH_SIZE =
            """
            SELECT *
            FROM http_calls
            ORDER BY date DESC
            LIMIT ?
            """

        internal const val HTTP_CALL_RECORD_GET_NEXT_SORT_BY_DATE_WITH_SIZE =
            """
            SELECT *
            FROM http_calls
            WHERE _id < ?
            ORDER BY date DESC
            LIMIT ?
            """

        internal const val HTTP_CALL_RECORD_GET_BY_ID =
            """
            SELECT *
            FROM http_calls
            WHERE _id = ?
            """

        const val HTTP_HEADER_GET_BY_CALL_ID =
            """
            SELECT *
            FROM header
            WHERE record_id = ?
              AND type = ?
            """

        internal const val HTTP_HEADER_VALUE_GET_BY_HEADER_ID =
            """
            SELECT *
            FROM header_value
            WHERE header_id = ?
            """

        // =====================================================
        // CREATE TABLES
        // =====================================================

        const val HTTP_CALL_RECORD_CREATE_TABLE =
            """
            CREATE TABLE IF NOT EXISTS http_calls (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                url TEXT,
                payload TEXT,
                responseBody TEXT,
                error TEXT,
                method VARCHAR(10),
                statusText VARCHAR(10),
                statusCode INTEGER,
                date DOUBLE
            )
            """

        const val HEADER_CREATE_TABLE =
            """
            CREATE TABLE IF NOT EXISTS header (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                type VARCHAR(3),
                name VARCHAR(255),
                record_id INTEGER,
                CONSTRAINT chk_header_type
                    CHECK (type IN ('req', 'res')),
                CONSTRAINT fk_http_call_header
                    FOREIGN KEY (record_id)
                    REFERENCES http_calls(_id)
                    ON DELETE CASCADE
            )
            """

        const val HEADER_VALUE_CREATE_TABLE =
            """
            CREATE TABLE IF NOT EXISTS header_value (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                value VARCHAR(255),
                header_id INTEGER,
                CONSTRAINT fk_header_value
                    FOREIGN KEY (header_id)
                    REFERENCES header(_id)
                    ON DELETE CASCADE
            )
            """
    }
}
