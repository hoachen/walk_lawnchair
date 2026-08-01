package com.android.launcher3.ads.npa

import android.app.Activity
import com.android.launcher3.ads.launcher.AdPlacement

/** Optional product policy for remote flags, frequency caps and placement eligibility. */
fun interface AdRequestPolicy {
    fun canRequest(activity: Activity, placement: AdPlacement): Boolean
}
