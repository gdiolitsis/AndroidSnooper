package com.prateekj.snooper.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DividerItemDecoration(
    context: Context,
    orientation: Int,
    drawableId: Int
) : RecyclerView.ItemDecoration() {

    private val divider: Drawable? =
        ContextCompat.getDrawable(
            context,
            drawableId
        )

    private var orientation =
        VERTICAL

    init {

        setOrientation(orientation)
    }

    fun setOrientation(
        orientation: Int
    ) {

        require(
            orientation == HORIZONTAL ||
                    orientation == VERTICAL
        ) {
            "Invalid orientation"
        }

        this.orientation = orientation
    }

    override fun onDraw(
        canvas: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {

        super.onDraw(
            canvas,
            parent,
            state
        )

        val drawable =
            divider ?: return

        if (orientation == VERTICAL) {

            drawVertical(
                canvas,
                parent,
                drawable
            )

        } else {

            drawHorizontal(
                canvas,
                parent,
                drawable
            )
        }
    }

    private fun drawVertical(
        canvas: Canvas,
        parent: RecyclerView,
        drawable: Drawable
    ) {

        val left =
            parent.paddingLeft

        val right =
            parent.width - parent.paddingRight

        val childCount =
            parent.childCount

        for (i in 0 until childCount) {

            val child =
                parent.getChildAt(i)

            val params =
                child.layoutParams
                        as RecyclerView.LayoutParams

            val top =
                child.bottom + params.bottomMargin

            val bottom =
                top + drawable.intrinsicHeight

            drawable.setBounds(
                left,
                top,
                right,
                bottom
            )

            drawable.draw(canvas)
        }
    }

    private fun drawHorizontal(
        canvas: Canvas,
        parent: RecyclerView,
        drawable: Drawable
    ) {

        val top =
            parent.paddingTop

        val bottom =
            parent.height - parent.paddingBottom

        val childCount =
            parent.childCount

        for (i in 0 until childCount) {

            val child =
                parent.getChildAt(i)

            val params =
                child.layoutParams
                        as RecyclerView.LayoutParams

            val left =
                child.right + params.rightMargin

            val right =
                left + drawable.intrinsicWidth

            drawable.setBounds(
                left,
                top,
                right,
                bottom
            )

            drawable.draw(canvas)
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {

        val drawable =
            divider ?: return

        if (orientation == VERTICAL) {

            outRect.set(
                0,
                0,
                0,
                drawable.intrinsicHeight
            )

        } else {

            outRect.set(
                0,
                0,
                drawable.intrinsicWidth,
                0
            )
        }
    }

    companion object {

        const val HORIZONTAL =
            LinearLayoutManager.HORIZONTAL

        const val VERTICAL =
            LinearLayoutManager.VERTICAL
    }
}
