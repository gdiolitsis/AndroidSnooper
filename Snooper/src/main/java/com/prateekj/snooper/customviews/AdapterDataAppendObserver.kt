package com.prateekj.snooper.customviews

import androidx.recyclerview.widget.RecyclerView

class AdapterDataAppendObserver(
    private val listener: PageAddedListener
) : RecyclerView.AdapterDataObserver() {

    override fun onChanged() {

        super.onChanged()

        listener.onPageAdded()
    }

    override fun onItemRangeInserted(
        positionStart: Int,
        itemCount: Int
    ) {

        super.onItemRangeInserted(
            positionStart,
            itemCount
        )

        if (itemCount > 0) {

            listener.onPageAdded()
        }
    }
}
