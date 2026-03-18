package com.neomud.client.platform

actual object PlatformLogger {
    actual fun d(tag: String, message: String) {
        println("D/$tag: $message")
    }

    actual fun w(tag: String, message: String) {
        println("W/$tag: $message")
    }

    actual fun w(tag: String, message: String, throwable: Throwable) {
        println("W/$tag: $message: ${throwable.message}")
    }

    actual fun e(tag: String, message: String) {
        println("E/$tag: $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable) {
        println("E/$tag: $message: ${throwable.message}")
    }
}
