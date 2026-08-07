package com.urtech.launcher.sdk

import android.app.Activity
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.util.concurrent.atomic.AtomicReference

/**
 * Thin game-facing facade for products that embed the Lawnchair based host.
 *
 * The SDK intentionally exposes product concepts instead of Launcher3 internals. Games should not
 * depend on Workspace, Launcher, AdManager or Lawnchair preference classes directly.
 */
object GameLauncherSdk {
    private val hostControllerRef = AtomicReference<GameLauncherHostController?>()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    var config: GameLauncherConfig = GameLauncherConfig.Builder().build()
        private set

    @JvmStatic
    fun init(context: Context, config: GameLauncherConfig = GameLauncherConfig.Builder().build()) {
        appContext = context.applicationContext
        this.config = config
        track(GameLauncherEvent.sdkInit(config.gameId))
    }

    @JvmStatic
    fun updateConfig(config: GameLauncherConfig) {
        this.config = config
        track(GameLauncherEvent("launcher_config_update", config.eventParams()))
    }

    @JvmStatic
    fun installHostController(controller: GameLauncherHostController?) {
        hostControllerRef.set(controller)
    }

    @JvmStatic
    fun openLauncher(context: Context): Boolean {
        track(GameLauncherEvent("launcher_open_request", mapOf("source" to "game_sdk")))
        if (hostControllerRef.get()?.openLauncher(context) == true) return true

        val intent = launcherIntent(context)
        return context.startSafely(intent)
    }

    @JvmStatic
    fun openMinusOne(context: Context): Boolean {
        track(GameLauncherEvent("launcher_minus_one_open_request", mapOf("source" to "game_sdk")))
        if (!config.minusOneEnabled) return false
        if (hostControllerRef.get()?.openMinusOne(context) == true) return true

        val intent = launcherIntent(context)
            .setAction(GameLauncherIntents.ACTION_OPEN_MINUS_ONE)
            .putExtra(GameLauncherIntents.EXTRA_OPEN_MINUS_ONE, true)
        return context.startSafely(intent)
    }

    @JvmStatic
    fun openAllApps(context: Context): Boolean {
        track(GameLauncherEvent("launcher_all_apps_open_request", mapOf("source" to "game_sdk")))
        if (hostControllerRef.get()?.openAllApps(context) == true) return true

        val intent = launcherIntent(context)
            .setAction(GameLauncherIntents.ACTION_OPEN_ALL_APPS)
            .putExtra(GameLauncherIntents.EXTRA_OPEN_ALL_APPS, true)
        return context.startSafely(intent)
    }

    @JvmStatic
    @JvmOverloads
    fun openFeature(context: Context, feature: GameLauncherFeature, query: String? = null): Boolean {
        track(
            GameLauncherEvent(
                "launcher_feature_open_request",
                mapOf(
                    "source" to "game_sdk",
                    "feature" to feature.key,
                ),
            ),
        )
        if (!config.isFeatureEnabled(feature)) return false
        when (feature) {
            GameLauncherFeature.LAUNCHER -> return openLauncher(context)
            GameLauncherFeature.MINUS_ONE,
            GameLauncherFeature.FEED_PAGE,
                -> return openMinusOne(context)
            GameLauncherFeature.ALL_APPS -> return openAllApps(context)
            else -> Unit
        }
        if (hostControllerRef.get()?.openFeature(context, feature, query) == true) return true

        val intent = launcherIntent(context)
            .setAction(GameLauncherIntents.ACTION_OPEN_FEATURE)
            .putExtra(GameLauncherIntents.EXTRA_OPEN_FEATURE, feature.key)
            .apply {
                if (!query.isNullOrBlank()) {
                    putExtra(GameLauncherIntents.EXTRA_FEATURE_QUERY, query)
                }
            }
        return context.startSafely(intent)
    }

    @JvmStatic
    fun openNews(context: Context): Boolean = openFeature(context, GameLauncherFeature.NEWS)

