package com.prateekj.snooper.dbreader.activity

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.prateekj.snooper.R
import com.prateekj.snooper.rules.TestDbRule
import com.prateekj.snooper.utils.EspressoViewMatchers.withTableLayout
import org.junit.Rule
import org.junit.Test

class TableDetailActivityTest {

    @get:Rule
    var testDbRule =
        TestDbRule(
            R.raw.test,
            "test.db"
        )

    @Test
    @Throws(Exception::class)
    fun shouldRenderDataOfTable() {

        val intent = Intent().apply {

            putExtra(
                DatabaseDetailActivity.TABLE_NAME,
                "person"
            )

            putExtra(
                DatabaseDetailActivity.DB_PATH,
                "${testDbRule.dbDirectory}/test.db"
            )
        }

        ActivityScenario.launch<TableDetailActivity>(
            intent
        )

        onView(
            withTableLayout(
                R.id.table_layout,
                0,
                0
            )
        ).check(
            matches(
                withText("S.No.")
            )
        )

        onView(
            withTableLayout(
                R.id.table_layout,
                0,
                1
            )
        ).check(
            matches(
                withText("_ID")
            )
        )

        onView(
            withTableLayout(
                R.id.table_layout,
                0,
                2
            )
        ).check(
            matches(
                withText("NAME")
            )
        )

        onView(
            withTableLayout(
                R.id.table_layout,
                1,
                0
            )
        ).check(
            matches(
                withText("1")
            )
        )

        onView(
            withTableLayout(
                R.id.table_layout,
                1,
                1
            )
        ).check(
            matches(
                withText("1")
            )
        )

        onView(
            withTableLayout(
                R.id.table_layout,
                1,
                2
            )
        ).check(
            matches(
                withText("Mr. Smith")
            )
        )

        onView(
            withTableLayout(
                R.id.table_layout,
                2,
                1
            )
        ).check(
            matches(
                withText("2")
            )
        )

        onView(
            withTableLayout(
                R.id.table_layout,
                2,
                2
            )
        ).check(
            matches(
                withText("Mr. Randolf")
            )
        )
    }
}
