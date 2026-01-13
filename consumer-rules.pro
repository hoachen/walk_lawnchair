-keep class com.tenjin.** { *; }
-keep public class com.google.android.gms.ads.identifier.** { *; }
-keep public class com.google.android.gms.common.** { *; }
-keep public class com.android.installreferrer.** { *; }
-keep class * extends java.util.ListResourceBundle {
    protected java.lang.Object[][] getContents();
}
-keepattributes *Annotation*


-keep class XI.CA.XI.**{*;}
-keep class XI.K0.XI.**{*;}
-keep class XI.XI.K0.**{*;}
-keep class XI.xo.XI.XI.**{*;}
-keep class com.asus.msa.SupplementaryDID.**{*;}
-keep class com.asus.msa.sdid.**{*;}
-keep class com.bun.lib.**{*;}
-keep class com.bun.miitmdid.**{*;}
-keep class com.huawei.hms.ads.identifier.**{*;}
-keep class com.samsung.android.deviceidservice.**{*;}
-keep class com.zui.opendeviceidlibrary.**{*;}
-keep class org.json.**{*;}
#-keep public class com.netease.nis.sdkwrapper.Utils {public <methods>;}

# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.tenjin.** { *; }
-keep public class com.google.android.gms.ads.identifier.** { *; }
-keep public class com.google.android.gms.common.** { *; }
-keep public class com.android.installreferrer.** { *; }
-keep class * extends java.util.ListResourceBundle {
    protected java.lang.Object[][] getContents();
}
-keepattributes *Annotation*

-keep class XI.CA.XI.**{*;}
-keep class XI.K0.XI.**{*;}
-keep class XI.XI.K0.**{*;}
-keep class XI.xo.XI.XI.**{*;}
-keep class com.asus.msa.SupplementaryDID.**{*;}
-keep class com.asus.msa.sdid.**{*;}
-keep class com.bun.lib.**{*;}
-keep class com.bun.miitmdid.**{*;}
-keep class com.huawei.hms.ads.identifier.**{*;}
-keep class com.samsung.android.deviceidservice.**{*;}
-keep class com.zui.opendeviceidlibrary.**{*;}
-keep class org.json.**{*;}
-keep public class com.netease.nis.sdkwrapper.Utils {public <methods>;}

-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}


# 保留所有入口类
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

-keepclassmembers class * {
    *;
}
-keepattributes *Annotation*
-keep public class * extends java.lang.reflect.Member

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object readResolve();
    java.lang.Object writeReplace();
}

-keep public class * extends android.view.View
-keep public class * extends android.view.ViewGroup

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepattributes Signature
-keepattributes *Annotation*
-keep public class * {
    public protected private *;
}

-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepattributes Signature

-keep class com.sg.response.**{*;}
-keep interface com.sg.response.**{*;}

-keep class com.sg.request.**{*;}
-keep interface com.sg.request.**{*;}


-keep class com.sg.model.**{*;}
-keep interface com.sg.model.**{*;}


-keep class com.sg.api.**{*;}
-keep interface com.sg.api.**{*;}

-keep class com.ur.apps.walk.model.**{*;}
-keep interface com.ur.apps.walk.model.**{*;}

-keep class com.ur.apps.walk.withdraw.**{*;}
-keep interface com.ur.apps.walk.withdraw.**{*;}

-keep class com.ur.apps.walk.settings.**{*;}
-keep interface com.ur.apps.walk.settings.**{*;}

-keep class com.ur.apps.walk.step.bean.**{*;}
-keep interface com.ur.apps.walk.step.bean.**{*;}

-keep class com.ur.apps.walk.step.bean.**{*;}
-keep interface com.ur.apps.walk.step.bean.**{*;}

-keep class com.ur.apps.walk.step.dao.**{*;}
-keep interface com.ur.apps.walk.step.dao.**{*;}


-keep class cn.shuzilm.core.** {*;}


# Vungle
-dontwarn com.vungle.ads.**
-keepclassmembers class com.vungle.ads.** {
  *;
}
-keep class com.vungle.ads.**



# Google
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**




# START OkHttp + Okio
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**


# A resource is loaded with a relative path so the package of this class must be preserved.
-adaptresourcefilenames okhttp3/internal/publicsuffix/PublicSuffixDatabase.gz


# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*


# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**


# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*


# END OkHttp + Okio


# START Protobuf
-dontwarn com.google.protobuf.**
-keepclassmembers class com.google.protobuf.** {
 *;
}
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }


