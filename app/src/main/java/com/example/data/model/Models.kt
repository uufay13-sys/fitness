package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Athlete",
    val age: Int = 26,
    val heightCm: Float = 175f,
    val weightKg: Float = 72f,
    val fitnessGoal: String = "Build Muscle",
    val experienceLevel: String = "Intermediate",
    val workoutDurationMinutes: Int = 45,
    val equipment: String = "Full Gym",
    val preferredDaysPerWeek: Int = 4,
    val targetMuscles: String = "Full Body",
    val dietaryPreference: String = "High Protein",
    val isDarkTheme: Boolean = true,
    val isOnboarded: Boolean = false,
    val voiceEnabled: Boolean = true,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "aura_memory")
data class AuraMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "Goal", "Injury/Limitation", "Preference", "Habit", "Achievement"
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String, // Chest, Back, Legs, Shoulders, Arms, Core
    val secondaryMuscles: String,
    val difficulty: String, // Beginner, Intermediate, Advanced
    val equipment: String, // Barbell, Dumbbell, Bodyweight, Cable, Machine
    val instructions: String,
    val commonMistakes: String,
    val safetyNotes: String,
    val defaultSets: Int = 3,
    val defaultReps: String = "10-12",
    val restSeconds: Int = 60
)

@Entity(tableName = "workout_plans")
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetMuscle: String,
    val durationMinutes: Int,
    val difficulty: String,
    val equipment: String,
    val warmupText: String,
    val exercisesJson: String, // List of PlanExercise
    val cooldownText: String,
    val safetyNotes: String,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class PlanExercise(
    val exerciseId: String,
    val name: String,
    val targetMuscles: String,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val instructions: String,
    val notes: String = ""
)

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long? = null,
    val workoutTitle: String,
    val startedAt: Long,
    val completedAt: Long,
    val durationSeconds: Long,
    val totalSets: Int,
    val totalReps: Int,
    val volumeKg: Float,
    val exercisesCompletedCount: Int,
    val userNotes: String = ""
)

@Entity(tableName = "nutrition_logs")
data class NutritionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateStr: String, // YYYY-MM-DD
    val mealType: String, // Breakfast, Lunch, Dinner, Snack
    val foodName: String,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey val dateStr: String,
    val glasses: Int = 0,
    val goalGlasses: Int = 8
)

@Entity(tableName = "daily_checkins")
data class DailyCheckIn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateStr: String,
    val energy: Int, // 1 to 5
    val sleepHours: Float,
    val recoveryFeeling: String, // Fresh, Moderate, Sore, Exhausted
    val mood: String, // Energized, Good, Neutral, Tired
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "user", "aura"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null, // "START_WORKOUT", "VIEW_PROGRESS", "VIEW_PLAN", "LOG_MEAL", "EXERCISE_CARD"
    val actionPayload: String? = null
)

// Active Workout In-Progress state
data class ActiveExerciseState(
    val exercise: Exercise,
    val targetSets: Int,
    val targetReps: String,
    val restSeconds: Int,
    var completedSets: Int = 0,
    var currentWeightKg: Float = 20f,
    var currentReps: Int = 10,
    var isCurrentCompleted: Boolean = false
)
