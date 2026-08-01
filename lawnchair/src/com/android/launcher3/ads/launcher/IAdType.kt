package com.android.launcher3.ads.launcher

/**
 * Advertising formats supported by the Lawnchair ad module.
 *
 * These are format capabilities, not placement IDs. The host app maps each placement to an ad
 * unit in [AdConfiguration].
 */
sealed class IAdType(val key: String) {
    data object Interstitial : IAdType("interstitial")
    data object Rewarded : IAdType("rewarded")
    data object AppOpen : IAdType("app_open")
    data object Native : IAdType("native")
}
