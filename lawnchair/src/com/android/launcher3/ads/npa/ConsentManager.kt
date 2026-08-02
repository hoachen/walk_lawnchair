package com.android.launcher3.ads.npa

import android.app.Activity
import com.android.launcher3.ads.launcher.AdPlacement

/**
 * Privacy/consent boundary for an external advertising SDK.
 *
 * The product app supplies its CMP implementation before enabling ads. Lawnchair never assumes
 * that personalised-ad consent has been granted.
 */
fun interface ConsentManager {
    fun canRequestAds(activity: Activity): Boolean
}

/**
 * Per-placement request privacy settings supplied by the product App. Lawnchair does not know an
 * ad network's extras format; the provider translates this value (for AdMob, NPA is `npa=1`).
 */
data class AdRequestPrivacy(
    val nonPersonalizedAds: Boolean = true,
)

fun interface AdRequestPrivacyProvider {
    fun get(placement: AdPlacement): AdRequestPrivacy
}