    @JvmStatic
    fun openWeather(context: Context): Boolean = openFeature(context, GameLauncherFeature.WEATHER)

    @JvmStatic
    fun openWallpaper(context: Context): Boolean = openFeature(context, GameLauncherFeature.WALLPAPER)

    @JvmStatic
    @JvmOverloads
    fun openSearch(context: Context, query: String? = null): Boolean = openFeature(
        context,
        GameLauncherFeature.SEARCH,
        query,
    )

    @JvmStatic
    fun openPolicy(context: Context): Boolean = openFeature(context, GameLauncherFeature.POLICY)

    @JvmStatic
    fun openTerms(context: Context): Boolean = openFeature(context, GameLauncherFeature.TERMS)

    @JvmStatic
    fun openGameCenter(context: Context): Boolean = openFeature(context, GameLauncherFeature.GAME_CENTER)

    @JvmStatic
    fun openCoupons(context: Context): Boolean = openFeature(context, GameLauncherFeature.COUPONS)

    @JvmStatic
    fun openScanCode(context: Context): Boolean = openFeature(context, GameLauncherFeature.SCAN_CODE)

    @JvmStatic
    fun returnHome(context: Context): Boolean {
        track(GameLauncherEvent("launcher_return_home_request", mapOf("source" to "game_sdk")))
        if (hostControllerRef.get()?.returnHome(context) == true) return true
        return openLauncher(context)
    }

    @JvmStatic
    @JvmOverloads
    fun requestSetAsDefaultLauncher(activity: Activity, requestCode: Int = REQUEST_HOME_ROLE): Boolean {
        track(GameLauncherEvent("launcher_default_request", mapOf("source" to "game_sdk")))
        if (hostControllerRef.get()?.requestSetAsDefaultLauncher(activity, requestCode) == true) {
            return true
        }
        return DefaultLauncherRole.request(activity, requestCode)
    }

    @JvmStatic
    @JvmOverloads
    fun requestSetAsDefaultLauncherIfAllowed(
        activity: Activity,
        requestCode: Int = REQUEST_HOME_ROLE,
    ): Boolean {
        if (!shouldRequestDefaultLauncher(activity)) return false
        DefaultLauncherPromptStore.markPrompted(activity, config.gameId)
        return requestSetAsDefaultLauncher(activity, requestCode)
    }

    @JvmStatic
    fun shouldRequestDefaultLauncher(context: Context): Boolean {
        if (!config.defaultLauncherPromptEnabled) return false
        if (isDefaultLauncher(context)) return false
        return DefaultLauncherPromptStore.isAllowed(
            context,
            config.gameId,
            config.defaultLauncherPromptMinIntervalMs,
        )
    }

    @JvmStatic
    fun isDefaultLauncher(context: Context): Boolean {
        hostControllerRef.get()?.isDefaultLauncher(context)?.let { return it }
        return DefaultLauncherRole.isDefault(context, config.resolvedLauncherPackage(context))
    }

    @JvmStatic
    fun isLauncherAlive(): Boolean {
        return hostControllerRef.get()?.isLauncherAlive() ?: false
    }

    @JvmStatic
    fun isInLauncherMainPage(): Boolean {
        return hostControllerRef.get()?.isInLauncherMainPage() ?: false
    }

    @JvmStatic
    fun isOrganicInstall(context: Context): Boolean {
        return config.acquisitionProvider?.isOrganic(context) ?: true
    }

    @JvmStatic
    fun notifyLauncherVisible(context: Context) {
        track(GameLauncherEvent("launcher_visible", mapOf("source" to "game_sdk")))
        preloadPlacement(context, GameLauncherPlacement.LAUNCHER_VISIBLE)
        hostControllerRef.get()?.preloadWhenLauncherVisible(context)
    }

    @JvmStatic
    fun preloadPlacement(context: Context, placement: GameLauncherPlacement): Boolean {
        if (!config.adsEnabled) return false
        if (hostControllerRef.get()?.preloadPlacement(context, placement) == true) return true
        return config.monetizationProvider?.preload(context, placement) == true
    }

