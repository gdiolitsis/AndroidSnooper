package com.prateekj.snooper.networksnooper.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.prateekj.snooper.databinding.FragmentHeadersBinding
import com.prateekj.snooper.networksnooper.activity.HttpCallActivity.Companion.HTTP_CALL_ID
import com.prateekj.snooper.networksnooper.adapter.HttpHeaderAdapter
import com.prateekj.snooper.networksnooper.database.SnooperRepo
import com.prateekj.snooper.networksnooper.viewmodel.HttpCallViewModel

class HttpHeadersFragment :
    Fragment() {

    private var _binding:
            FragmentHeadersBinding? = null

    private val binding:
            FragmentHeadersBinding
        get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentHeadersBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
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

        binding.responseHeaderList.adapter =
            HttpHeaderAdapter.newInstance(
                viewModel.responseHeaders
            )

        binding.requestHeaderList.adapter =
            HttpHeaderAdapter.newInstance(
                viewModel.requestHeaders
            )

        binding.responseHeaderContainer.visibility =
            viewModel.responseHeaderVisibility

        binding.requestHeaderContainer.visibility =
            viewModel.requestHeaderVisibility
    }

    override fun onDestroyView() {

        super.onDestroyView()

        binding.responseHeaderList.adapter =
            null

        binding.requestHeaderList.adapter =
            null

        _binding = null
    }
}
