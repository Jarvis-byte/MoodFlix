package com.arka.moodflix.core.ads

import android.app.Activity
import android.content.Context
import com.arka.moodflix.domain.repository.AdsPreferenceRepository
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Loads a rewarded interstitial ad ahead of time and shows it on demand.
 * The ad's own countdown/close UI is Google's - once it dismisses (watched
 * in full, skipped, or failed to show), [showOrSkip] hands control back to
 * the caller so it can proceed regardless of outcome.
 *
 * Whether ads run at all is gated per-user by [AdsPreferenceRepository]
 * (backed by Firestore), so a specific user's ads can be turned off from
 * the console - e.g. for a paid tier - without a release.
 */
@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adsPreferenceRepository: AdsPreferenceRepository
) {

    // Main.immediate: showOrSkip's callbacks (onAdDismissed, onComplete) must
    // land back on the UI thread since they drive Compose state directly.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var rewardedAd: RewardedInterstitialAd? = null
    private var isLoading = false

    init {
        preload()
    }

    fun preload() {
        scope.launch {
            if (rewardedAd != null || isLoading) return@launch
            if (!adsPreferenceRepository.isAdsEnabledForCurrentUser()) return@launch
            isLoading = true
            RewardedInterstitialAd.load(
                context,
                AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        isLoading = false
                        rewardedAd = ad
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoading = false
                        rewardedAd = null
                    }
                }
            )
        }
    }

    /**
     * Shows the ad if the user's flag allows it and one is ready; otherwise
     * skips straight to [onComplete] rather than blocking the user.
     */
    fun showOrSkip(activity: Activity, onComplete: () -> Unit) {
        scope.launch {
            if (!adsPreferenceRepository.isAdsEnabledForCurrentUser()) {
                onComplete()
                return@launch
            }

            val ad = rewardedAd
            if (ad == null) {
                preload()
                onComplete()
                return@launch
            }

            rewardedAd = null
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    preload()
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    preload()
                    onComplete()
                }
            }
            ad.show(activity) { /* reward earned; loadMore() runs on dismiss either way */ }
        }
    }

    private companion object {
        const val AD_UNIT_ID = "ca-app-pub-9247188440103276/4817449031"
    }
}
