package com.prateekj.snooper.networksnooper.fragment

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.TextView.BufferType.SPANNABLE
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.NestedScrollView.OnScrollChangeListener
import androidx.fragment.app.Fragment
import com.prateekj.snooper.R
import com.prateekj.snooper.databinding.FragmentResponseBodyBinding
import com.prateekj.snooper.formatter.ResponseFormatterFactory
import com.prateekj.snooper.infra.BackgroundTaskExecutor
import com.prateekj.snooper.networksnooper.activity.HttpCallActivity.Companion.HTTP_CALL_ID
import com.prateekj.snooper.networksnooper.activity.HttpCallActivity.Companion.HTTP_CALL_MODE
import com.prateekj.snooper.networksnooper.database.SnooperRepo
import com.prateekj.snooper.networksnooper.model.Bound
import com.prateekj.snooper.networksnooper.presenter.HttpCallFragmentPresenter
import com.prateekj.snooper.networksnooper.viewmodel.HttpBodyViewModel
import com.prateekj.snooper.networksnooper.views.HttpCallBodyView
import com.prateekj.snooper.utils.Logger
import kotlin.math.min

class HttpCallFragment :
    Fragment(),
    HttpCallBodyView,
    OnQueryTextListener,
    OnScrollChangeListener {

    private var _binding:
            FragmentResponseBodyBinding? = null

    private val binding:
            FragmentResponseBodyBinding
        get() = requireNotNull(_binding)

    private var mode = 0

    private lateinit var viewModel:
            HttpBodyViewModel

    private var presenter:
            HttpCallFragmentPresenter? = null

    private var lastBoundHighlightedIndex =
        0

    private var bounds:
            List<Bound>? = null

    private var ythPositionOfLastHighlightedBound =
        0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentResponseBodyBinding.inflate(
                inflater,
                container,
                false
            )

        viewModel =
            HttpBodyViewModel()

        val repo =
            SnooperRepo(
                requireActivity()
            )

        val httpCallId =
            requireArguments()
                .getLong(HTTP_CALL_ID)

        mode =
            requireArguments()
                .getInt(HTTP_CALL_MODE)

        presenter =
            HttpCallFragmentPresenter(
                repo,
                httpCallId,
                this,
                ResponseFormatterFactory(),
                BackgroundTaskExecutor(
                    requireActivity()
                )
            )

        setHasOptionsMenu(true)

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

        changeLoaderVisibility(
            View.VISIBLE
        )

        presenter?.init(
            viewModel,
            mode
        )

        binding.scrollView
            .setOnScrollChangeListener(this)
    }

    override fun onDestroyView() {

        super.onDestroyView()

        binding.scrollView
            .setOnScrollChangeListener(null)

        _binding = null
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {

        val searchMenu =
            menu.findItem(
                R.id.search_menu
            )

        searchMenu.isVisible = true

        (
            searchMenu.actionView
                    as? SearchView
            )?.setOnQueryTextListener(this)

        super.onCreateOptionsMenu(
            menu,
            inflater
        )
    }

    override fun onFormattingDone() {

        if (!isAdded || _binding == null) {
            return
        }

        val spannable =
            SpannableStringBuilder(
                viewModel.formattedBody
            )

        binding.payloadText.setText(
            spannable,
            SPANNABLE
        )

        changeLoaderVisibility(GONE)
    }

    private fun changeLoaderVisibility(
        visibility: Int
    ) {

        _binding?.embeddedLoader
            ?.visibility = visibility
    }

    override fun highlightBounds(
        bounds: List<Bound>
    ) {

        if (!isAdded || _binding == null) {
            return
        }

        this.bounds = bounds

        Logger.d(
            HttpCallFragment::class.java.simpleName,
            "Total size: ${bounds.size}"
        )

        if (bounds.isEmpty()) {
            return
        }

        highlightStringFromBounds(
            bounds.subList(
                lastBoundHighlightedIndex,
                min(
                    BOUNDS_HIGHLIGHT_SET_SIZE,
                    bounds.size
                )
            )
        )

        scrollTillYOffset(
            getYthPositionOfBoundInBody(
                bounds[0]
            )
        )
    }

    override fun removeOldHighlightedSpans() {

        val spannable =
            binding.payloadText.text
                    as? Spannable
                    ?: return

        val spans =
            spannable.getSpans(
                0,
                spannable.length,
                BackgroundColorSpan::class.java
            )

        spans.forEach {

            spannable.removeSpan(it)
        }
    }

    private fun scrollTillYOffset(
        yOffset: Int
    ) {

        binding.scrollView.post {

            binding.scrollView.scrollTo(
                0,
                yOffset
            )
        }
    }

    override fun onQueryTextSubmit(
        query: String?
    ): Boolean {

        return false
    }

    override fun onQueryTextChange(
        newText: String?
    ): Boolean {

        lastBoundHighlightedIndex = 0

        presenter?.searchInBody(
            newText
                ?.lowercase()
                ?: ""
        )

        return true
    }

    override fun onScrollChange(
        v: NestedScrollView,
        scrollX: Int,
        scrollY: Int,
        oldScrollX: Int,
        oldScrollY: Int
    ) {

        if (
            hasBoundsToHighlight() &&
            needToHighlightNextSetOfBounds(
                scrollY
            )
        ) {

            val calculatedToIndex =
                lastBoundHighlightedIndex +
                        BOUNDS_HIGHLIGHT_SET_SIZE

            highlightStringFromBounds(
                bounds!!.subList(
                    lastBoundHighlightedIndex + 1,
                    min(
                        calculatedToIndex,
                        bounds!!.size
                    )
                )
            )
        }
    }

    private fun getYthPositionOfBoundInBody(
        bound: Bound
    ): Int {

        val lineNumber =
            getLineNumber(
                bound.left
            )

        return binding.payloadText
            .layout
            ?.getLineTop(lineNumber)
            ?: 0
    }

    private fun getLineNumber(
        offset: Int
    ): Int {

        return binding.payloadText
            .layout
            ?.getLineForOffset(offset)
            ?: 0
    }

    private fun highlightStringFromBounds(
        bounds: List<Bound>
    ) {

        val text =
            binding.payloadText.text
                    as? Spannable
                    ?: return

        binding.payloadText.postDelayed(
            getHighlightAction(
                text,
                bounds
            ),
            5
        )
    }

    private fun getHighlightAction(
        text: Spannable,
        boundsCurrentSet: List<Bound>
    ): Runnable {

        return Runnable {

            if (!isAdded || _binding == null) {
                return@Runnable
            }

            boundsCurrentSet.forEach { bound ->

                text.setSpan(
                    BackgroundColorSpan(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.snooper_text_highlight_color
                        )
                    ),
                    bound.left,
                    bound.right,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                if (
                    boundsCurrentSet.last() == bound
                ) {

                    ythPositionOfLastHighlightedBound =
                        getYthPositionOfBoundInBody(
                            bound
                        )

                    lastBoundHighlightedIndex =
                        bounds?.indexOf(bound)
                            ?: 0
                }
            }
        }
    }

    private fun needToHighlightNextSetOfBounds(
        scrollY: Int
    ): Boolean {

        return (
            ythPositionOfLastHighlightedBound -
                    scrollY
            ) < NEXT_SET_HIGHLIGHT_SCROLL_LINE_BUFFER
    }

    private fun hasBoundsToHighlight():
            Boolean {

        return bounds != null &&
                lastBoundHighlightedIndex <
                (bounds!!.size - 1)
    }

    companion object {

        const val NEXT_SET_HIGHLIGHT_SCROLL_LINE_BUFFER =
            20

        const val BOUNDS_HIGHLIGHT_SET_SIZE =
            50
    }
}
