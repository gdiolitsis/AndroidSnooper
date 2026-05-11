package com.prateekj.snooper.formatter

import com.prateekj.snooper.utils.Logger
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

class JsonResponseFormatter : ResponseFormatter {

    override fun format(
        response: String
    ): String {

        return try {

            when (
                val json =
                    JSONTokener(response)
                        .nextValue()
            ) {

                is JSONObject -> {

                    json.toString(
                        INDENT_SPACES
                    )
                }

                is JSONArray -> {

                    json.toString(
                        INDENT_SPACES
                    )
                }

                else -> {

                    response
                }
            }

        } catch (e: JSONException) {

            Logger.e(
                TAG,
                e.message,
                e
            )

            response
        }
    }

    companion object {

        private val TAG =
            JsonResponseFormatter::class.java.simpleName

        private const val INDENT_SPACES =
            4
    }
}
