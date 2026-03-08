package com.neomud.client.platform

import org.slf4j.LoggerFactory

actual object PlatformLogger {
    private fun logger(tag: String) = LoggerFactory.getLogger(tag)

    actual fun d(tag: String, message: String) {
        logger(tag).debug(message)
    }

    actual fun w(tag: String, message: String) {
        logger(tag).warn(message)
    }

    actual fun w(tag: String, message: String, throwable: Throwable) {
        logger(tag).warn(message, throwable)
    }

    actual fun e(tag: String, message: String) {
        logger(tag).error(message)
    }

    actual fun e(tag: String, message: String, throwable: Throwable) {
        logger(tag).error(message, throwable)
    }
}
