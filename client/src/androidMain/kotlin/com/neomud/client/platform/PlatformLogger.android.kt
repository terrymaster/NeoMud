package com.neomud.client.platform

import android.util.Log

actual object PlatformLogger {
    actual fun d(tag: String, message: String) { Log.d(tag, message) }
    actual fun w(tag: String, message: String) { Log.w(tag, message) }
    actual fun w(tag: String, message: String, throwable: Throwable) { Log.w(tag, message, throwable) }
    actual fun e(tag: String, message: String) { Log.e(tag, message) }
    actual fun e(tag: String, message: String, throwable: Throwable) { Log.e(tag, message, throwable) }
}
