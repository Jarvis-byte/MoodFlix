package com.arka.moodflix.core

/** android.util.Log isn't available outside Android, hence this indirection. */
expect object Logger {
    fun d(tag: String, message: String)
    fun w(tag: String, message: String)
}
