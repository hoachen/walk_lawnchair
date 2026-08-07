# 游戏接入 Launcher SDK 调研报告

## 样本结论

样本为 `Arrow Master 1.1.1`，包名 `com.arrow.master.shoot`，`versionCode=100`，`versionName=1.1.1`，`minSdk=26`，`targetSdk=35`，`compileSdk=36`。它不是简单地从游戏跳转到 Launcher，而是把 Lawnchair/Launcher3 作为同包能力完整嵌入，再用游戏入口获量、Launcher 入口留存、内容/工具入口和广告中介做二次变现。

样本里的 Lawnchair 代码基线接近当前 Lawnchair 15 系列：保留 `com.android.launcher3`、`app.lawnchair.LawnchairLauncher`、Quickstep、Secondary Home、GridCustomizationsProvider 等模块，同时在 `LawnchairLauncher` 上加入了业务层逻辑：Minus One 左滑容器、默认桌面提示、All Apps 引导、安装/卸载事件、广告预加载和内容入口。

## 游戏 + Launcher 结合方式

Manifest 里游戏首入口是 `com.mgyb.wxevxsrfsevhegxmzmxcv`，带 `MAIN + LAUNCHER`。真正的 Launcher Activity 是 `app.lawnchair.LawnchairLauncher`，本体没有直接声明 HOME filter，而是通过 alias 控制是否作为桌面候选：

- `app.lawnchair.LawnchairLauncher.home`：`HOME + DEFAULT`，默认 `enabled=false`。
- `app.lawnchair.LawnchairLauncher.non.home`：默认 `enabled=true`，可作为非 HOME 入口。
- `weather`、`flashlight`、`game_center`、`coupons`、`scan_code`、`news`、`wallpaper`、`all_apps`、`feed_page`：多个功能 alias，目标均指向一个中转 Activity。

业务桥接集中在 `org.ks.f` 和实现类 `com.rso.h.cJSs`：

- `goLauncher(context)`：如果已经满足某些默认桌面状态，发 HOME intent；否则直接启动 `LawnchairLauncher`。
- `goMinusOnePage()`：当前 Activity 是 Lawnchair 时调用 `openSwipeLayout()`。
- `goNews/goWeather/goWallpaper`：先延迟执行本地功能，再延迟打开容器页面，形成“入口点击 -> 可能打开 feed/工具页 -> 商业化容器”的路径。
- `isLauncherAlive/isInLauncherMainPage/isOrganic`：给广告和入口策略判断状态。
- `getLastRemovedAppInfo`：从 All Apps 或缓存中拿卸载应用信息，可用于卸载挽留/推荐。

`LawnchairLauncher` 本体做了几类改造：

- `initSwipeLayout()`：外层换成可左右滑的容器，左侧承载 Minus One/feed。
- `openSwipeLayout()`：给外部入口直接打开 feed。
- `showAllAppsGuide()`、`showGameCenterGuide()`：首次引导用户进入 All Apps 或游戏中心。
- `checkDefaultLauncher()`：在 Launcher resume 后，带冷却地请求系统 HOME role。
- `bindNewsPopupKit()`：把新闻弹窗/内容组件挂进 Launcher root。
- 安装/卸载监听：打 `new_install`、`vupdate_end` 等事件。

## 变现策略

样本接入了非常重的广告和归因栈。Manifest 和源码中可以看到 TopOn/ThinkUp、SmartDigiMkt、AppLovin MAX、Unity Ads、Vungle、Pangle、Mintegral/MBridge、Bigo、Fyber、Moloco、BidMachine、Meta Audience Network、Google AdMob、Yandex、TaurusX、Xiaomi/Columbus 等广告组件，同时包含 Firebase Analytics/Remote Config/Crashlytics、AppsFlyer、AppMetrica、Reyun/Solar、Install Referrer。

可推断的变现链路：

