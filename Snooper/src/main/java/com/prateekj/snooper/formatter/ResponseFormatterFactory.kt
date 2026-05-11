package com.prateekj.snooper.formatter

import java.util.Locale

class ResponseFormatterFactory {

    fun getFor(
        data: String
    ): ResponseFormatter {

        return when {

            isXmlType(data) -> {

                XmlFormatter()
            }

            isJsonType(data) -> {

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

        return data.lowercase(
            Locale.US
        ).contains("xml")
    }

    private fun isJsonType(
        data: String
    ): Boolean {

        return data.lowercase(
            Locale.US
        ).contains("json")
    }
}
