package com.prateekj.snooper.formatter

import java.util.Locale

class ResponseFormatterFactory {

    fun getFor(
        data: String?
    ): ResponseFormatter {

        val normalizedData =
            data
                ?.trim()
                ?.lowercase(Locale.US)
                ?: ""

        return when {

            isXmlType(normalizedData) -> {

                XmlFormatter()
            }

            isJsonType(normalizedData) -> {

                JsonResponseFormatter()
            }

            else -> {

                PlainTextFormatter()
            }
        }
    }

    private fun isXmlType(
        data: String
    ): Boolean {

        return data.contains("xml")
    }

    private fun isJsonType(
        data: String
    ): Boolean {

        return data.contains("json")
    }
}
