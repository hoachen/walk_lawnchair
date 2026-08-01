package com.android.launcher3.ads.white

import android.app.Activity
import com.android.launcher3.ads.launcher.AdManager
import com.android.launcher3.ads.launcher.AdPlacement

/**
 * Reusable equivalent of the reference `AdDialogGate`: gate a product action behind configured
 * fullscreen inventory, while guaranteeing that the action still runs when advertising is off.
 */
class AdDialogGate(
    private val activity: Activity,
    private val placements: List<AdPlacement> = listOf(AdPlacement.DIALOG_GATE_FULLSCREEN),
) {
    fun runAfterAd(action: Runnable) {
        AdManager.showThenGroup(activity, placements, action)
    }
}
