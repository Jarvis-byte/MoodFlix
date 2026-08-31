package com.arka.moodflix.core.ads

import android.app.Activity
import android.content.Context
import com.arka.moodflix.core.analytics.AnalyticsEvent
import com.arka.moodflix.core.analytics.AnalyticsManager
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
 * The ad's own countdown/close UI is Google's - once it dismisses, [showOrSkip]
 * hands control back to the caller with whether the reward was actually
 * earned, so the caller can gate what the reward unlocks on that instead of
 * proceeding unconditionally.
 *
 * A technical failure (no ad ready yet, or the ad failing to render) is
 * *not* the user's fault and reports as earned=true so it doesn't block
 * them - only actually watching the ad and closing it early counts as
 * declining the reward.
 *
 * Whether ads run at all is gated per-user by [AdsPreferenceRepository]
 * (backed by Firestore), so a specific user's ads can be turned off from
 * the console - e.g. for a paid tier - without a release.
 */
@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adsPreferenceRepository: AdsPreferenceRepository,
    private val analytics: AnalyticsManager
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
     * Shows the ad if the user's flag allows it and one is ready. [onResult]
     * receives whether the reward was earned - false only when an ad was
     * actually shown and the user closed it before it finished; every other
     * path (ads disabled for this user, no ad ready, ad failed to render)
     * reports true so a technical hiccup never blocks the user.
     */
    fun showOrSkip(activity: Activity, onResult: (rewardEarned: Boolean) -> Unit) {
        scope.launch {
            if (!adsPreferenceRepository.isAdsEnabledForCurrentUser()) {
                onResult(true)
                return@launch
            }

            val ad = rewardedAd
            if (ad == null) {
                preload()
                onResult(true)
                return@launch
            }

            rewardedAd = null
            var rewardEarned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    analytics.log(
                        if (rewardEarned) {
                            AnalyticsEvent.RewardedAdCompleted
                        } else {
                            AnalyticsEvent.RewardedAdSkippedOrFailed
                        }
                    )
                    preload()
                    onResult(rewardEarned)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    analytics.log(AnalyticsEvent.RewardedAdSkippedOrFailed)
                    preload()
                    onResult(true)
                }
            }
            analytics.log(AnalyticsEvent.RewardedAdShown)
            ad.show(activity) { rewardEarned = true }
        }
    }

    private companion object {
        const val AD_UNIT_ID = "ca-app-pub-9247188440103276/4817449031"
    }
}