- 游戏首启/前台：Splash、App Open、Interstitial。
- Launcher 前台：resume 时预加载 App Open 和插屏。
- 点击桌面图标/应用列表：通过 icon launch placement 展示插屏后继续打开应用。
- All Apps/Search/Minus One：插入 Native/Banner 广告位。
- 新闻、天气、壁纸、工具 alias：中转到内容页或 loading 页，承载 Native、Interstitial 或落地页广告。
- 非自然量策略：`isOrganic()` 判断归因来源，广告默认偏向非自然量展示；organic 用户可能减少广告以降低投诉和商店风险。

这套策略的关键是“游戏负责获量，Launcher 负责高频场景和长期库存”。桌面成为默认 HOME 后，广告可触发点明显增多：返回桌面、打开应用、搜索、进入 All Apps、查看 feed、点击工具入口。

## 灰产风险判断

样本存在明显高风险信号，建议只参考产品结构，不照搬实现：

- 权限过重：包含 `MANAGE_ACCESSIBILITY`、`PACKAGE_USAGE_STATS`、`READ_FRAME_BUFFER`、`MONITOR_INPUT`、`SYSTEM_APPLICATION_OVERLAY`、`STATUS_BAR`、`REMOVE_TASKS`、`CALL_PHONE` 等高敏权限或系统级权限。
- 功能 alias 伪装：天气、手电、优惠券、扫码等 alias 可能让用户误以为安装了独立工具，本质上却进入同一个中转页面。
- 默认桌面请求较激进：Launcher resume 后自动弹 HOME role，请求成为默认桌面。
- 广告栈过重：多个广告网络、MRAID/OM SDK/落地页组件并存，容易引发合规、隐私和商店审核问题。
- `usesCleartextTraffic=true`、大量 exported provider/service/activity 增加攻击面。

我们的 SDK 实现因此采用合规边界：SDK 只提供 feature routing、默认桌面请求节流、埋点和广告 Provider 接口，不内置伪装入口、不申请高危权限、不直连广告网络、不写 Launcher 数据库。

## 已参考实现

本分支新增 `game-launcher-sdk`，将样本可复用部分抽成游戏侧 SDK：

- `GameLauncherSdk.openLauncher/openMinusOne/openAllApps/returnHome`：基础 Launcher 能力。
- `GameLauncherFeature` + `openFeature/openNews/openWeather/openWallpaper/openSearch`：参考 alias 矩阵，但通过显式 feature 路由暴露。
- `requestSetAsDefaultLauncherIfAllowed`：参考样本 HOME role 流程，但加本地冷却，默认 24 小时一次。
- `GameLauncherMonetizationProvider`：参考多广告位策略，但只提供 `preload/showThen` 抽象，广告 SDK 由游戏接入方持有。
- `GameLauncherAcquisitionProvider`：参考 organic/non-organic 分层，SDK 不直接接入 AppsFlyer/Tenjin。
- `notifyLauncherVisible/preloadPlacement`：参考 Launcher resume 预加载策略。
- `GameLauncherHostController`：宿主 Launcher 侧接入点，用于把 SDK feature 映射到真实 Lawnchair 状态。

## 推荐接入方案

游戏侧只依赖 SDK：

```kotlin
GameLauncherSdk.init(
    application,
    GameLauncherConfig.Builder()
        .setGameId("arrow_master")
        .setLauncherPackageName(application.packageName)
        .setLauncherActivityClassName("app.lawnchair.LawnchairLauncher")
        .setEntryMode(GameLauncherEntryMode.OPT_IN)
        .setMinusOneEnabled(true)
        .setAdsEnabled(true)
        .setMonetizationProvider(gameAdProvider)
        .setAcquisitionProvider { context -> attribution.isOrganic(context) }
        .setAnalyticsProvider { event -> analytics.track(event.name, event.params) }
        .build(),
)
```

宿主 Launcher 侧只实现 `GameLauncherHostController`，把 `MINUS_ONE/FEED_PAGE` 映射到 feed overlay，把 `ALL_APPS/SEARCH` 映射到 `LauncherState.ALL_APPS`，把默认桌面状态委托给系统 HOME resolver。业务功能页如新闻、天气、壁纸建议由游戏 App 自己提供显式 Activity，不建议做伪装 alias。

