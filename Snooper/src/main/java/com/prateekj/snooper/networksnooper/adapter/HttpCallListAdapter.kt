package com.prateekj.snooper.networksnooper.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.prateekj.snooper.databinding.ActivityHttpCallListItemBinding
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.networksnooper.viewmodel.HttpCallViewModel

class HttpCallListAdapter(
    private var httpCallRecords:
            MutableList<HttpCallRecord>,
    private val listener:
            HttpCallListClickListener
) : RecyclerView.Adapter<
        HttpCallListAdapter.HttpCallViewHolder>() {

    class HttpCallViewHolder(
        private val binding:
                ActivityHttpCallListItemBinding,
        private val listener:
                HttpCallListClickListener
    ) : RecyclerView.ViewHolder(
        binding.root
    ) {

        fun bind(
            httpCall: HttpCallRecord
        ) {

            val viewModel =
                HttpCallViewModel(
                    httpCall
                )

            val statusColor =
                ContextCompat.getColor(
                    binding.root.context,
                    viewModel.getStatusColor()
                )

            binding.url.text =
                viewModel.url

            binding.method.text =
                viewModel.method

            binding.statusCode.text =
                viewModel.statusCode

            binding.statusText.text =
                viewModel.statusText

            binding.timeStamp.text =
                viewModel.timeStamp

            binding.responseInfoContainer.visibility =
                viewModel.responseInfoVisibility

            binding.errorText.visibility =
                viewModel.failedTextVisibility

            binding.method.setTextColor(
                statusColor
            )

            binding.statusCode.setTextColor(
                statusColor
            )

            binding.statusText.setTextColor(
                statusColor
            )

            binding.root.setOnClickListener {

                listener.onClick(
                    httpCall
                )
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HttpCallViewHolder {

        val binding =
            ActivityHttpCallListItemBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )

        return HttpCallViewHolder(
            binding,
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
