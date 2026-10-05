package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HealthConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    PERMISSION_DENIED,
    UNAVAILABLE
}

enum class HealthPermissionType(val label: String, val description: String) {
    STEPS("Steps & Walking", "Daily step count, walking velocity and movement consistency."),
    DISTANCE("Distance & Velocity", "Total active distance across walking, running and cycling."),
    CALORIES("Active Energy Burned", "Calories expended during active exertion."),
    WORKOUTS("Workout Sessions", "Exercise types, duration, and synchronized training logs."),
    HEART_RATE("Heart Rate Metrics", "Heart rate telemetry and cardiovascular effort during training."),
    SLEEP("Sleep & Recovery", "Duration and rest quality to optimize training readiness.")
}

@Entity(tableName = "health_daily_summary")
data class HealthSummaryEntity(
    @PrimaryKey val dateStr: String,
    val steps: Int = 0,
    val distanceKm: Float = 0f,
    val activeCalories: Int = 0,
    val workoutCount: Int = 0,
    val avgHeartRate: Int? = null,
    val sleepHours: Float? = null,
    val isConnected: Boolean = false,
    val lastSyncedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "health_activities")
data class HealthActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateStr: String,
    val activityType: String, // Walking, Running, Cycling, Workout, High Intensity
    val startTime: Long,
    val durationMinutes: Int,
    val distanceKm: Float,
    val calories: Int,
    val avgHeartRate: Int? = null
)

@Entity(tableName = "health_privacy_settings")
data class HealthPrivacySettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isConnected: Boolean = false,
    val connectionStatus: String = "DISCONNECTED",
    val allowAiAccessToFitnessData: Boolean = true,
    val authorizedPermissionsJson: String = """["STEPS","DISTANCE","CALORIES","WORKOUTS"]""",
    val lastSyncedTimestamp: Long = 0L
)
