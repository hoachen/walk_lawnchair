# Lawnchair SDK 集成文档

## 概述

本文档说明如何通过 Maven 方式将 Lawnchair SDK 集成到您的 Android 项目中。

**最新版本**: `1.0.13`

## 1. 引入依赖

### 1.1 配置 Maven 仓库

在项目根目录的 `settings.gradle` (或项目级 `build.gradle`) 中配置 Maven 仓库地址：

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    
        maven { url uri('/path/to/lawnchair_gitcode/build/repo') }
    }
}
```

### 1.2 添加项目依赖

在模块级 `build.gradle` (例如 `app/build.gradle`) 中添加 SDK 依赖：

```gradle
dependencies {
    // Release 版本 (推荐)
    implementation 'com.lawnchair:launcher-sdk:1.0.13'

  
}
```

## 2. 初始化配置

在您的 `Application` 类中初始化 SDK：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. 初始化 SDK (必需)
        app.lawnchair.LauncherSDK.init(this)

        // 2. (可选) 配置负一屏
        // 开启/关闭负一屏 (默认开启)
        app.lawnchair.LauncherSDK.isOverlayEnabled = true
        
        // 自定义负一屏内容视图
        // app.lawnchair.LauncherSDK.overlayProvider = object : app.lawnchair.LauncherSDK.OverlayProvider {
        //     override fun createView(context: Context): View {
        //         return MyCustomView(context)
        //     }
        // }
    }
}
```

## 3. 混淆策略 (ProGuard Rules)

如果在 Release 构建中开启了混淆 (`minifyEnabled true`)，**必须**将以下规则添加到您的 `proguard-rules.pro` 文件中，否则会导致运行时崩溃或功能缺失。

```proguard
# ================= Lawnchair SDK 混淆规则 =================

# 1. 核心 SDK 类
-keep class app.lawnchair.** { *; }
-keep class com.android.launcher3.** { *; }

# 2. Protocol Buffers (SDK 内部使用)
-keep class com.google.protobuf.** { *; }

# 3. 网络库 (Retrofit & OkHttp)
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# 4. 数据库 (Room)
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**
```

## 4. 权限说明

SDK 内部已经声明了必要的权限。如果您的应用 targetSdk >= 30 (Android 11+)，SDK 会自动合并以下 `<queries>` 规则到您的清单文件中，以确保应用发现（App Discovery）、微件（Widget）和图标包（Icon Pack）功能正常工作。

您**不需要**手动添加

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
    </intent>
  
    <intent>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent>
    <intent>
        <action android:name="com.novalauncher.THEME" />
    </intent>
    <intent>
        <action android:name="org.adw.launcher.icons.ACTION_PICK_ICON" />
    </intent>

</queries>
```
