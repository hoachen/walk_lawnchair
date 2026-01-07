package com.ur.apps.walk.model

import android.content.Context
import com.ur.apps.utils.URLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LeaderboardRepository(private val context: Context) {
    private val TAG = "LeaderboardRepository"
    private val dao = LeaderboardDatabase.getInstance(context).leaderboardDao()
    
    suspend fun initializeDataIfNeeded() {
        withContext(Dispatchers.IO) {
            try {
                // 检查数据库是否已有数据
                val count = dao.getDataCount(0)
                URLog.d(TAG, "检查数据库数据: dataType=0 的记录数: $count")
                
                if (count == 0) {
                    URLog.d(TAG, "初始化排行榜数据")
                    
                    // 导入user_pool.json文件中的数据
                    val jsonFileName = "user_pool.json"
                    importUserPoolData(jsonFileName)
                    
                    // 验证导入是否成功
                    val count0 = dao.getDataCount(0)
                    val count1 = dao.getDataCount(1)
                    val count2 = dao.getDataCount(2)
                    URLog.d(TAG, "导入后数据统计 - 昨日: $count0, 七天: $count1, 三十天: $count2")
                } else {
                    URLog.d(TAG, "排行榜数据已存在，无需初始化")
                }
            } catch (e: Exception) {
                URLog.e(TAG, "初始化数据出错", e)
                e.printStackTrace()
            }
        }
    }
    
    private suspend fun importUserPoolData(jsonFileName: String) {
        try {
            URLog.d(TAG, "开始从 $jsonFileName 导入数据")
            val assetFile = context.assets.open(jsonFileName)
            val json = assetFile.bufferedReader().use { it.readText() }
            URLog.d(TAG, "读取到JSON数据: ${json.take(100)}...")
            
            // 使用自定义数据类来解析JSON
            val type = object : TypeToken<List<UserPoolItem>>() {}.type
            val userPoolItems = Gson().fromJson<List<UserPoolItem>>(json, type)
            URLog.d(TAG, "解析到 ${userPoolItems.size} 条用户数据")
            
            // 将UserPoolItem转换为三种不同时间段的LeaderboardUser
            val yesterdayUsers = userPoolItems.map { 
                LeaderboardUser(
                    id = it.userId,
                    nickname = it.nickname,
                    avatarUrl = it.avatarUrl,
                    country = it.country,
                    coins = it.coinsYesterday,
                    dataType = 0 // 昨天
                )
            }
            
            val sevenDaysUsers = userPoolItems.map { 
                LeaderboardUser(
                    id = it.userId,
                    nickname = it.nickname,
                    avatarUrl = it.avatarUrl,
                    country = it.country,
                    coins = it.coins7Days,
                    dataType = 1 // 7天
                )
            }
            
            val thirtyDaysUsers = userPoolItems.map { 
                LeaderboardUser(
                    id = it.userId,
                    nickname = it.nickname,
                    avatarUrl = it.avatarUrl,
                    country = it.country,
                    coins = it.coins30Days,
                    dataType = 2 // 30天
                )
            }
            
            // 按金币数量排序
            val sortedYesterdayUsers = yesterdayUsers.sortedByDescending { it.coins }
            val sortedSevenDaysUsers = sevenDaysUsers.sortedByDescending { it.coins }
            val sortedThirtyDaysUsers = thirtyDaysUsers.sortedByDescending { it.coins }
            
            // 插入数据库
            dao.insertAll(sortedYesterdayUsers)
            URLog.d(TAG, "成功导入昨日数据: ${sortedYesterdayUsers.size} 条")
            
            dao.insertAll(sortedSevenDaysUsers)
            URLog.d(TAG, "成功导入7天数据: ${sortedSevenDaysUsers.size} 条")
            
            dao.insertAll(sortedThirtyDaysUsers)
            URLog.d(TAG, "成功导入30天数据: ${sortedThirtyDaysUsers.size} 条")
            
            URLog.d(TAG, "所有数据导入完成")
        } catch (e: Exception) {
            URLog.e(TAG, "导入 $jsonFileName 数据失败", e)
            e.printStackTrace()
        }
    }
    
    suspend fun getLeaderboardData(dataType: Int): List<LeaderboardUser> {
        return withContext(Dispatchers.IO) {
            try {
                URLog.d(TAG, "开始查询 dataType=$dataType 的排行榜数据")
                val users = dao.getLeaderboardByType(dataType)
                URLog.d(TAG, "查询到 ${users.size} 条数据，dataType=$dataType")
                
                if (users.isEmpty()) {
                    URLog.w(TAG, "警告: dataType=$dataType 的排行榜数据为空!")
                } else {
                    URLog.d(TAG, "数据示例: ${users.take(2)}")
                }
                
                // 设置排名
                users.mapIndexed { index, user ->
                    user.apply { rank = index + 1 }
                }
            } catch (e: Exception) {
                URLog.e(TAG, "获取排行榜数据出错", e)
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    // 用于解析user_pool.json的数据类
    private data class UserPoolItem(
        val userId: String,
        val nickname: String,
        val avatarUrl: String,
        val country: String,
        val coinsYesterday: Int,
        val coins7Days: Int,
        val coins30Days: Int
    )
} 