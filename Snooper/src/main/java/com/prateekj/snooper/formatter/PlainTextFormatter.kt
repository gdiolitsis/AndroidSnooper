package com.prateekj.snooper.formatter

class PlainTextFormatter :
    ResponseFormatter {

    override fun format(
        response: String
    ): String {

        if (response.isEmpty()) {
            return response
        }

        return response

            .replace(
                "\r\n",
                "\n"
            )

            .replace(
                "\r",
                "\n"
            )

            .replace(
                "\n",
                System.lineSeparator()
            )
    }
}
