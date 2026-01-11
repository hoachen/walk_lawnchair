package com.ur.apps.ad

import com.ur.apps.ad.admob.NativeAdCardType

/**
 * Configuration for launcher ad display strategy
 */
object LauncherAdConfig {
    // Show an ad every N apps in the app list
    const val ALL_APPS_AD_INTERVAL = 16

    // Whether to show ads in folders
    const val SHOW_AD_IN_FOLDER = true

    // Minimum number of items in folder to show ad
    const val FOLDER_MIN_ITEMS_FOR_AD = 3
}
