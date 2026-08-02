# Walk Lawnchair：工具 App 接入与页面定制指南

**适用发布物：** `com.lawnchair:launcher-sdk:1.0.26`（Release）与 `com.lawnchair:launcher-sdk-debug:1.0.26`（Debug）
**适用工程：** `walk_lawnchair` 当前分支  
**最后更新：** 2026-08-01

## 1. 先读结论

当前 `build/repo` 中发布的是完整 Launcher 的 AAR，而不是独立 APK 可跨进程调用的稳定 SDK。`app.lawnchair.LauncherSDK.overlayProvider` 只是 Launcher 进程内的静态变量：工具 App 即使依赖了 AAR，设置该变量也只会作用于**自己的进程**，不会改变已安装的 Lawnchair。

因此，接入方式应按目标选择：

| 目标 | 当前可用方式 | 是否需要改 Launcher |
|---|---|---|
| 在主页放置工具入口 | Android App Widget、动态快捷方式、`requestPinShortcut()` | 否；用户确认放置 |
| 做完整负一屏 | `WINDOW_OVERLAY` 服务 + Lawnfeed/Google Feed AIDL 协议 | 否，但须通过签名白名单 |
| 由独立工具 App 提供自定义负一屏内容 | 同上；工具 App 自己实现 `ILauncherOverlay` 服务 | 否，推荐 |
| 在 Launcher 内置一个产品专属负一屏 | `CustomFeedOverlay` / `LauncherSDK.overlayProvider` | 是；代码随 Launcher 打包 |
| 定制普通主页网格、壁纸、Hotseat、手势 | Lawnchair 设置与 Android Widget/Shortcut | 不能由外部 App 强制改动 |
| 插入一个非标准主页页面或完全控制主页布局 | 需要 Launcher 侧新增公开接口/页面宿主 | 是 |
| 在搜索结果中插广告位 | 需要 Launcher 侧扩展搜索 Adapter 和受控广告 Provider | 是 |

> 不建议将 `launcher-sdk` AAR 直接打进工具 App 的运行时依赖。它包含 Launcher 实现及大量传递依赖，既不能连接已安装 Launcher，也可能引入 Manifest、资源与类冲突。工具 App 应使用一个小型、独立发布的 API/AIDL 契约模块。

## 2. 发布仓库与依赖边界

发布仓库位于 `build/repo`，当前坐标为：

```kotlin
repositories {
    maven { url = uri("/绝对路径/walk_lawnchair/build/repo") }
}

dependencies {
    // 仅用于源码阅读或同进程定制；不作为独立工具 App 的运行时接入方式
    implementation("com.lawnchair:launcher-sdk:1.0.26")
}
```

重新生成 Release 发布物：

```bash
./gradlew publishLawnWithQuickstepPlayReleasePublicationToLocalRepoRepository
```

发布目录中应至少包含 `.aar`、`.pom`、`.module` 与 `-sources.jar`。当前 POM 还会带出多个 Lawnchair 内部模块；这进一步说明它不适合作为对外运行时 SDK。

**建议的对外产物拆分：**

1. `launcher-tool-api`：只含常量、Parcelable 数据模型和 AIDL；无 Launcher 实现依赖。
2. `launcher-sdk`：保留为 Launcher 自身/同进程定制依赖，不承诺跨版本 ABI。
3. `tool-app`：仅依赖 `launcher-tool-api`，通过 Binder 或显式 Intent 与 Launcher 通讯。

## 3. 工具 App 的基础接入

### 3.1 作为普通主页入口

工具 App 可以正常提供 Activity、动态快捷方式及 App Widget。用户可将它们放到主页；Launcher 管理其位置和生命周期。

```kotlin
val shortcut = ShortcutInfo.Builder(context, "open_tools")
    .setShortLabel("工具中心")
    .setIntent(Intent(context, ToolActivity::class.java).setAction(Intent.ACTION_VIEW))
    .build()

val request = ShortcutManagerCompat.createShortcutResultIntent(context, shortcut)
ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
```

限制：Android 的置顶快捷方式需要用户确认；工具 App 不应直接写 Launcher 数据库、也不能强制改变页面顺序、网格或 Hotseat。

### 3.2 启动 Launcher 与设置页

将 Lawnchair 设为默认桌面后，可以通过 HOME Intent 回到主页；可通过 `android.intent.action.APPLICATION_PREFERENCES` 打开当前工程导出的 Lawnchair 设置页。调用前应用 `resolveActivity()`，并为找不到目标准备降级处理。

