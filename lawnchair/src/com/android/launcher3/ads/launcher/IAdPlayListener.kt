package com.android.launcher3.ads.launcher

/** Callback for fullscreen and native-ad playback. */
interface IAdPlayListener {
    fun onShown() = Unit
    fun onDismissed() = Unit
    fun onFailed(reason: String? = null) = Unit
}
