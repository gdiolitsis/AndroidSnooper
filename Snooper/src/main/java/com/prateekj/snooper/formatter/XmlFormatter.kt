package com.prateekj.snooper.formatter

import com.prateekj.snooper.utils.Logger
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import javax.xml.transform.OutputKeys
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.stream.StreamResult

class XmlFormatter :
    ResponseFormatter {

    override fun format(
        response: String
    ): String {

        if (response.isBlank()) {
            return response
        }

        return try {

            val transformer =
                SAXTransformerFactory
                    .newInstance()
                    .newTransformer()
                    .apply {

                        setOutputProperty(
                            OutputKeys.INDENT,
                            "yes"
                        )

                        setOutputProperty(
                            OutputKeys.OMIT_XML_DECLARATION,
                            "yes"
                        )

                        setOutputProperty(
                            "{http://xml.apache.org/xslt}indent-amount",
                            "2"
                        )
                    }

            val inputStream =
                ByteArrayInputStream(
                    response.toByteArray(
                        StandardCharsets.UTF_8
                    )
                )

            val outputStream =
                ByteArrayOutputStream()

            val xmlSource =
                SAXSource(
                    InputSource(inputStream)
                )

            val result =
                StreamResult(outputStream)

            transformer.transform(
                xmlSource,
                result
            )

            outputStream.toString(
                StandardCharsets.UTF_8.name()
            ).trim()

        } catch (e: Exception) {

            Logger.e(
                TAG,
                e.message ?: "XML formatting error",
                e
            )

            response
        }
    }

    companion object {

        private val TAG =
            XmlFormatter::class.java.simpleName
    }
}
