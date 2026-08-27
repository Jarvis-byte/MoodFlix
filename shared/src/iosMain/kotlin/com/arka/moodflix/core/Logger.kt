package com.arka.moodflix.core

import platform.Foundation.NSLog

actual object Logger {
    actual fun d(tag: String, message: String) {
        NSLog("[$tag] $message")
    }

    actual fun w(tag: String, message: String) {
        NSLog("[$tag] WARN $message")
    }
}