```kotlin
val settingsIntent = Intent("android.intent.action.APPLICATION_PREFERENCES")
    .addCategory(Intent.CATEGORY_DEFAULT)
if (settingsIntent.resolveActivity(packageManager) != null) {
    startActivity(settingsIntent)
}
```

## 4. 自定义负一屏

### 4.1 方案 A：独立 Feed Provider（推荐）

当前 Lawnchair 会发现带有以下 action 的 Service：

```text
com.android.launcher3.WINDOW_OVERLAY
```

然后按 Google/Lawnfeed 的 `ILauncherOverlay` Binder 协议绑定。协议定义在：

- `lawnchair/aidl/com/google/android/libraries/launcherclient/ILauncherOverlay.aidl`
- `lawnchair/aidl/com/google/android/libraries/launcherclient/ILauncherOverlayCallback.aidl`

最小 Manifest 形态：

```xml
<service
    android:name=".feed.ToolFeedService"
    android:exported="true">
    <intent-filter>
        <action android:name="com.android.launcher3.WINDOW_OVERLAY" />
    </intent-filter>
</service>
```

Service 必须在 `onBind()` 返回 `ILauncherOverlay.Stub`。Launcher 会依次调用 `windowAttached2()`、`setActivityState()`、`startScroll()`、`onScroll()`、`endScroll()`、`openOverlay()` 和 `closeOverlay()`。实现方负责创建/移动自己的窗口，并以 `ILauncherOverlayCallback` 回调滚动进度与内容状态。

核心时序：

```text
用户在第 0 页继续向左滑
  -> Launcher 绑定 ToolFeedService
  -> windowAttached2(configuration / layout_params / client_options)
  -> startScroll -> onScroll(progress: 0..1) -> endScroll
  -> Provider 绘制负一屏，并 callback.overlayScrollChanged(progress)
```

**发现与安全门槛：**

- 用户在 Lawnchair 设置的“负一屏 / Feed Provider”中选择 Provider。
- Release 包会校验 Provider 签名哈希。自定义包名默认不在白名单中，因而不会出现在可选列表。
- 应在 Launcher 的 `FeedBridge.initializeWhitelist()` 加入工具 App 包名和**发布签名**哈希，重新签名并发布 Launcher；不要在生产环境开启 `ignoreFeedWhitelist`。
- 调试构建会放宽签名检查，适合联调，不可作为生产安全策略。

`FeedBridge` 的现有兼容路径还可通过 `amirz.aidlbridge` 转发，但新 Provider 应优先直接实现 `ILauncherOverlay`。

### 4.2 方案 B：内置负一屏

若负一屏与 Launcher 由同一团队维护，可在 Launcher 内部设置：

```kotlin
LauncherSDK.isOverlayEnabled = true
LauncherSDK.overlayProvider = object : LauncherSDK.OverlayProvider {
    override fun createView(context: Context): View = ToolFeedView(context)
}
```

`CustomFeedOverlay` 会在 `createOverlayView()` 调用该 Provider，并将其包进 Launcher 顶层的手势宿主。该方式可直接使用普通 View/Compose 容器，开发成本最低。

SDK 手势宿主会统一处理“从负一屏向右滑回主页”：它不会让 `ScrollView` / `RecyclerView` 的
`requestDisallowInterceptTouchEvent()` 阻断横向手势，且会通过 `onOverlayScrollChanged()` 同步
Workspace 位移。接入 App **不要**自行隐藏页面或实现回主页动画；Provider 只处理纵向滚动和自身点击。

**重要：** 这段代码必须在 Lawnchair 进程、且在创建 `CustomFeedOverlay` 前执行。独立工具 App 无法借由 Maven AAR 或反射设置它。

内置默认 Feed 的可定制文件：

| 内容 | 位置 |
|---|---|
| 覆盖层生命周期、手势、进出动画 | `lawnchair/src/app/lawnchair/overlay/CustomFeedOverlay.kt` |
| Feed 数据模型 | `lawnchair/src/app/lawnchair/overlay/FeedItem.kt` |
| Feed 列表渲染 | `lawnchair/src/app/lawnchair/overlay/CustomFeedAdapter.kt` |
| 默认 Overlay 入口 | `lawnchair/src/app/lawnchair/LawnchairLauncher.kt` |

## 5. Launcher 主页面如何定制

### 原生首页时间卡

`1.0.20` 将时间/日期卡实现为 Lawnchair 原有的 Smartspace 日期卡
（`res/layout/smartspace_card_date.xml`），而非叠加在 `DragLayer` 上的悬浮 View。因此它只占用
首页原生布局中的 Smartspace 区域，不覆盖图标，也不会出现在其他 Workspace 页。样式可通过该布局、
`home_clock_card_background.xml` 和 `enhanced_smartspace_height` 定制。

