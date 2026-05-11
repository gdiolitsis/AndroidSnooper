package com.prateekj.snooper.customviews

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PaginatedRecyclerView :
    RecyclerView,
    PageAddedListener {

    private var listener:
            NextPageRequestListener? = null

    @Volatile
    private var loading =
        false

    constructor(
        context: Context
    ) : super(context)

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(
        context,
        attrs
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(
        context,
        attrs,
        defStyle
    )

    override fun setAdapter(
        adapter: Adapter<*>?
    ) {

        super.setAdapter(adapter)

        adapter?.registerAdapterDataObserver(
            AdapterDataAppendObserver(this)
        )
    }

    fun setNextPageListener(
        listener: NextPageRequestListener
    ) {

        this.listener = listener
    }

    override fun onScrolled(
        dx: Int,
        dy: Int
    ) {

        super.onScrolled(dx, dy)

        val nextPageListener =
            listener ?: return

        if (loading) {
            return
        }

        if (
            needToRequestNextPage() &&
            !nextPageListener.areAllPagesLoaded()
        ) {

            loading = true

            nextPageListener.requestNextPage()
        }
    }

    private fun needToRequestNextPage(): Boolean {

        val linearLayoutManager =
            layoutManager as? LinearLayoutManager
                ?: return false

        val recyclerAdapter =
            adapter ?: return false

        val visibleItemCount =
            linearLayoutManager.childCount

        val totalItemCount =
            recyclerAdapter.itemCount

        val firstVisibleItemPosition =
            linearLayoutManager
                .findFirstVisibleItemPosition()

        if (totalItemCount <= 0) {
            return false
        }

        return firstVisibleItemPosition >= 0 &&
                (
                    firstVisibleItemPosition +
                    visibleItemCount
                ) >= (totalItemCount - 5)
    }

    override fun onPageAdded() {

        loading = false
    }
}
