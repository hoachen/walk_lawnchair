package com.ur.apps.walk.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// 使用复合主键：id + dataType
@Entity(tableName = "leaderboard_users", primaryKeys = ["id", "dataType"])
data class LeaderboardUser(
    val id: String,
    val nickname: String,
    val avatarUrl: String,
    val country: String,
    val coins: Int,
    var rank: Int = 0,
    val dataType: Int = 0 // 0: 昨天, 1: 7天, 2: 30天
) 