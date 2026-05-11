package com.prateekj.snooper.networksnooper.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.prateekj.snooper.databinding.HeaderListItemBinding
import com.prateekj.snooper.networksnooper.model.HttpHeader
import com.prateekj.snooper.networksnooper.viewmodel.HttpHeaderViewModel

class HttpHeaderAdapter private constructor(
    headers: List<HttpHeader>
) : BaseAdapter() {

    private val viewModels:
            List<HttpHeaderViewModel> =
        headers.map {
            HttpHeaderViewModel(it)
        }

    override fun getCount():
            Int {

        return viewModels.size
    }

    override fun getItem(
        position: Int
    ): HttpHeaderViewModel {

        return viewModels[position]
    }

    override fun getItemId(
        position: Int
    ): Long {

        return position.toLong()
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {

        val binding =
            if (convertView == null) {

                HeaderListItemBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ),
                    parent,
                    false
                )

            } else {

                convertView.tag
                        as HeaderListItemBinding
            }

        val view =
            binding.root

        if (view.tag == null) {

            view.tag = binding
        }

        val viewModel =
            getItem(position)

        binding.headerName.text =
            viewModel.headerName()

        binding.headerValue.text =
            viewModel.headerValues()

        return view
    }

    companion object {

        fun newInstance(
            headers: List<HttpHeader>
        ): HttpHeaderAdapter {

            return HttpHeaderAdapter(
                headers
            )
        }
    }
}
