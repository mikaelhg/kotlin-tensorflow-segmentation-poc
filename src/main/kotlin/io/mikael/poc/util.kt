package io.mikael.poc

import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal inline fun <reified T : Any> T.getClassLogger(): Logger = LoggerFactory.getLogger(T::class.java)
