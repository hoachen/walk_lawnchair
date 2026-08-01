package com.android.launcher3.ads.launcher

import android.app.Activity
import android.app.Application
import android.view.View
import android.view.ViewGroup
import com.android.launcher3.ads.npa.AdRequestPolicy
import com.android.launcher3.ads.npa.ConsentManager
import com.android.launcher3.ads.white.NativeAdSlot
import java.util.WeakHashMap

/** Lawnchair-owned locations. New product locations should be added here deliberately. */
enum class AdPlacement(val type: IAdType) {
    /** Reference splash activity / first foreground transition. */
    SPLASH_FULLSCREEN(IAdType.AppOpen),
    /** Consent/onboarding screen completion gate. */
    ONBOARDING_COMPLETE_FULLSCREEN(IAdType.Interstitial),
    /** Fullscreen gate before launching an app from workspace or All Apps. */
    APP_ICON_LAUNCH_FULLSCREEN(IAdType.Interstitial),
    /** Fullscreen gate before opening the workspace long-press options menu. */
    WORKSPACE_LONG_PRESS_FULLSCREEN(IAdType.Interstitial),
    /** Optional app-open inventory to preload when Launcher becomes foreground. */
    LAUNCHER_RESUME_APP_OPEN(IAdType.AppOpen),
    SEARCH_LANDING_NATIVE(IAdType.Native),
    /** Reference launcher search activity native card. */
    SEARCH_PAGE_NATIVE(IAdType.Native),
    /** Reference weather activity native card. */
    WEATHER_PAGE_NATIVE(IAdType.Native),
    /** Reserved for a future All Apps adapter native row. */
    ALL_APPS_NATIVE_FIRST(IAdType.Native),
    /** Reserved for a future All Apps adapter native row. */
    ALL_APPS_NATIVE_SECOND(IAdType.Native),
    /** Generic dialog/action gate; the caller supplies a native host when applicable. */
    DIALOG_GATE_FULLSCREEN(IAdType.Interstitial),
}

/** Product-specific IDs. Lawnchair ships neither an app ID nor production ad-unit IDs. */
data class AdConfiguration(
    val appId: String,
    val unitIds: Map<AdPlacement, String>,
    val enabled: Boolean = false,
) {
    fun unitIdFor(placement: AdPlacement): String? = unitIds[placement]?.takeIf { it.isNotBlank() }
}

fun interface AdConfigurationProvider {
    fun get(): AdConfiguration
}

/**
 * Launcher-facing ad orchestrator, following the reference project's manager/provider pattern.
 * It owns placement checks and native slots; the host app owns its SDK, consent UI and IDs.
 */
object AdManager {
    private var application: Application? = null
    private var provider: IAdProvider? = null
    private var configurationProvider: AdConfigurationProvider? = null
    private var consentManager: ConsentManager? = null
    private var requestPolicy: AdRequestPolicy? = null
    private var initializedProvider: IAdProvider? = null
    private val slotRequests = WeakHashMap<ViewGroup, Long>()

    @Synchronized
    fun install(
        provider: IAdProvider,
        configurationProvider: AdConfigurationProvider,
        consentManager: ConsentManager? = null,
        requestPolicy: AdRequestPolicy? = null,
    ) {
        this.provider = provider
        this.configurationProvider = configurationProvider
        this.consentManager = consentManager
        this.requestPolicy = requestPolicy
        initializedProvider = null
        application?.let(::initializeIfEnabled)
    }

    @Synchronized
    fun uninstall() {
        provider = null
        configurationProvider = null
        consentManager = null
        requestPolicy = null
        initializedProvider = null
    }

    @Synchronized
    fun onAppCreate(application: Application) {
        this.application = application
        initializeIfEnabled(application)
    }

    @Synchronized
    private fun initializeIfEnabled(application: Application) {
        val installedProvider = provider ?: return
        val configuration = configurationProvider?.get() ?: return
        if (!configuration.enabled || initializedProvider === installedProvider) return
        installedProvider.initialize(application, configuration) { initialized ->
            if (initialized) initializedProvider = installedProvider
        }
    }

    fun preload(placement: AdPlacement): Boolean {
        val installedProvider = provider ?: return false
        val configuration = configurationProvider?.get() ?: return false
        val unitId = configuration.unitIdFor(placement) ?: return false
        if (!configuration.enabled) return false
        application?.let(::initializeIfEnabled)
        return installedProvider.loadAd(placement.type, placement, unitId)
    }

