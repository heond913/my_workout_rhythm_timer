package com.example.ad

import android.app.Activity
import android.content.Context
import android.os.SystemClock
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Interface representing the Workout Ad Manager.
 * Facilitates decoupling, unit-testing (via Mocking/Faking), and 
 * clean dependency injection (DI) integrations.
 */
interface WorkoutAdManager {
    /**
     * Preloads the Interstitial Ad in a thread-safe, asynchronous manner.
     */
    fun preloadAd()

    /**
     * Triggers the showing of the Daily Workout Ad if the user has not seen one today.
     * Guarantees that the critical callback [onCompleteAction] is executed under all circumstances.
     */
    fun showDailyWorkoutAd(activity: Activity, onCompleteAction: () -> Unit)
}

/**
 * DailyAdManager implements the "Strictly 1-Ad Per Day" strategy for workout completion.
 * It encapsulates preloading, disk I/O off-threading via Kotlin Coroutines, and local-only clock 
 * manipulation defenses on top of AdMob SDK integration.
 */
class DailyAdManager private constructor(context: Context) : WorkoutAdManager {

    private val appContext: Context = context.applicationContext
    
    // Explicit private lock object to avoid lock pollution or accidental external locks
    private val adLock = Any()
    
    // Core states protected by [adLock]
    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    // SupervisorJob ensures failure of one coroutine doesn't kill the entire scope
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val TAG = "DailyAdManager"
        
        // Official AdMob Test Interstitial Ad Unit ID
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val PREFS_NAME = "workout_rhythm_prefs"
        
        // Preference key mappings
        private const val KEY_LAST_SHOWN_DATE = "last_ad_shown_date_yyyymmdd"
        private const val KEY_LAST_SHOWN_TIMESTAMP = "last_ad_shown_timestamp_millis"
        private const val KEY_MAX_VERIFIED_TIMESTAMP = "last_verified_system_millis"

        @Volatile
        private var INSTANCE: DailyAdManager? = null

