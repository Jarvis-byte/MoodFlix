package com.arka.moodflix

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import com.arka.moodflix.ui.navigation.MoodFlixNavHost
import com.arka.moodflix.ui.theme.MoodFlixTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MoodFlixTheme {
                MoodFlixNavHost(onOpenUrl = ::openUrl)
            }
        }
    }

    /**
     * Custom Tabs rather than a WebView: the user keeps their existing
     * Google/YouTube session, which matters both for trailers and for the
     * "get a key" flow on AI Studio.
     */
    private fun openUrl(url: String) {
        runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(this, Uri.parse(url))
        }
    }
}
