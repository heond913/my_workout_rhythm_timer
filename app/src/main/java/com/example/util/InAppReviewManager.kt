package com.example.util

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

object InAppReviewManager {
    private const val TAG = "InAppReviewManager"

    /**
     * Request and launch the In-App Review flow.
     * Guaranteed to call [onComplete] regardless of success or failure.
     */
    fun requestAndLaunchReview(activity: Activity, onComplete: () -> Unit) {
        Log.d(TAG, "Initiating In-App Review flow...")
        try {
            val manager = ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    Log.d(TAG, "ReviewInfo obtained successfully. Launching review flow...")
                    val flow = manager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener { _ ->
                        Log.d(TAG, "In-App Review flow finished.")
                        onComplete()
                    }
                } else {
                    Log.e(TAG, "Failed to request ReviewInfo", task.exception)
                    onComplete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during In-App Review flow", e)
            onComplete()
        }
    }
}