        /**
         * Thread-safe singleton retriever using the Double-Checked Locking pattern.
         */
        fun getInstance(context: Context): DailyAdManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DailyAdManager(context).also { INSTANCE = it }
            }
        }
    }

    init {
        // Run AdMob initialize and preloading flow in the background scope to avoid blocking main thread start
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                MobileAds.initialize(appContext) {
                    Log.d(TAG, "AdMob MobileAds initialized successfully.")
                    preloadAd()
                }
            }
        }
    }

    /**
     * Preloads the Interstitial Ad in a thread-safe and non-blocking background context.
     */
    override fun preloadAd() {
        coroutineScope.launch {
            // First check fast lock states
            val shouldSkip = synchronized(adLock) {
                interstitialAd != null || isAdLoading
            }
            if (shouldSkip) {
                Log.d(TAG, "Ad already loaded or loading in progress. Skipping load.")
                return@launch
            }

            // Check SharedPreferences on IO thread
            val alreadyShownToday = withContext(Dispatchers.IO) {
                hasShownAdToday()
            }
            
            if (alreadyShownToday) {
                Log.d(TAG, "Ad already shown today. Skipping preloading to optimize resources.")
                return@launch
            }

            synchronized(adLock) {
                isAdLoading = true
            }

            // Move back to Main Thread since Google AdMob SDK expects loading calls strictly on Main Thread
            withContext(Dispatchers.Main) {
                val adRequest = AdRequest.Builder().build()
                Log.d(TAG, "Starting interstitial ad load request.")
                
                InterstitialAd.load(
                    appContext,
                    AD_UNIT_ID,
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            synchronized(adLock) {
                                interstitialAd = ad
                                isAdLoading = false
                            }
                            Log.d(TAG, "Interstitial ad loaded successfully.")
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            synchronized(adLock) {
                                interstitialAd = null
                                isAdLoading = false
                            }
                            Log.e(TAG, "Interstitial ad failed to load: ${loadAdError.message}")
                        }
                    }
                )
            }
        }
    }

    /**
     * Determines whether the ad has been shown today by reading SharedPreferences.
     * Integrates clock spoofing bypass to prevent users from changing device dates to reset limits.
     * MUST be called from a background thread (e.g., [Dispatchers.IO]) to avoid ANRs.
     */
    private fun hasShownAdToday(): Boolean {
        // Skip check in debug/development mode to allow continuous, repetitive QA testing
        if (com.example.BuildConfig.DEBUG) {
            Log.d(TAG, "hasShownAdToday: Debug build detected. Bypassing daily limit for QA.")
            return false
        }

        val sharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastShownDate = sharedPreferences.getString(KEY_LAST_SHOWN_DATE, "")
        val todayDate = getTodayDateString()
        
        // Clock manipulation mitigation check
        if (isClockManipulated(sharedPreferences)) {
            Log.w(TAG, "hasShownAdToday: Detected clock manipulation. Enforcing ad lock state.")
            return true // Fallback to safe lock state (treat as shown)
        }

        val hasShown = lastShownDate == todayDate
        Log.d(TAG, "hasShownAdToday: Current date = $todayDate, Last shown date = $lastShownDate, result = $hasShown")
        return hasShown
    }

    /**
     * Saves today's tracking indices (Date string, Millisecond Timestamp) to SharedPreferences.
     * MUST be called from a background thread (e.g., [Dispatchers.IO]) to avoid blocking UI.
     */
    private fun saveAdShownToday() {
        val sharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayDate = getTodayDateString()
        val currentMillis = System.currentTimeMillis()

        sharedPreferences.edit()
            .putString(KEY_LAST_SHOWN_DATE, todayDate)
            .putLong(KEY_LAST_SHOWN_TIMESTAMP, currentMillis)
            .putLong(KEY_MAX_VERIFIED_TIMESTAMP, currentMillis)
            .apply()

        Log.d(TAG, "Saved ad completion markers: Date=$todayDate, Timestamp=$currentMillis")
    }

    /**
     * Combats local device time manipulation (rolling clock back or forward) to cheat ad timers.
     */
    private fun isClockManipulated(sharedPreferences: android.content.SharedPreferences): Boolean {
        val currentMillis = System.currentTimeMillis()
        val lastShownTimestamp = sharedPreferences.getLong(KEY_LAST_SHOWN_TIMESTAMP, 0L)
        val maxVerifiedTimestamp = sharedPreferences.getLong(KEY_MAX_VERIFIED_TIMESTAMP, 0L)

        // Rule A: Current system epoch milliseconds are less than the last known ad presentation timestamp 
        // OR the peak historical recorded timestamp on this device (indicating a backward time roll).
        if (currentMillis < lastShownTimestamp || currentMillis < maxVerifiedTimestamp) {
            Log.e(TAG, "Clock Spoof Warning: Current device time is older than the verified baseline.")
            return true
        }

        // Rule B: Always update the maximum observed verified timestamp to prevent forward/backward hopping.
        sharedPreferences.edit()
            .putLong(KEY_MAX_VERIFIED_TIMESTAMP, currentMillis)
            .apply()

        return false
    }

    /**
     * Returns today's date formatted as "yyyyMMdd".
     */
    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        return sdf.format(Date())
    }

    /**
     * Triggers presentation of the interstitial ad.
     * Checks are processed asynchronously; main thread operations are restricted strictly 
     * to presentation and final execution callbacks to protect UI performance and prevent any blockages.
     *
     * @param activity Strong context reference converted safely to WeakReference inside scope.
     * @param onCompleteAction Invoked regardless of whether ad presents or fails – UX is completely seamless.
     */
    override fun showDailyWorkoutAd(activity: Activity, onCompleteAction: () -> Unit) {
        val activityRef = WeakReference(activity)

        // Launch task asynchronously
        coroutineScope.launch {
            // Step 1: Query database / storage on IO thread
            val alreadyShown = withContext(Dispatchers.IO) {
                hasShownAdToday()
            }

            if (alreadyShown) {
                Log.d(TAG, "Workout ad daily limit reached or clock spoofed. Skipping presentation.")
                withContext(Dispatchers.Main) {
                    onCompleteAction()
                }
                return@launch
            }

            // Step 2: Extract load reference from private synchronization block
            val adToShow = synchronized(adLock) {
                val ad = interstitialAd
                interstitialAd = null // Consumed immediately to prevent duplicate triggers
                ad
            }

            // Step 3: Shift presentation back to Main Thread safely
            withContext(Dispatchers.Main) {
                val hostActivity = activityRef.get()
                if (hostActivity == null || hostActivity.isFinishing || hostActivity.isDestroyed) {
                    Log.e(TAG, "Host Activity state invalid. Executing completion callback directly.")
                    onCompleteAction()
                    return@withContext
                }

                if (adToShow != null) {
                    Log.d(TAG, "Ad loaded and ready. Rendering.")
                    adToShow.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Ad dismissed by user. Writing state and preloading tomorrow's ad.")
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    saveAdShownToday()
                                }
                                withContext(Dispatchers.Main) {
                                    onCompleteAction()
                                }
                                preloadAd()
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Ad failed to present: ${adError.message}. Proceeding instantly.")
                            onCompleteAction()
                            preloadAd()
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.i(TAG, "Ad successfully displayed.")
                        }
                    }
                    adToShow.show(hostActivity)

                } else {
                    // Option C: Ad is null, failed loading or is currently in-transit
                    Log.d(TAG, "Ad reference is null or not loaded yet. Discharging completion callback seamlessly.")
                    onCompleteAction()
                    preloadAd()
                }
            }
        }
    }
}