    @JvmStatic
    fun showAdThen(activity: Activity, placement: GameLauncherPlacement, continuation: Runnable) {
        if (!config.adsEnabled) {
            continuation.run()
            return
        }
        track(GameLauncherEvent("launcher_ad_gate_request", mapOf("placement" to placement.key)))
        val started =
            config.monetizationProvider?.showThen(activity, placement, continuation) == true ||
                config.adGateProvider?.showThen(activity, placement, continuation) == true
        if (!started) continuation.run()
    }

    @JvmStatic
    fun track(event: GameLauncherEvent) {
        config.analyticsProvider?.track(event)
    }

    private fun launcherIntent(context: Context): Intent {
        val targetPackage = config.resolvedLauncherPackage(context)
        val targetClass = config.launcherActivityClassName
        return Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .apply {
                if (targetPackage.isNotBlank() && targetClass.isNotBlank()) {
                    component = ComponentName(targetPackage, targetClass)
                } else if (targetPackage.isNotBlank()) {
                    setPackage(targetPackage)
                }
            }
    }

    private fun Context.startSafely(intent: Intent): Boolean {
        return runCatching {
            startActivity(intent)
            true
        }.getOrElse { false }
    }

    const val REQUEST_HOME_ROLE = 9107
    const val DEFAULT_HOME_PROMPT_INTERVAL_MS = 24 * 60 * 60 * 1000L
}

enum class GameLauncherEntryMode {
    GAME_FIRST,
    OPT_IN,
    LAUNCHER_FIRST,
}

enum class GameLauncherPlacement {
    GAME_ENTRY,
    LAUNCHER_OPEN,
    LAUNCHER_VISIBLE,
    MINUS_ONE_OPEN,
    ALL_APPS_OPEN,
    FEATURE_ENTRY,
    NEWS_OPEN,
    WEATHER_OPEN,
    WALLPAPER_OPEN,
    SEARCH_OPEN,
    DEFAULT_LAUNCHER_PROMPT,
    APP_ICON_LAUNCH,
    WORKSPACE_LONG_PRESS,
    APP_OPEN,
    RESUME,
    EXIT,
    REWARD,
    NATIVE_FEED,
    BANNER_ALL_APPS,
    INTERSTITIAL_TRANSITION,
    ;

    val key: String
        get() = name.lowercase()
}

enum class GameLauncherFeature {
    LAUNCHER,
    MINUS_ONE,
    ALL_APPS,
    NEWS,
    WEATHER,
    WALLPAPER,
    SEARCH,
    POLICY,
    TERMS,
    GAME_CENTER,
    COUPONS,
    SCAN_CODE,
    FEED_PAGE,
    ;

    val key: String
        get() = name.lowercase()
}

fun interface GameLauncherAnalyticsProvider {
    fun track(event: GameLauncherEvent)
}

fun interface GameLauncherAdGateProvider {
    fun showThen(activity: Activity, placement: GameLauncherPlacement, continuation: Runnable): Boolean
}

interface GameLauncherMonetizationProvider {
    fun preload(context: Context, placement: GameLauncherPlacement): Boolean = false
    fun showThen(activity: Activity, placement: GameLauncherPlacement, continuation: Runnable): Boolean = false
}

fun interface GameLauncherAcquisitionProvider {
    fun isOrganic(context: Context): Boolean
}

data class GameLauncherEvent(
    val name: String,
    val params: Map<String, String> = emptyMap(),
) {
    companion object {
        fun sdkInit(gameId: String) = GameLauncherEvent(
            "launcher_sdk_init",
            if (gameId.isBlank()) emptyMap() else mapOf("game_id" to gameId),
        )
    }
}

