package com.android.launcher3.ads.npa

import android.app.Activity

/**
 * Privacy/consent boundary for an external advertising SDK.
 *
 * The product app supplies its CMP implementation before enabling ads. Lawnchair never assumes
 * that personalised-ad consent has been granted.
 */
fun interface ConsentManager {
    fun canRequestAds(activity: Activity): Boolean
}