该样式要求 Smartspace 模式为 **Lawnchair**；Google Smartspace 由 Google App 自身渲染，Launcher
不能可靠修改其内部文字样式。已有设备若仍显示 “August …” 的 Google 卡片，请在 Lawnchair 设置中将
Smartspace 模式切换为 Lawnchair 后重启 Launcher。

内嵌 SDK 的接入 App 可在 `ContentProvider.onCreate()` 中、创建 Launcher 前调用：

```kotlin
LauncherSDK.useLawnchairSmartspace(appContext)
```

这是 `1.0.21` 提供的公开门面；不要直接依赖 `PreferenceManager2` 或写入 Launcher 的内部 DataStore。

主页是 `Workspace` 的多个 `CellLayout` 页面；图标、文件夹和 Widget 都由 Launcher 数据库与 Model 绑定。当前工程没有对外“插入自定义页面”的稳定 API。

### 可不改 Launcher 的能力

- App Widget：适合状态卡片、快捷控制和内容预览；由用户添加、拖动和缩放。
- Pin Shortcut：适合一次性入口；用户确认后放到主页。
- Deep Link / App Search：适合从搜索结果直达工具能力。
- 配置 Launcher 偏好：只能让用户在设置 UI 中选择，工具 App 不应直接写 SharedPreferences 或数据库。

### 需要改 Launcher 的能力

若产品要求“固定的工具页、仪表盘页或广告/运营页”，应在 Launcher 中实现 Page Host，而不是让第三方 App 操作 `Workspace` 私有数据：

1. 定义 `HomePageProvider` AIDL：版本、页面 id、标题、可见条件、`RemoteViews`/数据快照、点击 PendingIntent。
2. Launcher 绑定可信 Provider，做签名白名单、超时、死亡重连和版本协商。
3. 在 `Workspace` 前后插入一个受 Launcher 控制的页面容器；页面自身不要伪装成 `CellLayout`，避免破坏拖拽、页面删除和数据库持久化。
4. 所有点击由 Launcher 校验并使用 `PendingIntent` 执行；Provider 不能取得 Workspace 或数据库写权限。
5. 为用户提供开关和“移除该页”，并在 Provider 不可用时隐藏该页。

这项改造建议以新模块 `launcher-tool-api` 为唯一对外契约；不要公开 `Workspace`、`Launcher` 或 `LauncherSDK` 实现类。

## 6. 搜索页添加广告位

### 当前实现与边界

当前 All Apps 搜索不是独立 Activity：`AllAppsSearchInput` 接收查询，`LawnchairSearchAlgorithm` 生成 `SearchAdapterItem`，`LawnchairSearchAdapterProvider` 再为 `SearchRecyclerView` 创建和绑定每行 View。

现有 Adapter 只认识 `LayoutType` 映射表中的结果类型，外部工具 App 没有广告位 SPI，不能从自身进程往搜索 RecyclerView 注入 View。

### 推荐实现：受控的 Sponsored Result Provider

将广告作为一种**明确标识的搜索结果行**，而不是浮层或全屏插屏。新增一个 Launcher 内部广告数据源及一个很小的跨进程 Provider 契约：

```kotlin
interface SearchPromotionProvider {
    suspend fun load(query: String, limit: Int): List<Promotion>
}

data class Promotion(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconUri: Uri?,
    val clickIntent: PendingIntent,
    val disclosure: String = "赞助内容",
)
```

在 Launcher 中完成以下四个改动：

1. **新增 View Type。** 在 `LawnchairSearchAdapterProvider` 增加 `SEARCH_RESULT_SPONSORED`，并映射到 `R.layout.search_result_sponsored`；`getItemsPerRow()` 返回 `1`，使其全宽显示。
2. **新增数据项。** 创建 `SponsoredSearchAdapterItem`（或扩展 `SearchAdapterItem`）保存受校验的展示字段和 `PendingIntent`；不要让外部 App 传入 View、HTML 或任意代码。
3. **合并结果。** 在 `LawnchairSearchAlgorithm.transformSearchResults()` 的结果生成后插入一条广告行。建议仅在非空 query、自然结果至少一条、且每次查询最多一条时展示；不得挤掉首个自然结果。
4. **绑定与度量。** 在 Provider 的 `onCreateViewHolder()` / `onBindView()` 中显示“赞助内容”标签、广告主名称和关闭/不感兴趣入口；曝光在至少 50% 可见且停留 1 秒后记一次，点击只通过校验后的 `PendingIntent.send()` 跳转。

