package com.ur.apps.lock

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName


data class LockAdFormats(
    @SerializedName("platform")
    val platform : String = "admob",

    @SerializedName("native")
    val native : Int = 50,

    @SerializedName("banner")
    val banner : Int = 50,

    @SerializedName("inters")
    val inters : Int = 0,

    @SerializedName("reward")
    val reward : Int = 0,

    @SerializedName("none")
    val none : Int = 0
) {

    fun getRandomAdType(): String {
        val totalWeight = native + banner + inters + reward + none

        // 如果总权重为0，直接返回NONE
        if (totalWeight == 0) {
            return LockAdFormat.AD_FORMAT_NONE
        }

        val random = (0 until totalWeight).random()

        // 正确计算累积权重
        val nativeEnd = native
        val bannerEnd = nativeEnd + banner
        val intersEnd = bannerEnd + inters
        val rewardEnd = intersEnd + reward
        // noneEnd = rewardEnd + none = totalWeight

        return when {
            random < nativeEnd -> LockAdFormat.AD_FORMAT_NATIVE
            random < bannerEnd -> LockAdFormat.AD_FORMAT_BANNER
            random < intersEnd -> LockAdFormat.AD_FORMAT_INTERS
            random < rewardEnd -> LockAdFormat.AD_FORMAT_REWORD
            else -> LockAdFormat.AD_FORMAT_NATIVE
        }
    }
}


data class LockAdConfig (

    @SerializedName("kg")
    val kg: Boolean = true,// 激励视频load的等待时长，单位：秒

    @SerializedName("country")
    val country : List<String> = emptyList(),

    @SerializedName("hide_country")
    val hideCountry: List<String> = listOf("HK", "SG"),

    @SerializedName("channel")
    val channel : List<String> = emptyList(),

    @SerializedName("hide_channel")
    val hideChannel : List<String> = listOf("organic"),

    @SerializedName("ad_conf")
    val adAdFormats : LockAdFormats = LockAdFormats(),

    @SerializedName("limit")
    val limit: Int = 1000,

    @SerializedName("ctr")
    val ctr : Int = 5,

    @SerializedName("itm")
    val itm : Int = 0,

    @SerializedName("launcher_show_inter")
    val launcherShowInterval: Long = 5* 60,
)


// 为 String 添加扩展函数，将 JSON 字符串解析为 LockAdConfig 对象
fun String.toLockAdConfig(): LockAdConfig? {
    return try {
        Gson().fromJson(this, LockAdConfig::class.java)
    } catch (e: JsonSyntaxException) {
        // 处理 JSON 解析异常
        e.printStackTrace()
        null
    }
}

fun LockAdConfig.toJson(): String {
    return Gson().toJson(this)
}
