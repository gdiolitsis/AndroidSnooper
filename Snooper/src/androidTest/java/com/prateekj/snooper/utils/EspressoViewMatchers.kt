package com.prateekj.snooper.utils

import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.hamcrest.CustomTypeSafeMatcher
import org.hamcrest.Matcher

object EspressoViewMatchers {

    fun withRecyclerView(
        recyclerViewId: Int,
        position: Int
    ): Matcher<View> {

        return object : CustomTypeSafeMatcher<View>(
            "RecyclerView id=$recyclerViewId position=$position"
        ) {

            override fun matchesSafely(
                item: View
            ): Boolean {

                val recyclerView =
                    item.rootView.findViewById<RecyclerView>(
                        recyclerViewId
                    ) ?: return false

                val viewHolder =
                    recyclerView.findViewHolderForAdapterPosition(
                        position
                    ) ?: return false

                return viewHolder.itemView == item
            }
        }
    }

    fun withTableLayout(
        tableLayoutId: Int,
        row: Int,
        column: Int
    ): Matcher<View> {

        return object : CustomTypeSafeMatcher<View>(
            "TableLayout id=$tableLayoutId row=$row column=$column"
        ) {

            override fun matchesSafely(
                item: View
            ): Boolean {

                val tableLayout =
                    item.rootView.findViewById<TableLayout>(
                        tableLayoutId
                    ) ?: return false

                val tableRow =
                    tableLayout.getChildAt(row)
                            as? TableRow
                        ?: return false

                val childView =
                    tableRow.getChildAt(column)
                        ?: return false

                return childView == item
            }
        }
    }

    fun withListSize(
        size: Int
    ): Matcher<View> {

        return object : CustomTypeSafeMatcher<View>(
            "RecyclerView with size=$size"
        ) {

            override fun matchesSafely(
                view: View
            ): Boolean {

                val recyclerView =
                    view as? RecyclerView
                        ?: return false

                return recyclerView
                    .adapter
                    ?.itemCount == size
            }
        }
    }

    fun hasBackgroundSpanOn(
        text: String,
        @ColorRes colorResource: Int
    ): Matcher<View> {

        return object : CustomTypeSafeMatcher<View>(
            "Text contains highlighted span"
        ) {

            override fun matchesSafely(
                view: View
            ): Boolean {

                val textView =
                    view as? TextView
                        ?: return false

                val spannable =
                    textView.text as? Spannable
                        ?: return false

                val expectedColor =
                    ContextCompat.getColor(
                        textView.context,
                        colorResource
                    )

                val spans =
                    spannable.getSpans(
                        0,
                        spannable.length,
                        BackgroundColorSpan::class.java
                    )

                for (span in spans) {

                    val start =
                        spannable.getSpanStart(span)

                    val end =
                        spannable.getSpanEnd(span)

                    val highlightedText =
                        spannable.subSequence(
                            start,
                            end
                        ).toString()

                    if (
                        highlightedText == text &&
                        span.backgroundColor == expectedColor
                    ) {

                        return true
                    }
                }

                return false
            }
        }
    }
}
