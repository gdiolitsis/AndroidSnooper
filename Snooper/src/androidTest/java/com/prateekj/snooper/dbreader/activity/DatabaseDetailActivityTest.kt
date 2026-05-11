package com.prateekj.snooper.dbreader.activity

import android.content.Intent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.prateekj.snooper.R
import com.prateekj.snooper.dbreader.activity.DatabaseDetailActivity.Companion.TABLE_NAME
import com.prateekj.snooper.dbreader.activity.DatabaseListActivity.Companion.DB_PATH
import com.prateekj.snooper.rules.TestDbRule
import com.prateekj.snooper.utils.EspressoViewMatchers.withRecyclerView
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DatabaseDetailActivityTest {

    @get:Rule
    var testDbRule =
        TestDbRule(
            R.raw.test,
            "test.db"
        )

    @get:Rule
    var activityRule =
        ActivityScenarioRule(
            Intent(
                ApplicationProvider.getApplicationContext(),
                DatabaseDetailActivity::class.java
            )
        )

    @Before
    fun setupIntents() {

        Intents.init()
    }

    @After
    fun releaseIntents() {

        Intents.release()
    }

    @Test
    @Throws(Exception::class)
    fun shouldRenderDatabaseInformation() {

        val context =
            ApplicationProvider.getApplicationContext<android.content.Context>()

        val dbPath =
            "${testDbRule.dbDirectory}/test.db"

        val intent =
            Intent(
                context,
                DatabaseDetailActivity::class.java
            ).apply {

                putExtra(
                    DatabaseDetailActivity.DB_PATH,
                    dbPath
                )

                putExtra(
                    DatabaseListActivity.DB_NAME,
                    "test.db"
                )
            }

        activityRule.scenario.onActivity {

            it.intent.putExtras(
                intent
            )
        }

        onView(
            withId(R.id.db_name)
        ).check(
            matches(
                withText("test.db")
            )
        )

        onView(
            withId(R.id.db_version)
        ).check(
            matches(
                withText("0")
            )
        )

        onView(
            withRecyclerView(
                R.id.table_list,
                0
            )
        ).check(

            matches(

                allOf<View>(

                    hasDescendant(
                        withText("1. ")
                    ),

                    hasDescendant(
                        withText("person")
                    )
                )
            )
        )

        onView(
            withRecyclerView(
                R.id.table_list,
                1
            )
        ).check(

            matches(

                allOf<View>(

                    hasDescendant(
                        withText("2. ")
                    ),

                    hasDescendant(
                        withText("sqlite_sequence")
                    )
                )
            )
        )

        onView(
            withRecyclerView(
                R.id.table_list,
                0
            )
        ).perform(
            click()
        )

        intended(

            allOf(

                hasComponent(
                    TableDetailActivity::class.java.name
                ),

                hasExtra(
                    TABLE_NAME,
                    "person"
                ),

                hasExtra(
                    DB_PATH,
                    dbPath
                )
            )
        )
    }
}
