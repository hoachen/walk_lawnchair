package com.ur.apps.lock

import com.ur.apps.lock.LockAdFormat.AD_FORMAT_BANNER
import com.ur.apps.lock.LockAdFormat.AD_FORMAT_INTERS
import com.ur.apps.lock.LockAdFormat.AD_FORMAT_NATIVE
import com.ur.apps.lock.LockAdFormat.AD_FORMAT_REWORD
import org.json.JSONObject

data class LockAdRecord(
    var saveDateTs: Long = System.currentTimeMillis(),
    var native : Int = 0,
    var banner : Int = 0,
    var inters : Int = 0,
    var reward : Int = 0
) {
    fun totalShowCount(): Int {
        return native + banner + inters + reward
    }

    fun resetRecord() {
        native = 0
        banner = 0
        inters = 0
        reward = 0
    }

    fun recordAdShow(adFormat: String) {
        saveDateTs = System.currentTimeMillis();
        when (adFormat) {
            AD_FORMAT_NATIVE -> {
                native = native + 1
            }
            AD_FORMAT_BANNER -> {
                banner = banner + 1
            }
            AD_FORMAT_INTERS -> {
                inters = inters + 1
            }
            AD_FORMAT_REWORD -> {
                reward = reward + 1
            }
        }
    }
}

fun LockAdRecord.toJSON() = JSONObject().apply {
    put("saveDateTs", saveDateTs)
    put("native", native)
    put("banner", banner)
    put("inters", inters)
    put("reward", reward)
}

fun LockAdRecord.parserJSON(json: JSONObject) = LockAdRecord(
    saveDateTs = json.optLong("saveDateTs", System.currentTimeMillis()),
    native= json.optInt("native", 0),
    banner = json.optInt("banner", 0),
    inters = json.optInt("inters", 0),
    reward = json.optInt("reward", 0))