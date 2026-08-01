package com.android.launcher3.ads.launcher

import android.app.Activity
import android.app.Application
import android.view.ViewGroup

/**
 * SDK adapter owned by the integrating product application.
 *
 * Lawnchair intentionally has no dependency on an ad network. An AdMob/mediation implementation
 * belongs in the product module and implements this interface.
 */
interface IAdProvider {
    /**
     * Initialise the SDK once. Read [AdConfiguration.appId] here; do not embed it in Lawnchair.
     * Call [listener] exactly once, on the main thread, when SDK initialisation succeeds or fails.
     */
    fun initialize(application: Application, configuration: AdConfiguration, listener: InitListener)

    /** Return true only if an already loaded ad can be shown immediately for this placement. */
    fun isReadyAd(type: IAdType, placement: AdPlacement): Boolean = false

    /** Start an asynchronous load. Return false when the request cannot be started. */
    fun loadAd(type: IAdType, placement: AdPlacement, unitId: String): Boolean = false

    fun playInterstitial(activity: Activity, placement: AdPlacement, listener: IAdPlayListener): Boolean = false

    fun playRewarded(activity: Activity, placement: AdPlacement, listener: IAdPlayListener): Boolean = false

    fun playAppOpen(activity: Activity, placement: AdPlacement, listener: IAdPlayListener): Boolean = false

    /**
     * Provider adds its rendered native view only after a successful load. It must report
     * `onShown`, and must call `onFailed` if no view can be rendered. On close/destroy it must
     * release the network SDK's native ad object; Lawnchair clears the host ViewGroup separately.
     */
    fun playNative(
        activity: Activity,
        placement: AdPlacement,
        container: ViewGroup,
        listener: IAdPlayListener,
    ): Boolean = false

    /** Release any SDK object currently bound to [container]. */
    fun destroyNativeAd(container: ViewGroup) = Unit
}
