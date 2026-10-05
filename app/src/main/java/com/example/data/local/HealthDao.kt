package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HealthActivityEntity
import com.example.data.model.HealthPrivacySettingsEntity
import com.example.data.model.HealthSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Query("SELECT * FROM health_daily_summary WHERE dateStr = :dateStr")
    fun getSummaryForDate(dateStr: String): Flow<HealthSummaryEntity?>

    @Query("SELECT * FROM health_daily_summary WHERE dateStr = :dateStr")
    suspend fun getSummaryForDateOnce(dateStr: String): HealthSummaryEntity?

    @Query("SELECT * FROM health_daily_summary ORDER BY dateStr DESC LIMIT 7")
    fun getRecentSummaries(): Flow<List<HealthSummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSummary(summary: HealthSummaryEntity)

    @Query("SELECT * FROM health_activities WHERE dateStr = :dateStr ORDER BY startTime DESC")
    fun getActivitiesForDate(dateStr: String): Flow<List<HealthActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: HealthActivityEntity): Long

    @Query("SELECT * FROM health_privacy_settings WHERE id = 1")
    fun getPrivacySettings(): Flow<HealthPrivacySettingsEntity?>

    @Query("SELECT * FROM health_privacy_settings WHERE id = 1")
    suspend fun getPrivacySettingsOnce(): HealthPrivacySettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePrivacySettings(settings: HealthPrivacySettingsEntity)

    @Query("DELETE FROM health_daily_summary")
    suspend fun clearAllSummaries()

    @Query("DELETE FROM health_activities")
    suspend fun clearAllActivities()
}
