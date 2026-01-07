package com.ur.apps.ad

import com.sg.request.RewardImpRequest
import java.util.UUID

class RewardAdRecord(
    val adTraceId : String = UUID.randomUUID().toString(),
    var scene : AdShowScene = AdShowScene.COMMON,
    var pbTs : Long = -1L,
    var peTs : Long = -1L,
    var ecpm : Float = -1F,
    var auid : String = "",
    var adPlatform:  String = PLATFORM_TOPON,
    var adFormat: String = FORMAT_REWARDED_VIDEO,
    var adNetwork : String = ""
) {
    fun getAdShowDuration(): Long {
        return if (peTs > 0) (peTs - pbTs) else 0
    }

    companion object {
        const val PLATFORM_TOPON = "TOPON"
        const val PLATFORM_MAX = "MAX"

        // 广告格式常量
        const val FORMAT_REWARDED_VIDEO = "REWARDED_VIDEO"
        const val FORMAT_INTERSTITIAL = "INTERSTITIAL"
    }
}



fun RewardAdRecord.toRewardImpRequest()  = RewardImpRequest(
    scene = this.scene.toString(),
    ecpm = this.ecpm,
    atid = this.adTraceId,
    auid = this.auid,
    pbTs = this.pbTs,
    peTs = this.peTs,
    pDur = this.getAdShowDuration(),
    adPlatform = this.adPlatform,
    adFormat = this.adFormat,
    adNetwork = this.adNetwork
)