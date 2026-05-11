package com.prateekj.snooper.networksnooper.presenter

import com.prateekj.snooper.infra.BackgroundTask
import com.prateekj.snooper.infra.BackgroundTaskExecutor
import com.prateekj.snooper.networksnooper.activity.HttpCallTab
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.ERROR
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.HEADERS
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.REQUEST
import com.prateekj.snooper.networksnooper.activity.HttpCallTab.RESPONSE
import com.prateekj.snooper.networksnooper.helper.DataCopyHelper
import com.prateekj.snooper.networksnooper.model.HttpCallRecord
import com.prateekj.snooper.networksnooper.views.HttpCallView
import com.prateekj.snooper.utils.FileUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale.US

class HttpCallPresenter(
    private val dataCopyHelper:
            DataCopyHelper,
    private val httpCallRecord:
            HttpCallRecord,
    private val view:
            HttpCallView,
    private val fileUtil:
            FileUtil,
    private val executor:
            BackgroundTaskExecutor
) {

    private val logFileName:
            String
        get() {

            val dateFormat =
                SimpleDateFormat(
                    "yyyy_MM_dd_HH_mm_ss",
                    US
                )

            val safeDate =
                httpCallRecord.date
                    ?: Date()

            return "${dateFormat.format(safeDate)}.txt"
        }

    fun copyHttpCallBody(
        httpCallTab: HttpCallTab
    ) {

        val text =
            getTextToCopy(
                httpCallTab
            )

        if (text.isNotBlank()) {

            view.copyToClipboard(
                text
            )
        }
    }

    fun shareHttpCallBody() {

        val completeHttpCallData =
            dataCopyHelper.getHttpCallData()

        if (
            completeHttpCallData.isBlank()
        ) {

            view.showMessageShareNotAvailable()

            return
        }

        val fileName =
            logFileName

        executor.execute(
            object : BackgroundTask<String> {

                override fun onExecute():
                        String {

                    return try {

                        fileUtil.createLogFile(
                            completeHttpCallData,
                            fileName
                        )

                    } catch (_: Exception) {

                        ""
                    }
                }

                override fun onResult(
                    result: String
                ) {

                    if (
                        result.isNotBlank()
                    ) {

                        view.shareData(
                            result
                        )

                    } else {

                        view.showMessageShareNotAvailable()
                    }
                }
            }
        )
    }

    fun onPermissionDenied() {

        view.showMessageShareNotAvailable()
    }

    private fun getTextToCopy(
        httpCallTab: HttpCallTab
    ): String {

        return when (httpCallTab) {

            RESPONSE -> {

                dataCopyHelper
                    .getResponseDataForCopy()
            }

            REQUEST -> {

                dataCopyHelper
                    .getRequestDataForCopy()
            }

            HEADERS -> {

                dataCopyHelper
                    .getHeadersForCopy()
            }

            ERROR -> {

                dataCopyHelper
                    .getErrorsForCopy()
            }
        }
    }
}