建议插入规则：**第 3 个自然结果之后**；当结果少于 3 条时放在自然结果之后。广告行必须全宽、可被 TalkBack 正确读为“赞助内容”，且不参与回车的默认 quick-launch。

相关代码入口：

| 目的 | 文件 |
|---|---|
| 搜索输入、结果回调 | `lawnchair/src/app/lawnchair/allapps/AllAppsSearchInput.kt` |
| 搜索结果转换与分组背景 | `lawnchair/src/app/lawnchair/search/algorithms/LawnchairSearchAlgorithm.kt` |
| 自定义 View Type、ViewHolder、跨列规则 | `lawnchair/src/app/lawnchair/search/LawnchairSearchAdapterProvider.kt` |
| 结果数据模型 | `lawnchair/src/app/lawnchair/search/adapter/SearchAdapterItem.kt` |
| RecyclerView 网格跨度 | `src/com/android/launcher3/allapps/AllAppsGridAdapter.java` |

### Lawnchair 广告模块：接入 App 必须实现的内容

Lawnchair 已提供 `com.android.launcher3.ads` 通用模块，不携带任何广告网络 SDK、App ID 或广告单元 ID。接入方需在**最终打包 Launcher 的 App 模块**中实现 `IAdProvider`；`AdManager` 只负责广告位调度、回退、原生广告容器清理和原操作的继续执行。

> 若“接入 App”是另一个独立安装、不同进程的 APK，它不能直接实现并注入 `IAdProvider`（接口和 View 均在 Launcher 进程）。该模式需要另建 AIDL/服务协议，且仅传递经校验的数据，不应跨进程传递广告 SDK View。当前接口适用于产品 App 与 Lawnchair 一起编译、一起打包的场景。

#### `AdMobProvider` 应放在哪里

参考工程的 `com.android.launcher3.ads.launcher.AdMobProvider` **不应直接放入 Lawnchair 核心模块**，而应由接入 App 实现，例如 `com.yourapp.ads.ProductAdMobProvider : IAdProvider`。原因是该实现会直接依赖 Google Mobile Ads 的插屏、激励、App Open、原生广告 API，以及 Firebase/Singular 等产品级收入与归因能力；同时还需要使用该产品自己的 App ID 和各广告位 unit id。

推荐分层：

```text
Lawnchair 核心
└── com.android.launcher3.ads
    ├── AdManager / IAdProvider
    ├── ConsentManager / AdRequestPolicy
    └── 广告位与容器调度

最终产品 App
└── com.yourapp.ads
    ├── ProductAdMobProvider : IAdProvider
    ├── ProductConsentManager / ProductAdRequestPolicy
    └── BuildConfig、CI 私密变量中的 App ID / unit id
```

若多个产品都确定使用 AdMob，可单独发布可选的 `ads-admob` 扩展模块来提供通用 `AdMobProvider`，但它仍必须由最终产品 App 显式依赖、传入配置并负责广告 SDK 的 Manifest 声明。不要让 Lawnchair 默认产物传递依赖任何广告网络 SDK。

#### 已预留广告位

| 广告位 | 格式 | 当前 Lawnchair 调用状态 |
|---|---|---|
| `SPLASH_FULLSCREEN` | App Open | 预留给启动页 |
| `ONBOARDING_COMPLETE_FULLSCREEN` | 插屏 | 预留给引导/同意完成页 |
| `APP_ICON_LAUNCH_FULLSCREEN` | 插屏 | 已在桌面与全部应用图标启动前调用 |
| `WORKSPACE_LONG_PRESS_FULLSCREEN` | 插屏 | 已在桌面空白处长按菜单前调用 |
| `LAUNCHER_RESUME_APP_OPEN` | App Open | 已在 Launcher 恢复时预加载，并作为门控回退候选 |
| `SEARCH_LANDING_NATIVE` / `SEARCH_PAGE_NATIVE` | 原生 | 前者已接入自定义搜索落地页；后者供独立搜索页使用 |
| `WEATHER_PAGE_NATIVE` | 原生 | 预留给产品天气页 |
| `ALL_APPS_NATIVE_FIRST` / `ALL_APPS_NATIVE_SECOND` | 原生 | 预留给全部应用列表的两个广告行 |
| `DIALOG_GATE_FULLSCREEN` | 插屏 | 可通过 `AdDialogGate` 接入任意产品操作 |

#### 产品 App 的最小实现

