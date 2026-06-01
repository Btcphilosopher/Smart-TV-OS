package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileName: String,
    val avatarColorHex: String,
    val parentalControlsEnabled: Boolean,
    val viewingLimitMinutes: Int,
    val preferredGenre: String
)

@Entity(tableName = "smart_tv_apps")
data class SmartTVApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val iconName: String,
    val installState: String, // "installed", "launched", "paused", "terminated", "updated"
    val version: String,
    val sizeMb: Double
)

@Entity(tableName = "tv_media_items")
data class TvMediaItem(
    @PrimaryKey val id: String,
    val title: String,
    val category: String, // "Movie", "TV Show", "Live Channel", "Playlist"
    val genre: String,
    val streamUrl: String,
    val durationSeconds: Int,
    val rating: String,
    val description: String,
    val thumbnailUrl: String
)

@Entity(tableName = "watch_history")
data class WatchHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int,
    val mediaId: String,
    val mediaTitle: String,
    val watchedDurationSeconds: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "iot_automation_logs")
data class IoTLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val logMessage: String,
    val actionType: String // "LIGHTING", "BLINDS", "AUDIO", "VOICE"
)

@Entity(tableName = "analytics_metrics")
data class AnalyticsMetric(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mediaId: String,
    val mediaTitle: String,
    val viewDurationSeconds: Int,
    val completionStatus: String, // "PARTIAL", "COMPLETED", "DROP_OUT"
    val deviceSessionId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TvOSDao {
    // Profiling
    @Query("SELECT * FROM user_profiles")
    fun getAllProfiles(): Flow<List<UserProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Delete
    suspend fun deleteProfile(profile: UserProfile)

    // Apps Management
    @Query("SELECT * FROM smart_tv_apps")
    fun getAllApps(): Flow<List<SmartTVApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: SmartTVApp)

    @Query("UPDATE smart_tv_apps SET installState = :state WHERE packageName = :packageName")
    suspend fun updateAppInstallState(packageName: String, state: String)

    @Query("DELETE FROM smart_tv_apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)

    // Content Management
    @Query("SELECT * FROM tv_media_items")
    fun getAllMedia(): Flow<List<TvMediaItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: List<TvMediaItem>)

    @Query("SELECT * FROM tv_media_items WHERE id = :id")
    suspend fun getMediaById(id: String): TvMediaItem?

    // Watch History
    @Query("SELECT * FROM watch_history WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getHistoryByProfile(profileId: Int): Flow<List<WatchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WatchHistory)

    @Query("DELETE FROM watch_history WHERE profileId = :profileId")
    suspend fun clearHistoryForProfile(profileId: Int)

    // IoT Logs
    @Query("SELECT * FROM iot_automation_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<IoTLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: IoTLog)

    @Query("DELETE FROM iot_automation_logs")
    suspend fun clearLogs()

    // Analytics
    @Query("SELECT * FROM analytics_metrics ORDER BY timestamp DESC")
    fun getAnalytics(): Flow<List<AnalyticsMetric>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: AnalyticsMetric)
}