# END Protobuf
-keep class com.bytedance.sdk.** { *; }
-keep class com.inmobi.** { *; }
-keep public class com.google.android.gms.**
-dontwarn com.google.android.gms.**
-dontwarn com.squareup.picasso.**
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient{
     public *;
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info{
     public *;
}
# skip the Picasso library classes
-keep class com.squareup.picasso.** {*;}
-dontwarn com.squareup.okhttp.**
# skip Moat classes
-keep class com.moat.** {*;}
-dontwarn com.moat.**
# skip IAB classes
-keep class com.iab.** {*;}
-dontwarn com.iab.**
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.mbridge.** {*; }
-keep interface com.mbridge.** {*; }
-keep class android.support.v4.** { *; }
-dontwarn com.mbridge.**
-keep class **.R$* { public static final int mbridge*; }
-keep public class com.mbridge.* extends androidx.** { *; }
-keep public class androidx.viewpager.widget.PagerAdapter{ *; }
#-keep public class androidx.viewpager.widget.ViewPager.OnPageChangeListener{ *; }
-keep interface androidx.annotation.IntDef{ *; }
-keep interface androidx.annotation.Nullable{ *; }
-keep interface androidx.annotation.CheckResult{ *; }
-keep interface androidx.annotation.NonNull{ *; }
-keep public class androidx.fragment.app.Fragment{ *; }
-keep public class androidx.core.content.FileProvider{ *; }
-keep public class androidx.core.app.NotificationCompat{ *; }
-keep public class androidx.appcompat.widget.AppCompatImageView { *; }
-keep public class androidx.recyclerview.*{ *; }
-keep class com.chartboost.** { *; }

-keep class com.tenjin.** { *; }
-keep public class com.google.android.gms.ads.identifier.** { *; }
-keep public class com.google.android.gms.common.** { *; }
-keep public class com.android.installreferrer.** { *; }
-keep class * extends java.util.ListResourceBundle {
    protected java.lang.Object[][] getContents();
}
-keepattributes *Annotation*


-keep class XI.CA.XI.**{*;}
-keep class XI.K0.XI.**{*;}
-keep class XI.XI.K0.**{*;}
-keep class XI.xo.XI.XI.**{*;}
-keep class com.asus.msa.SupplementaryDID.**{*;}
-keep class com.asus.msa.sdid.**{*;}
-keep class com.bun.lib.**{*;}
-keep class com.bun.miitmdid.**{*;}
-keep class com.huawei.hms.ads.identifier.**{*;}
-keep class com.samsung.android.deviceidservice.**{*;}
-keep class com.zui.opendeviceidlibrary.**{*;}
-keep class org.json.**{*;}
#-keep public class com.netease.nis.sdkwrapper.Utils {public <methods>;}

-keep class com.bigossp.** { *; }
-keep class com.google.ads.mediation.bigo.** { *; }


# --- 1. AdMob 通用规则 ---
-keep public class com.google.android.gms.ads.** { *; }
-keep public class com.google.ads.mediation.** { *; }

# --- 2. Meta (Facebook) ---
-keep class com.facebook.ads.** { *; }
-keep class com.google.ads.mediation.facebook.** { *; }

# --- 3. Unity Ads ---
-keep class com.unity3d.ads.** { *; }
-keep class com.unity3d.services.** { *; }
-keep class com.google.ads.mediation.unity.** { *; }

# --- 4. Liftoff (Vungle) ---
-keep class com.vungle.warren.** { *; }
-keep class com.google.ads.mediation.vungle.** { *; }

# --- 5. Chartboost ---
-keep class com.chartboost.sdk.** { *; }
-keep class com.google.ads.mediation.chartboost.** { *; }

# --- 6. Fyber ---
-keep class com.fyber.** { *; }
-keep class com.google.ads.mediation.fyber.** { *; }

# --- 7. IronSource ---
-keep class com.ironsource.** { *; }
-keep class com.google.ads.mediation.ironsource.** { *; }

# --- 8. Pangle (TikTok) ---
-keep class com.bytedance.sdk.openadsdk.** { *; }
-keep class com.google.ads.mediation.pangle.** { *; }

# --- 9. Mintegral ---
-keep class com.mbridge.msdk.** { *; }
-keep class com.google.ads.mediation.mintegral.** { *; }

# --- 10. InMobi ---
-keep class com.inmobi.** { *; }
-keep class com.google.ads.mediation.inmobi.** { *; }

# --- 11. Bigo ---
-keep class com.bigossp.** { *; }
-keep class com.google.ads.mediation.bigo.** { *; }

# --- 通用：保留所有中介适配器类，防止被 R8 优化掉 ---
-keep class * extends com.google.android.gms.ads.mediation.MediationAdapter { *; }
-keep class * extends com.google.android.gms.ads.mediation.Adapter { *; }
-keep class com.bytedance.sdk.** { *; }
