package com.neomud.client.platform

actual object PlatformLogger {
    actual fun d(tag: String, message: String) {
        console.log("D/$tag: $message")
    }
    actual fun w(tag: String, message: String) {
        console.warn("W/$tag: $message")
    }
    actual fun w(tag: String, message: String, throwable: Throwable) {
        console.warn("W/$tag: $message: ${throwable.message}")
    }
    actual fun e(tag: String, message: String) {
        console.error("E/$tag: $message")
    }
    actual fun e(tag: String, message: String, throwable: Throwable) {
        console.error("E/$tag: $message: ${throwable.message}")
    }
}

private external object console {
    fun log(message: String)
    fun warn(message: String)
    fun error(message: String)
}
