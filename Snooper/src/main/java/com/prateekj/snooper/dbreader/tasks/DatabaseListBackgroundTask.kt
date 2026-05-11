package com.prateekj.snooper.dbreader.tasks

import android.content.Context
import com.prateekj.snooper.database.SnooperDbHelper.Companion.DATABASE_NAME
import com.prateekj.snooper.dbreader.model.Database
import com.prateekj.snooper.dbreader.view.DbReaderCallback
import com.prateekj.snooper.infra.BackgroundTask

class DatabaseListBackgroundTask(
    private val context: Context,
    private val dbReaderCallback: DbReaderCallback
) : BackgroundTask<List<Database>> {

    override fun onExecute(): List<Database> {

        val applicationDatabases =
            context.applicationContext
                .databaseList()

        if (
            applicationDatabases.isNullOrEmpty()
        ) {

            return emptyList()
        }

        return applicationDatabases

            .filter { dbName ->

                dbName.endsWith(".db") &&
                        dbName != DATABASE_NAME
            }

            .map { dbName ->

                val databasePath =
                    context.getDatabasePath(
                        dbName
                    )

                Database(
                    name = databasePath.name,
                    path = databasePath.absolutePath
                )
            }
    }

    override fun onResult(
        result: List<Database>
    ) {

        dbReaderCallback
            .onApplicationDbFetchCompleted(
                result
            )
    }
}