class GameLauncherConfig private constructor(
    val gameId: String,
    val launcherPackageName: String?,
    val launcherActivityClassName: String,
    val entryMode: GameLauncherEntryMode,
    val minusOneEnabled: Boolean,
    val enabledFeatures: Set<GameLauncherFeature>,
    val adsEnabled: Boolean,
    val defaultLauncherPromptEnabled: Boolean,
    val defaultLauncherPromptMinIntervalMs: Long,
    val analyticsProvider: GameLauncherAnalyticsProvider?,
    val adGateProvider: GameLauncherAdGateProvider?,
    val monetizationProvider: GameLauncherMonetizationProvider?,
    val acquisitionProvider: GameLauncherAcquisitionProvider?,
) {
    fun resolvedLauncherPackage(context: Context): String = launcherPackageName ?: context.packageName

    fun isFeatureEnabled(feature: GameLauncherFeature): Boolean {
        return feature in enabledFeatures && (feature != GameLauncherFeature.MINUS_ONE || minusOneEnabled)
    }

    internal fun eventParams(): Map<String, String> = buildMap {
        if (gameId.isNotBlank()) put("game_id", gameId)
        put("entry_mode", entryMode.name)
        put("minus_one_enabled", minusOneEnabled.toString())
        put("ads_enabled", adsEnabled.toString())
        put("default_launcher_prompt_enabled", defaultLauncherPromptEnabled.toString())
    }

    class Builder {
        private var gameId: String = ""
        private var launcherPackageName: String? = null
        private var launcherActivityClassName: String = "app.lawnchair.LawnchairLauncher"
        private var entryMode: GameLauncherEntryMode = GameLauncherEntryMode.OPT_IN
        private var minusOneEnabled: Boolean = true
        private var enabledFeatures: Set<GameLauncherFeature> = GameLauncherFeature.entries.toSet()
        private var adsEnabled: Boolean = false
        private var defaultLauncherPromptEnabled: Boolean = true
        private var defaultLauncherPromptMinIntervalMs: Long = GameLauncherSdk.DEFAULT_HOME_PROMPT_INTERVAL_MS
        private var analyticsProvider: GameLauncherAnalyticsProvider? = null
        private var adGateProvider: GameLauncherAdGateProvider? = null
        private var monetizationProvider: GameLauncherMonetizationProvider? = null
        private var acquisitionProvider: GameLauncherAcquisitionProvider? = null

        fun setGameId(gameId: String) = apply { this.gameId = gameId }
        fun setLauncherPackageName(packageName: String?) = apply { this.launcherPackageName = packageName }
        fun setLauncherActivityClassName(className: String) = apply { this.launcherActivityClassName = className }
        fun setEntryMode(entryMode: GameLauncherEntryMode) = apply { this.entryMode = entryMode }
        fun setMinusOneEnabled(enabled: Boolean) = apply { this.minusOneEnabled = enabled }
        fun setEnabledFeatures(features: Collection<GameLauncherFeature>) = apply {
            this.enabledFeatures = features.toSet()
        }
        fun setEnabledFeatures(vararg features: GameLauncherFeature) = apply {
            this.enabledFeatures = features.toSet()
        }
        fun setAdsEnabled(enabled: Boolean) = apply { this.adsEnabled = enabled }
        fun setDefaultLauncherPromptEnabled(enabled: Boolean) = apply {
            this.defaultLauncherPromptEnabled = enabled
        }
        fun setDefaultLauncherPromptMinIntervalMs(intervalMs: Long) = apply {
            this.defaultLauncherPromptMinIntervalMs = intervalMs.coerceAtLeast(0)
        }
        fun setAnalyticsProvider(provider: GameLauncherAnalyticsProvider?) = apply { this.analyticsProvider = provider }
        fun setAdGateProvider(provider: GameLauncherAdGateProvider?) = apply { this.adGateProvider = provider }
        fun setMonetizationProvider(provider: GameLauncherMonetizationProvider?) = apply {
            this.monetizationProvider = provider
        }
        fun setAcquisitionProvider(provider: GameLauncherAcquisitionProvider?) = apply {
            this.acquisitionProvider = provider
        }

        fun build() = GameLauncherConfig(
            gameId = gameId,
            launcherPackageName = launcherPackageName,
            launcherActivityClassName = launcherActivityClassName,
            entryMode = entryMode,
            minusOneEnabled = minusOneEnabled,
            enabledFeatures = enabledFeatures,
            adsEnabled = adsEnabled,
            defaultLauncherPromptEnabled = defaultLauncherPromptEnabled,
            defaultLauncherPromptMinIntervalMs = defaultLauncherPromptMinIntervalMs,
            analyticsProvider = analyticsProvider,
            adGateProvider = adGateProvider,
            monetizationProvider = monetizationProvider,
            acquisitionProvider = acquisitionProvider,
        )
    }
}

