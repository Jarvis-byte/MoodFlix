package com.arka.moodflix.core

import android.util.Log

actual object Logger {
    actual fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    actual fun w(tag: String, message: String) {
        Log.w(tag, message)
    }
}
