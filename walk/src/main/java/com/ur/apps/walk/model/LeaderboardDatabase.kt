package com.ur.apps.walk.model

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Dao
interface LeaderboardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<LeaderboardUser>)
    
    @Query("SELECT * FROM leaderboard_users WHERE dataType = :dataType ORDER BY coins DESC")
    suspend fun getLeaderboardByType(dataType: Int): List<LeaderboardUser>
    
    @Query("SELECT COUNT(*) FROM leaderboard_users WHERE dataType = :dataType")
    suspend fun getDataCount(dataType: Int): Int
}

@Database(entities = [LeaderboardUser::class], version = 2, exportSchema = false)
abstract class LeaderboardDatabase : RoomDatabase() {
    abstract fun leaderboardDao(): LeaderboardDao
    
    companion object {
        @Volatile
        private var INSTANCE: LeaderboardDatabase? = null
        
        fun getInstance(context: Context): LeaderboardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LeaderboardDatabase::class.java,
                    "leaderboard_database"
                )
                .fallbackToDestructiveMigration() // 强制迁移，会删除旧表
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 