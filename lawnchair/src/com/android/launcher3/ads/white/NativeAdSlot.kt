package com.android.launcher3.ads.white

import android.view.View
import android.view.ViewGroup

/** Utilities shared by native-ad hosts; providers remain responsible for their SDK resources. */
object NativeAdSlot {
    fun clear(container: ViewGroup) {
        container.removeAllViews()
        container.visibility = View.GONE
    }
}
