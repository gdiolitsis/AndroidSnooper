package com.prateekj.snooper.networksnooper.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.prateekj.snooper.R
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.networksnooper.viewmodel.HttpCallViewModel
import kotlinx.android.synthetic.main.activity_http_call_list_item.view.*

class HttpCallListAdapter(
    private var httpCallRecords:
            MutableList<HttpCallRecord>,
    private val listener:
            HttpCallListClickListener
) : RecyclerView.Adapter<
        HttpCallListAdapter.HttpCallViewHolder>() {

    class HttpCallViewHolder(
        private val view: View,
        private val listener:
                HttpCallListClickListener
    ) : RecyclerView.ViewHolder(view) {

        fun bind(
            httpCall: HttpCallRecord
        ) {

            val viewModel =
                HttpCallViewModel(httpCall)

            val statusColor =
                ContextCompat.getColor(
                    view.context,
                    viewModel.getStatusColor()
                )

            view.url.text =
                viewModel.url

            view.method.text =
                viewModel.method

            view.status_code.text =
                viewModel.statusCode

            view.status_text.text =
                viewModel.statusText

            view.time_stamp.text =
                viewModel.timeStamp

            view.response_info_container.visibility =
                viewModel.responseInfoVisibility

            view.error_text.visibility =
                viewModel.failedTextVisibility

            view.method.setTextColor(
                statusColor
            )

            view.status_code.setTextColor(
                statusColor
            )

            view.status_text.setTextColor(
                statusColor
            )

            itemView.setOnClickListener {

                listener.onClick(httpCall)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HttpCallViewHolder {

        val itemView =
            LayoutInflater.from(
                parent.context
            ).inflate(
                R.layout.activity_http_call_list_item,
                parent,
                false
            )

        return HttpCallViewHolder(
            itemView,
            listener
        )
    }

    override fun onBindViewHolder(
        holder: HttpCallViewHolder,
        position: Int
    ) {

        holder.bind(
            httpCallRecords[position]
        )
    }

    override fun getItemCount():
            Int {

        return httpCallRecords.size
    }

    fun refreshData(
        httpCallRecords:
                MutableList<HttpCallRecord>
    ) {

        this.httpCallRecords =
            httpCallRecords
    }

    fun appendData(
        httpCallRecords:
                List<HttpCallRecord>
    ) {

        this.httpCallRecords.addAll(
            httpCallRecords
        )
    }
}
