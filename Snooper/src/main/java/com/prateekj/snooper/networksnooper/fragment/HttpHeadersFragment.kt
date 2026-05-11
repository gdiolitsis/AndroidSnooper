package com.prateekj.snooper.networksnooper.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.prateekj.snooper.R
import com.prateekj.snooper.networksnooper.activity.HttpCallActivity.Companion.HTTP_CALL_ID
import com.prateekj.snooper.networksnooper.adapter.HttpHeaderAdapter
import com.prateekj.snooper.networksnooper.database.SnooperRepo
import com.prateekj.snooper.networksnooper.viewmodel.HttpCallViewModel
import kotlinx.android.synthetic.main.fragment_headers.view.*
import kotlinx.android.synthetic.main.http_call_general_detail.view.*

class HttpHeadersFragment :
    Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view =
            inflater.inflate(
                R.layout.fragment_headers,
                container,
                false
            )

        val repo =
            SnooperRepo(
                requireActivity()
            )

        val httpCallId =
            requireArguments()
                .getLong(HTTP_CALL_ID)

        val httpCallRecord =
            repo.findById(httpCallId)

        val viewModel =
            HttpCallViewModel(
                httpCallRecord
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

        view.response_header_list.adapter =
            HttpHeaderAdapter.newInstance(
                viewModel.responseHeaders
            )

        view.request_header_list.adapter =
            HttpHeaderAdapter.newInstance(
                viewModel.requestHeaders
            )

        view.response_header_container.visibility =
            viewModel.responseHeaderVisibility

        view.request_header_container.visibility =
            viewModel.requestHeaderVisibility

        return view
    }
}
