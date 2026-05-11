package com.prateekj.snooper.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.HEADER_CREATE_TABLE
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.HEADER_VALUE_CREATE_TABLE
import com.prateekj.snooper.networksnooper.database.HttpCallRecordContract.Companion.HTTP_CALL_RECORD_CREATE_TABLE

class SnooperDbHelper private constructor(
    context: Context
) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    init {

        super.setWriteAheadLoggingEnabled(true)
    }

    override fun onConfigure(
        db: SQLiteDatabase
    ) {

        super.onConfigure(db)

        db.execSQL(
            "PRAGMA foreign_keys = ON"
        )
    }

    override fun onCreate(
        db: SQLiteDatabase
    ) {

        db.execSQL(
            HTTP_CALL_RECORD_CREATE_TABLE
        )

        db.execSQL(
            HEADER_CREATE_TABLE
        )

        db.execSQL(
            HEADER_VALUE_CREATE_TABLE
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {

        // no-op
    }

    companion object {

        private const val DATABASE_VERSION =
            1

        const val DATABASE_NAME =
            "snooper.db"

        @Volatile
        private var INSTANCE:
                SnooperDbHelper? = null

        @MainThread
        fun getInstance(
            context: Context
        ): SnooperDbHelper {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: SnooperDbHelper(
                    context.applicationContext
                ).also {

                    INSTANCE = it
                }
            }
        }

        @AnyThread
        fun create(
            context: Context
        ): SnooperDbHelper {

            return SnooperDbHelper(
                context.applicationContext
            )
        }
    }
}
