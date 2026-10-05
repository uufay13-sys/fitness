package com.example.data.health

import android.content.Context
import com.example.data.local.HealthDao
import com.example.data.model.HealthActivityEntity
import com.example.data.model.HealthConnectionStatus
import com.example.data.model.HealthPermissionType
import com.example.data.model.HealthPrivacySettingsEntity
import com.example.data.model.HealthSummaryEntity
import com.example.data.model.WorkoutLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class HealthService(
    private val context: Context,
    private val healthDao: HealthDao,
    private val scope: CoroutineScope
) {
    val bridge = HealthDataBridge(context)

    private val _connectionStatus = MutableStateFlow(HealthConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<HealthConnectionStatus> = _connectionStatus.asStateFlow()

    init {
        scope.launch {
            // Restore persistent connection state
            val settings = healthDao.getPrivacySettingsOnce()
            if (settings != null && settings.isConnected) {
                _connectionStatus.value = HealthConnectionStatus.CONNECTED
                bridge.setConnectionStatus(HealthConnectionStatus.CONNECTED)
            } else {
                _connectionStatus.value = HealthConnectionStatus.DISCONNECTED
            }
        }
    }

    fun markPermissionDenied() {
        _connectionStatus.value = HealthConnectionStatus.PERMISSION_DENIED
        bridge.setConnectionStatus(HealthConnectionStatus.PERMISSION_DENIED)
    }

    fun markUnavailable() {
        _connectionStatus.value = HealthConnectionStatus.UNAVAILABLE
        bridge.setConnectionStatus(HealthConnectionStatus.UNAVAILABLE)
    }

    fun getTodaySummary(dateStr: String): Flow<HealthSummaryEntity?> {
        return healthDao.getSummaryForDate(dateStr)
    }

    fun getRecentSummaries(): Flow<List<HealthSummaryEntity>> {
        return healthDao.getRecentSummaries()
    }

    fun getTodayActivities(dateStr: String): Flow<List<HealthActivityEntity>> {
        return healthDao.getActivitiesForDate(dateStr)
    }

    val privacySettings: Flow<HealthPrivacySettingsEntity?> = healthDao.getPrivacySettings()

    suspend fun connect(authorizedTypes: List<HealthPermissionType>, dateStr: String) = withContext(Dispatchers.IO) {
        val jsonArray = JSONArray()
        authorizedTypes.forEach { jsonArray.put(it.name) }

        val newSettings = HealthPrivacySettingsEntity(
            isConnected = true,
            connectionStatus = "CONNECTED",
            allowAiAccessToFitnessData = true,
            authorizedPermissionsJson = jsonArray.toString(),
            lastSyncedTimestamp = System.currentTimeMillis()
        )
        healthDao.savePrivacySettings(newSettings)
        _connectionStatus.value = HealthConnectionStatus.CONNECTED
        bridge.setConnectionStatus(HealthConnectionStatus.CONNECTED)

        // Read initial sensor steps or existing summary
        val existing = healthDao.getSummaryForDateOnce(dateStr)
        val sensorSteps = bridge.readSteps()
        val currentSteps = if (sensorSteps > 0) sensorSteps else (existing?.steps ?: 0)
        val distance = currentSteps * 0.00075f // ~0.75m per step in km
        val calories = (currentSteps * 0.04f).toInt()

        healthDao.saveSummary(
            HealthSummaryEntity(
                dateStr = dateStr,
                steps = currentSteps,
                distanceKm = distance,
                activeCalories = calories,
                workoutCount = existing?.workoutCount ?: 0,
                avgHeartRate = existing?.avgHeartRate ?: if (currentSteps > 0) 102 else null,
                sleepHours = existing?.sleepHours,
                isConnected = true,
                lastSyncedTimestamp = System.currentTimeMillis()
            )
        )

        // If authorized for steps and steps were detected, log walking activity record in timeline
        val currentActivities = healthDao.getActivitiesForDate(dateStr)
        if (currentSteps > 0) {
            val walkMins = (currentSteps / 100).coerceIn(10, 120)
            healthDao.insertActivity(
                HealthActivityEntity(
                    dateStr = dateStr,
                    activityType = "Walking",
                    startTime = System.currentTimeMillis() - (walkMins * 60 * 1000L),
                    durationMinutes = walkMins,
                    distanceKm = distance,
                    calories = calories,
                    avgHeartRate = 104
                )
            )
        }
    }

    suspend fun recordActivity(
        dateStr: String,
        activityType: String,
        durationMinutes: Int,
        distanceKm: Float,
        calories: Int,
        avgHeartRate: Int? = null
    ) = withContext(Dispatchers.IO) {
        val settings = healthDao.getPrivacySettingsOnce()
        if (settings == null || !settings.isConnected) return@withContext

        healthDao.insertActivity(
            HealthActivityEntity(
                dateStr = dateStr,
                activityType = activityType,
                startTime = System.currentTimeMillis() - (durationMinutes * 60 * 1000L),
                durationMinutes = durationMinutes,
                distanceKm = distanceKm,
                calories = calories,
                avgHeartRate = avgHeartRate
            )
        )

        val existing = healthDao.getSummaryForDateOnce(dateStr)
        val newCalories = (existing?.activeCalories ?: 0) + calories
        val newDistance = (existing?.distanceKm ?: 0f) + distanceKm
        val newWorkouts = (existing?.workoutCount ?: 0) + (if (activityType == "Workout") 1 else 0)

        healthDao.saveSummary(
            HealthSummaryEntity(
                dateStr = dateStr,
                steps = existing?.steps ?: bridge.readSteps(),
                distanceKm = newDistance,
                activeCalories = newCalories,
                workoutCount = newWorkouts,
                avgHeartRate = avgHeartRate ?: existing?.avgHeartRate,
                sleepHours = existing?.sleepHours,
                isConnected = true,
                lastSyncedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun disconnect(clearLocalData: Boolean) = withContext(Dispatchers.IO) {
        val currentSettings = healthDao.getPrivacySettingsOnce() ?: HealthPrivacySettingsEntity()
        healthDao.savePrivacySettings(
            currentSettings.copy(
                isConnected = false,
                connectionStatus = "DISCONNECTED"
            )
        )
        _connectionStatus.value = HealthConnectionStatus.DISCONNECTED
        bridge.disconnect()

        if (clearLocalData) {
            healthDao.clearAllSummaries()
            healthDao.clearAllActivities()
        }
    }

    suspend fun setAiAccessEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        val currentSettings = healthDao.getPrivacySettingsOnce() ?: HealthPrivacySettingsEntity()
        healthDao.savePrivacySettings(
            currentSettings.copy(allowAiAccessToFitnessData = enabled)
        )
    }

    suspend fun syncWorkoutToHealth(workoutLog: WorkoutLog, dateStr: String) = withContext(Dispatchers.IO) {
        val settings = healthDao.getPrivacySettingsOnce()
        if (settings == null || !settings.isConnected) return@withContext

        // Record workout session in activity timeline
        val durationMins = (workoutLog.durationSeconds / 60).toInt().coerceAtLeast(1)
        val estimatedWorkoutCalories = (durationMins * 7).coerceAtLeast(25) // standard metabolic equivalent

        healthDao.insertActivity(
            HealthActivityEntity(
                dateStr = dateStr,
                activityType = "Workout",
                startTime = workoutLog.startedAt,
                durationMinutes = durationMins,
                distanceKm = 0f,
                calories = estimatedWorkoutCalories,
                avgHeartRate = 138
            )
        )

        // Update daily summary
        val existing = healthDao.getSummaryForDateOnce(dateStr)
        val currentWorkouts = (existing?.workoutCount ?: 0) + 1
        val currentCalories = (existing?.activeCalories ?: 0) + estimatedWorkoutCalories

        healthDao.saveSummary(
            HealthSummaryEntity(
                dateStr = dateStr,
                steps = existing?.steps ?: bridge.readSteps(),
                distanceKm = existing?.distanceKm ?: 0f,
                activeCalories = currentCalories,
                workoutCount = currentWorkouts,
                avgHeartRate = 136,
                sleepHours = existing?.sleepHours ?: 7.5f,
                isConnected = true,
                lastSyncedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun refreshDailySummary(dateStr: String) = withContext(Dispatchers.IO) {
        val settings = healthDao.getPrivacySettingsOnce()
        if (settings == null || !settings.isConnected) return@withContext

        val sensorSteps = bridge.readSteps()
        val existing = healthDao.getSummaryForDateOnce(dateStr)
        val steps = if (sensorSteps > 0) sensorSteps else (existing?.steps ?: 0)
        val distance = steps * 0.00075f
        val calories = (steps * 0.04f).toInt() + (existing?.activeCalories ?: 0)

        healthDao.saveSummary(
            HealthSummaryEntity(
                dateStr = dateStr,
                steps = steps,
                distanceKm = distance,
                activeCalories = calories,
                workoutCount = existing?.workoutCount ?: 0,
                avgHeartRate = existing?.avgHeartRate,
                sleepHours = existing?.sleepHours,
                isConnected = true,
                lastSyncedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteLocalFitnessData() = withContext(Dispatchers.IO) {
        healthDao.clearAllSummaries()
        healthDao.clearAllActivities()
    }
}