    fun showNativeAd(activity: Activity, placement: AdPlacement, container: ViewGroup) {
        val requestId = nextRequestId(container)
        val installedProvider = provider
        val configuration = configurationProvider?.get()
        val eligible = installedProvider != null && configuration?.enabled == true &&
            configuration.unitIdFor(placement) != null && placement.type == IAdType.Native &&
            (consentManager?.canRequestAds(activity) ?: true) &&
            (requestPolicy?.canRequest(activity, placement) ?: true)
        if (!eligible) {
            clearSlot(container)
            return
        }

        application?.let(::initializeIfEnabled)
        installedProvider.destroyNativeAd(container)
        NativeAdSlot.clear(container)
        if (!installedProvider.isReadyAd(IAdType.Native, placement)) preload(placement)
        val started = installedProvider.playNative(activity, placement, container, object : IAdPlayListener {
            override fun onShown() {
                if (isCurrentRequest(container, requestId)) container.visibility = View.VISIBLE
            }

            override fun onFailed(reason: String?) {
                if (isCurrentRequest(container, requestId)) NativeAdSlot.clear(container)
            }
        })
        if (!started && isCurrentRequest(container, requestId)) NativeAdSlot.clear(container)
    }

    fun clearNativeAd(container: ViewGroup) = clearSlot(container)

    /**
     * Shows a fullscreen ad, if the product configuration makes this placement eligible, then
     * always runs [afterAd]. This is the equivalent of the reference `displaySingleAds` gate.
     */
    fun showThen(activity: Activity, placement: AdPlacement, afterAd: Runnable) {
        showThenGroup(activity, listOf(placement), afterAd)
    }

    /**
     * Reference-compatible group gate: try configured fullscreen placements in order and perform
     * the original action after the first ad closes, or immediately when none can be displayed.
     */
    fun showThenGroup(activity: Activity, placements: List<AdPlacement>, afterAd: Runnable) {
        showNextFullscreenAd(activity, placements.iterator(), afterAd)
    }

    private fun showNextFullscreenAd(
        activity: Activity,
        placements: Iterator<AdPlacement>,
        afterAd: Runnable,
    ) {
        if (!placements.hasNext()) {
            afterAd.run()
            return
        }
        val placement = placements.next()
        val installedProvider = provider
        val configuration = configurationProvider?.get()
        val eligible = installedProvider != null && configuration?.enabled == true &&
            configuration.unitIdFor(placement) != null && placement.type != IAdType.Native &&
            (consentManager?.canRequestAds(activity) ?: true) &&
            (requestPolicy?.canRequest(activity, placement) ?: true)
        if (!eligible) {
            showNextFullscreenAd(activity, placements, afterAd)
            return
        }

        application?.let(::initializeIfEnabled)
        if (!installedProvider.isReadyAd(placement.type, placement)) preload(placement)
        var completed = false
        fun completeAndContinue() {
            if (!completed) {
                completed = true
                afterAd.run()
            }
        }
        fun tryNext() {
            if (!completed) {
                completed = true
                showNextFullscreenAd(activity, placements, afterAd)
            }
        }
        val listener = object : IAdPlayListener {
            override fun onDismissed() = completeAndContinue()
            override fun onFailed(reason: String?) = tryNext()
        }
        val started = when (placement.type) {
            IAdType.Interstitial -> installedProvider.playInterstitial(activity, placement, listener)
            IAdType.Rewarded -> installedProvider.playRewarded(activity, placement, listener)
            IAdType.AppOpen -> installedProvider.playAppOpen(activity, placement, listener)
            IAdType.Native -> false
        }
        if (!started) tryNext()
    }

    @Synchronized
    private fun nextRequestId(container: ViewGroup): Long {
        val next = (slotRequests[container] ?: 0L) + 1L
        slotRequests[container] = next
        return next
    }

    @Synchronized
    private fun isCurrentRequest(container: ViewGroup, requestId: Long) = slotRequests[container] == requestId

    private fun clearSlot(container: ViewGroup) {
        nextRequestId(container)
        provider?.destroyNativeAd(container)
        NativeAdSlot.clear(container)
    }
}
