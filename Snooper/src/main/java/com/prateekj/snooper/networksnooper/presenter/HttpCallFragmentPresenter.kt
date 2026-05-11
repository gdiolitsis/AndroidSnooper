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

    private var formattedBodyLowerCase =
        ""

    fun init(
        viewModel: HttpBodyViewModel,
        mode: Int
    ) {

        this.mode = mode

        executor.execute(
            object : BackgroundTask<String> {

                override fun onExecute():
                        String {

                    val httpCallRecord =
                        repo.findById(
                            httpCallId
                        )

                    val formatter =
                        getFormatter(
                            httpCallRecord
                        )

                    val bodyToFormat =
                        getBodyToFormat(
                            httpCallRecord
                        ).orEmpty()

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

                    viewModel.init(
                        result
                    )

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

        val normalizedPattern =
            pattern.trim()

        if (
            normalizedPattern.isEmpty()
        ) {
            return
        }

        val lowerPattern =
            normalizedPattern.lowercase(
                Locale.getDefault()
            )

        val bounds =
            mutableListOf<Bound>()

        var startIndex = 0

        while (true) {

            val indexOfKeyword =
                formattedBodyLowerCase.indexOf(
                    lowerPattern,
                    startIndex
                )

            if (indexOfKeyword < 0) {
                break
            }

            val rightBound =
                indexOfKeyword +
                        lowerPattern.length

            bounds.add(
                Bound(
                    indexOfKeyword,
                    rightBound
                )
            )

            startIndex =
                rightBound
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

        val headerValue =
            contentTypeHeader
                ?.values
                ?.firstOrNull()
                ?.value
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return PlainTextFormatter()

        return formatterFactory.getFor(
            headerValue
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
