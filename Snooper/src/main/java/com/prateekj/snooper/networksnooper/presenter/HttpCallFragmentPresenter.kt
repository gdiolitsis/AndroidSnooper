package com.prateekj.snooper.networksnooper.presenter

import com.prateekj.snooper.formatter.PlainTextFormatter
import com.prateekj.snooper.formatter.ResponseFormatter
import com.prateekj.snooper.formatter.ResponseFormatterFactory
import com.prateekj.snooper.infra.BackgroundTask
import com.prateekj.snooper.infra.BackgroundTaskExecutor
import com.prateekj.snooper.networksnooper.activity.HttpCallActivity.Companion.ERROR_MODE
import com.prateekj.snooper.networksnooper.activity.HttpCallActivity.Companion.REQUEST_MODE
import com.prateekj.snooper.networksnooper.database.SnooperRepo
import com.prateekj.snooper.networksnooper.model.Bound
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.networksnooper.model.HttpHeader
import com.prateekj.snooper.networksnooper.model.HttpHeader.Companion.CONTENT_TYPE
import com.prateekj.snooper.networksnooper.viewmodel.HttpBodyViewModel
import com.prateekj.snooper.networksnooper.views.HttpCallBodyView
import java.util.Locale

class HttpCallFragmentPresenter(
    private val repo: SnooperRepo,
    private val httpCallId: Long,
    private val httpCallBodyView: HttpCallBodyView,
    private val formatterFactory: ResponseFormatterFactory,
    private val executor: BackgroundTaskExecutor
) {

    private var mode: Int = 0

    private var formattedBodyLowerCase:
            String = ""

    fun init(
        viewModel: HttpBodyViewModel,
        mode: Int
    ) {

        this.mode = mode

        val httpCallRecord =
            repo.findById(httpCallId)

        val formatter =
            getFormatter(httpCallRecord)

        val bodyToFormat =
            getBodyToFormat(httpCallRecord)
                ?: ""

        executor.execute(
            object : BackgroundTask<String> {

                override fun onExecute():
                        String {

                    val formattedBody =
                        formatter.format(
                            bodyToFormat
                        )

                    formattedBodyLowerCase =
                        formattedBody.lowercase(
                            Locale.getDefault()
                        )

                    return formattedBody
                }

                override fun onResult(
                    result: String
                ) {

                    viewModel.init(result)

                    httpCallBodyView
                        .onFormattingDone()
                }
            }
        )
    }

    fun searchInBody(
        pattern: String
    ) {

        httpCallBodyView
            .removeOldHighlightedSpans()

        if (
            pattern.isEmpty()
        ) {
            return
        }

        val bounds =
            mutableListOf<Bound>()

        var indexOfKeyword =
            formattedBodyLowerCase.indexOf(
                pattern
            )

        while (indexOfKeyword > -1) {

            val rightBound =
                indexOfKeyword +
                        pattern.length

            bounds.add(
                Bound(
                    indexOfKeyword,
                    rightBound
                )
            )

            indexOfKeyword =
                formattedBodyLowerCase.indexOf(
                    pattern,
                    rightBound
                )
        }

        if (
            bounds.isNotEmpty()
        ) {

            httpCallBodyView
                .highlightBounds(bounds)
        }
    }

    private fun getBodyToFormat(
        httpCallRecord: HttpCallRecord
    ): String? {

        return when (mode) {

            ERROR_MODE -> {
                httpCallRecord.error
            }

            REQUEST_MODE -> {
                httpCallRecord.payload
            }

            else -> {
                httpCallRecord.responseBody
            }
        }
    }

    private fun getFormatter(
        httpCallRecord: HttpCallRecord
    ): ResponseFormatter {

        val contentTypeHeader =
            getContentTypeHeader(
                httpCallRecord
            )
                ?: return PlainTextFormatter()

        val headerValue =
            contentTypeHeader.values
                .firstOrNull()
                ?: return PlainTextFormatter()

        return formatterFactory.getFor(
            headerValue.value
        )
    }

    private fun getContentTypeHeader(
        httpCall: HttpCallRecord
    ): HttpHeader? {

        return if (
            mode == REQUEST_MODE
        ) {

            httpCall.getRequestHeader(
                CONTENT_TYPE
            )

        } else {

            httpCall.getResponseHeader(
                CONTENT_TYPE
            )
        }
    }
}