1. 在产品模块添加所选广告网络/聚合 SDK、网络权限和该 SDK 需要的 Manifest `meta-data`；App ID 放在构建变量或私有配置中。
2. 实现 `IAdProvider`：`initialize` 初始化 SDK，`loadAd` 使用对应广告位的 unit id 预加载，`play*` 展示已缓存广告。全屏广告关闭或失败必须回调 `onDismissed` / `onFailed`，否则 Launcher 的原操作不会继续。
3. 对原生广告，`playNative` 在加载成功后将渲染 View 添加到传入 `ViewGroup`，失败调用 `onFailed`；`destroyNativeAd` 必须销毁 SDK 的 `NativeAd` 对象和监听器。
4. 实现 `ConsentManager`（CMP/地区隐私同意）和 `AdRequestPolicy`（远程开关、频控、冷启动保护、测试设备）。任一项不允许时返回 `false`，Lawnchair 将无广告继续原操作。
5. 在产品 `Application.onCreate()` 中、`LawnchairApp` 初始化之前或之后安装 Provider：

```kotlin
AdManager.install(
    provider = ProductAdProvider(),
    configurationProvider = {
        AdConfiguration(
            appId = BuildConfig.ADS_APP_ID,
            unitIds = mapOf(
                AdPlacement.APP_ICON_LAUNCH_FULLSCREEN to BuildConfig.AD_UNIT_LAUNCH,
                AdPlacement.WORKSPACE_LONG_PRESS_FULLSCREEN to BuildConfig.AD_UNIT_MENU,
                AdPlacement.SEARCH_LANDING_NATIVE to BuildConfig.AD_UNIT_SEARCH_NATIVE,
            ),
            enabled = !BuildConfig.DEBUG && BuildConfig.ADS_ENABLED,
        )
    },
    consentManager = ProductConsentManager(),
    requestPolicy = ProductAdRequestPolicy(),
)
```

`AdManager.install()` 可在 `LawnchairApp.onCreate()` 前后调用；若 Launcher 已创建，会立即尝试初始化。未安装、`enabled=false`、缺少 unit id、未同意或频控拒绝时均为安全 no-op。

#### 接入验收

- 使用广告平台测试 App ID / 测试 unit id 和测试设备完成联调，生产 ID 不写入仓库。
- 验证初始化、无网、加载失败、关闭广告、旋转、Activity 销毁和回到前台时，原操作均只执行一次。
- 原生广告必须含平台要求的“广告/赞助”标识、关闭/不感兴趣入口及无障碍描述。
- 发布前复核 GDPR/CCPA/当地法规、儿童/家庭政策、数据安全声明与应用商店广告政策。

### 广告安全与产品要求

- 默认关闭；首次开启前明确告知数据用途，并可在 Launcher 设置中关闭个性化/广告。
- 只在用户输入查询后请求；不要上传完整已安装应用列表、主页布局或联系人数据。
- Provider 需签名校验、每次请求超时（建议 300 ms 预算）和缓存；超时/失败时不显示广告，不阻塞本地搜索。
- 使用 HTTPS、最小化日志；广告 SDK 仅由最终产品 App 的 `IAdProvider` 承载，Launcher 核心不直接依赖任何广告网络或远程 WebView。
- 始终显示“赞助内容”，不得模拟自然搜索结果；广告点击、关闭和频控均应可审计。

## 7. 上线前验收清单

- [ ] 工具 App 与 Release Launcher 使用预期签名；负一屏 Provider 已写入 `FeedBridge` 白名单。
- [ ] Provider 被发现、在设置中可选择、绑定失败后主页仍可正常使用。
- [ ] RTL、旋转、返回键、锁屏/解锁、进程重启以及 Provider 被卸载均已验证。
- [ ] 自定义主页页在 Provider 不可用时自动隐藏，不影响拖拽/添加 Widget/页面删除。
- [ ] 搜索广告只在允许条件下出现，有“赞助内容”标识、关闭入口、频控和无网降级。
- [ ] 搜索广告未改变自然结果排序与默认 quick-launch 行为。
- [ ] Release 构建执行签名校验；没有依赖调试开关或 `ignoreFeedWhitelist`。

## 8. 代码依据

- Maven 发布：`build.gradle` 的 `publishing` 配置。
- 内置 Overlay API：`lawnchair/src/app/lawnchair/LauncherSDK.kt`。
- 负一屏发现、白名单和签名校验：`lawnchair/src/app/lawnchair/FeedBridge.kt`。
- Feed AIDL 协议：`lawnchair/aidl/com/google/android/libraries/launcherclient/`。
- Workspace Overlay 手势接入：`src/com/android/launcher3/Workspace.java` 与 `src_plugins/com/android/systemui/plugins/shared/LauncherOverlayManager.java`。
- 搜索扩展链路：`AllAppsSearchInput.kt`、`LawnchairSearchAlgorithm.kt`、`LawnchairSearchAdapterProvider.kt`。
