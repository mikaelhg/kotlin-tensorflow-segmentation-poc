package io.mikael.poc

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType

internal val TEXT_HTML_UTF8: MediaType = MediaType.parseMediaType("text/html; charset=utf-8")

internal inline fun <reified T : Any> T.getClassLogger(): Logger = LoggerFactory.getLogger(T::class.java)
