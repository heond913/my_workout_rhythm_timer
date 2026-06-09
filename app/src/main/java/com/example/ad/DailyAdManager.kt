package com.example.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DailyAdManager implements the "Strictly 1-Ad Per Day" strategy for workout completion.
 * It manages preloading, persistence of the shown date via SharedPreferences,
 * and presenting the interstitial ad to the user safely with no disruption to the workflow.
 */
class DailyAdManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    companion object {
        private const val TAG = "DailyAdManager"
        // Official AdMob Test Interstitial Ad Unit ID
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val PREFS_NAME = "workout_rhythm_prefs"
        private const val KEY_LAST_SHOWN_DATE = "last_ad_shown_date_yyyymmdd"

        @Volatile
        private var INSTANCE: DailyAdManager? = null

        /**
         * Thread-safe singleton retrieval method.
         */
        fun getInstance(context: Context): DailyAdManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DailyAdManager(context).also { INSTANCE = it }
            }
        }
    }

    init {
        // Initialize Mobile Ads SDK and automatically load initial ad
        MobileAds.initialize(appContext) {
            Log.d(TAG, "AdMob MobileAds initialized successfully.")
            preloadAd()
        }
    }

    /**
     * Preloads the Interstitial Ad in a thread-safe and non-blocking manner.
     */
    @Synchronized
    fun preloadAd() {
        if (interstitialAd != null || isAdLoading) {
            Log.d(TAG, "Ad already loaded or loading in progress. Skipping load.")
            return
        }

        // Check if ad has already shown today. If so, avoid wasting bandwidth preloading!
        if (hasShownAdToday()) {
            Log.d(TAG, "Ad already shown today. Skipping preloading to optimize resources.")
            return
        }

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()

        Log.d(TAG, "Starting interstitial ad load request.")
        InterstitialAd.load(
            appContext,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    synchronized(this@DailyAdManager) {
                        interstitialAd = ad
                        isAdLoading = false
                    }
                    Log.d(TAG, "Interstitial ad loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    synchronized(this@DailyAdManager) {
                        interstitialAd = null
                        isAdLoading = false
                    }
                    Log.e(TAG, "Interstitial ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Compares the current device date with the stored date in SharedPreferences.
     */
    private fun hasShownAdToday(): Boolean {
        // Skip check in debug/development mode to allow continuous testing
        if (com.example.BuildConfig.DEBUG) {
            Log.d(TAG, "hasShownAdToday: Debug build detected, bypassing daily limit for testing.")
            return false
        }
        val sharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastShownDate = sharedPreferences.getString(KEY_LAST_SHOWN_DATE, "")
        val todayDate = getTodayDateString()
        val hasShown = lastShownDate == todayDate
        Log.d(TAG, "hasShownAdToday: Current date = $todayDate, Last shown date = $lastShownDate, result = $hasShown")
        return hasShown
    }

    /**
     * Saves today's date in SharedPreferences to mark that the ad has been shown today.
     */
    private fun saveAdShownToday() {
        val sharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayDate = getTodayDateString()
        sharedPreferences.edit()
            .putString(KEY_LAST_SHOWN_DATE, todayDate)
            .apply()
        Log.d(TAG, "Saved ad shown date: $todayDate")
    }

    /**
     * Returns today's date formatted as "yyyyMMdd".
     */
    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        return sdf.format(Date())
    }

    /**
     * Shows the interstitial ad to the user on workout completion.
     * Crucially, the custom `onCompleteAction` must ALWAYS be invoked regardless of whether
     * the ad was shown, failed to show, or was already shown, ensuring the user's flow is never blocked.
     *
     * @param activity The Activity context in which to display the ad (uses WeakReference to prevent leaks).
     * @param onCompleteAction Callback to execute after the ad is dismissed or if it cannot be shown.
     */
    fun showDailyWorkoutAd(activity: Activity, onCompleteAction: () -> Unit) {
        val activityRef = WeakReference(activity)

        // Ensure we operate on the Main/UI thread for UI/Ad interactions
        activity.runOnUiThread {
            val hostActivity = activityRef.get()
            if (hostActivity == null || hostActivity.isFinishing || hostActivity.isDestroyed) {
                Log.e(TAG, "Host activity is no longer valid. Invoking onCompleteAction immediately.")
                onCompleteAction()
                return@runOnUiThread
            }

            // Option A: Ad has already been shown today
            if (hasShownAdToday()) {
                Log.d(TAG, "Ad already shown today. Proceeding to action directly.")
                onCompleteAction()
                return@runOnUiThread
            }

            val adToShow: InterstitialAd?
            synchronized(this) {
                adToShow = interstitialAd
                interstitialAd = null // Consuming reference
            }

            // Option B: Ad is available to be shown
            if (adToShow != null) {
                Log.d(TAG, "Ad is loaded. Attaching callbacks and presenting.")
                adToShow.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad dismissed by user. Saving state and preloading next day.")
                        saveAdShownToday()
                        onCompleteAction()
                        preloadAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e(TAG, "Ad failed to show: ${adError.message}. Proceeding to action directly.")
                        onCompleteAction()
                        preloadAd()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad is being displayed on screen.")
                    }
                }
                adToShow.show(hostActivity)
            } else {
                // Option C: Ad is null, failed to load, or not ready yet
                Log.d(TAG, "Ad is not ready or failed to load. Proceeding to action instantly.")
                onCompleteAction()
                preloadAd()
            }
        }
    }
}
