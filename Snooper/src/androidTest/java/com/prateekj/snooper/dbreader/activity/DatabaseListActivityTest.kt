package com.prateekj.snooper.dbreader.activity

import android.content.Intent
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.prateekj.snooper.R
import com.prateekj.snooper.dbreader.activity.DatabaseListActivity.Companion.DB_NAME
import com.prateekj.snooper.dbreader.activity.DatabaseListActivity.Companion.DB_PATH
import com.prateekj.snooper.rules.TestDbRule
import com.prateekj.snooper.utils.EspressoViewMatchers.withRecyclerView
import org.hamcrest.core.AllOf.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DatabaseListActivityTest {

    @get:Rule
    var testDbRule =
        TestDbRule(
            R.raw.test,
            "test.db"
        )

    @get:Rule
    var activityRule =
        ActivityScenarioRule(
            DatabaseListActivity::class.java
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
    fun shouldRenderListOfDatabases() {

        val dbName =
            "test.db"

        val dbLocation =
            "${testDbRule.dbDirectory}/$dbName"

        onView(
            withRecyclerView(
                R.id.db_list,
                0
            )
        ).check(

            matches(

                allOf<View>(

                    hasDescendant(
                        withText(dbName)
                    ),

                    hasDescendant(
                        withText(dbLocation)
                    )
                )
            )
        )

        onView(
            withText(dbName)
        ).perform(click())

        intended(

            allOf<Intent>(

                hasComponent(
                    DatabaseDetailActivity::class.java.name
                ),

                hasExtra(
                    DB_NAME,
                    dbName
                ),

                hasExtra(
                    DB_PATH,
                    dbLocation
                )
            )
        )
    }
}