interface GameLauncherHostController {
    fun openLauncher(context: Context): Boolean = false
    fun openMinusOne(context: Context): Boolean = false
    fun openAllApps(context: Context): Boolean = false
    fun openFeature(context: Context, feature: GameLauncherFeature, query: String?): Boolean = false
    fun returnHome(context: Context): Boolean = false
    fun requestSetAsDefaultLauncher(activity: Activity, requestCode: Int): Boolean = false
    fun isDefaultLauncher(context: Context): Boolean? = null
    fun isLauncherAlive(): Boolean? = null
    fun isInLauncherMainPage(): Boolean? = null
    fun preloadPlacement(context: Context, placement: GameLauncherPlacement): Boolean = false
    fun preloadWhenLauncherVisible(context: Context): Boolean = false
}

object GameLauncherIntents {
    const val ACTION_OPEN_MINUS_ONE = "com.urtech.launcher.sdk.action.OPEN_MINUS_ONE"
    const val ACTION_OPEN_ALL_APPS = "com.urtech.launcher.sdk.action.OPEN_ALL_APPS"
    const val ACTION_OPEN_FEATURE = "com.urtech.launcher.sdk.action.OPEN_FEATURE"
    const val EXTRA_OPEN_MINUS_ONE = "com.urtech.launcher.sdk.extra.OPEN_MINUS_ONE"
    const val EXTRA_OPEN_ALL_APPS = "com.urtech.launcher.sdk.extra.OPEN_ALL_APPS"
    const val EXTRA_OPEN_FEATURE = "com.urtech.launcher.sdk.extra.OPEN_FEATURE"
    const val EXTRA_FEATURE_QUERY = "com.urtech.launcher.sdk.extra.FEATURE_QUERY"
}

private object DefaultLauncherRole {
    fun request(activity: Activity, requestCode: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(RoleManager::class.java) ?: return openHomeSettings(activity)
            if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) return openHomeSettings(activity)
            if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) return true
            runCatching {
                activity.startActivityForResult(
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME),
                    requestCode,
                )
                true
            }.getOrElse { openHomeSettings(activity) }
        } else {
            openHomeSettings(activity)
        }
    }

    fun isDefault(context: Context, launcherPackageName: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && launcherPackageName == context.packageName) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == launcherPackageName
    }

    private fun openHomeSettings(context: Context): Boolean {
        return runCatching {
            context.startActivity(
                Intent(Settings.ACTION_HOME_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }.getOrElse { false }
    }
}

private object DefaultLauncherPromptStore {
    private const val PREFS = "com.urtech.launcher.sdk.default_prompt"
    private const val KEY_PREFIX = "last_prompt_"

    fun isAllowed(context: Context, gameId: String, intervalMs: Long): Boolean {
        val lastPrompt = prefs(context).getLong(key(gameId), 0L)
        return System.currentTimeMillis() - lastPrompt >= intervalMs
    }

    fun markPrompted(context: Context, gameId: String) {
        prefs(context)
            .edit()
            .putLong(key(gameId), System.currentTimeMillis())
            .apply()
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(gameId: String) = KEY_PREFIX + gameId.ifBlank { "default" }
}
