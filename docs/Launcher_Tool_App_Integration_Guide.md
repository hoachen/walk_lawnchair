# Walk Lawnchair：工具 App 接入与页面定制指南

**适用发布物：** `com.lawnchair:launcher-sdk:1.0.15`（Release）与 `com.lawnchair:launcher-sdk-debug:1.0.15`（Debug）  
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
    implementation("com.lawnchair:launcher-sdk:1.0.15")
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

`CustomFeedOverlay` 会在 `createOverlayView()` 调用该 Provider，并把 View 放入 Launcher 的 `DragLayer`。该方式可直接使用普通 View/Compose 容器，开发成本最低。

**重要：** 这段代码必须在 Lawnchair 进程、且在创建 `CustomFeedOverlay` 前执行。独立工具 App 无法借由 Maven AAR 或反射设置它。

内置默认 Feed 的可定制文件：

| 内容 | 位置 |
|---|---|
| 覆盖层生命周期、手势、进出动画 | `lawnchair/src/app/lawnchair/overlay/CustomFeedOverlay.kt` |
| Feed 数据模型 | `lawnchair/src/app/lawnchair/overlay/FeedItem.kt` |
| Feed 列表渲染 | `lawnchair/src/app/lawnchair/overlay/CustomFeedAdapter.kt` |
| 默认 Overlay 入口 | `lawnchair/src/app/lawnchair/LawnchairLauncher.kt` |

## 5. Launcher 主页面如何定制

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

### 广告安全与产品要求

- 默认关闭；首次开启前明确告知数据用途，并可在 Launcher 设置中关闭个性化/广告。
- 只在用户输入查询后请求；不要上传完整已安装应用列表、主页布局或联系人数据。
- Provider 需签名校验、每次请求超时（建议 300 ms 预算）和缓存；超时/失败时不显示广告，不阻塞本地搜索。
- 使用 HTTPS、最小化日志；不得在 Launcher 进程加载第三方广告 SDK 或远程 WebView。
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
