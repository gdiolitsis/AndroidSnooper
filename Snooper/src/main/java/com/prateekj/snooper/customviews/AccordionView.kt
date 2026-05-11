package com.prateekj.snooper.customviews

import android.annotation.TargetApi
import android.content.Context
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.prateekj.snooper.R

class AccordionView : LinearLayout {

    private var bodyView: View? = null

    private var state =
        COLLAPSE

    private var headerText =
        R.string.accordion_header

    private var headerView: View? = null

    constructor(
        context: Context
    ) : super(context)

    constructor(
        context: Context,
        attrs: AttributeSet
    ) : super(context, attrs) {

        initAttributes(attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int
    ) : super(
        context,
        attrs,
        defStyleAttr
    ) {

        initAttributes(attrs)
    }

    @TargetApi(LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {

        initAttributes(attrs)
    }

    override fun onAttachedToWindow() {

        super.onAttachedToWindow()

        if (headerView == null) {

            initViews()
        }
    }

    fun onStateChange() {

        val body =
            bodyView ?: return

        val header =
            headerView ?: return

        body.visibility =
            if (state == COLLAPSE) {
                View.GONE
            } else {
                View.VISIBLE
            }

        val statusIcon =
            header.findViewById<ImageView>(
                R.id.state_icon
            )

        statusIcon.setImageResource(
            if (state == COLLAPSE) {
                R.drawable.arrow_left_white
            } else {
                R.drawable.arrow_down_white
            }
        )
    }

    private fun initAttributes(
        attributeSet: AttributeSet
    ) {

        val typedArray =
            context.theme.obtainStyledAttributes(
                attributeSet,
                R.styleable.AccordionView,
                0,
                0
            )

        try {

            state =
                typedArray.getInt(
                    R.styleable.AccordionView_state,
                    COLLAPSE
                )

            headerText =
                typedArray.getResourceId(
                    R.styleable.AccordionView_headerText,
                    R.string.accordion_header
                )

        } finally {

            typedArray.recycle()
        }
    }

    private fun initViews() {

        orientation =
            VERTICAL

        headerView =
            LayoutInflater.from(context).inflate(
                R.layout.accordion_view_heading,
                this,
                false
            )

        val header =
            headerView ?: return

        header.findViewById<TextView>(
            R.id.header_text
        ).setText(headerText)

        addView(header, 0)

        bodyView =
            findViewWithTag(
                context.getString(
                    R.string.accordion_body
                )
            )

        val body =
            bodyView ?: return

        val layoutParams =
            body.layoutParams as? MarginLayoutParams
                ?: return

        val margin =
            resources.getDimensionPixelSize(
                R.dimen.accordion_view_body_margin
            )

        layoutParams.setMargins(
            margin,
            0,
            margin,
            0
        )

        body.layoutParams =
            layoutParams

        onStateChange()

        header.setOnClickListener {

            toggleState()

            onStateChange()
        }
    }

    private fun toggleState() {

        state =
            if (state == COLLAPSE) {
                EXPAND
            } else {
                COLLAPSE
            }
    }

    companion object {

        const val COLLAPSE =
            0

        const val EXPAND =
            1
    }
}
