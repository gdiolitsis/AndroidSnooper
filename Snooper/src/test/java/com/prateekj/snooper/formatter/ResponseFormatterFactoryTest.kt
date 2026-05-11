package com.prateekj.snooper.formatter

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResponseFormatterFactoryTest {

    private lateinit var factory:
            ResponseFormatterFactory

    @Before
    fun setUp() {

        factory =
            ResponseFormatterFactory()
    }

    @Test
    fun shouldReturnXmlFormatter() {

        assertTrue(
            factory.getFor(
                "application/xml"
            ) is XmlFormatter
        )
    }

    @Test
    fun shouldReturnJsonFormatter() {

        assertTrue(
            factory.getFor(
                "application/json"
            ) is JsonResponseFormatter
        )
    }

    @Test
    fun shouldReturnPlainTextFormatter() {

        assertTrue(
            factory.getFor(
                "plain/text"
            ) is PlainTextFormatter
        )
    }

    @Test
    fun shouldReturnXmlFormatterForMixedCase() {

        assertTrue(
            factory.getFor(
                "Application/XML"
            ) is XmlFormatter
        )
    }

    @Test
    fun shouldReturnJsonFormatterForMixedCase() {

        assertTrue(
            factory.getFor(
                "Application/JSON"
            ) is JsonResponseFormatter
        )
    }
}
