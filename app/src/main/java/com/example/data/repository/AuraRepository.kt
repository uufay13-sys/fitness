package com.example.data.repository

import com.example.data.api.AuraAiService
import com.example.data.api.AuraAiStatus
import com.example.data.api.AuraResponse
import com.example.data.local.AuraDatabase
import com.example.data.model.AuraMemory
import com.example.data.model.ChatMessage
import com.example.data.model.DailyCheckIn
import com.example.data.model.Exercise
import com.example.data.model.NutritionLog
import com.example.data.model.UserProfile
import com.example.data.model.WaterLog
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuraRepository(
    private val database: AuraDatabase,
    private val aiService: AuraAiService = AuraAiService()
) {
    // User
    val userProfile: Flow<UserProfile?> = database.userDao().getUserProfile()

    suspend fun saveUserProfile(profile: UserProfile) {
        database.userDao().saveUserProfile(profile)
    }

    // Memories
    val memories: Flow<List<AuraMemory>> = database.auraMemoryDao().getAllMemories()

    suspend fun addMemory(category: String, key: String, value: String): Long {
        return database.auraMemoryDao().insertMemory(
            AuraMemory(category = category, key = key, value = value)
        )
    }

    suspend fun deleteMemory(memory: AuraMemory) {
        database.auraMemoryDao().deleteMemory(memory)
    }

    suspend fun clearMemories() {
        database.auraMemoryDao().clearAllMemories()
    }

    // Exercises
    val exercises: Flow<List<Exercise>> = database.exerciseDao().getAllExercises()

    suspend fun getExerciseById(id: String): Exercise? {
        return database.exerciseDao().getExerciseById(id)
    }

    fun getExercisesByMuscle(muscle: String): Flow<List<Exercise>> {
        return database.exerciseDao().getExercisesByMuscle(muscle)
    }

    // Workouts
    val workoutPlans: Flow<List<WorkoutPlan>> = database.workoutDao().getAllWorkoutPlans()
    val workoutLogs: Flow<List<WorkoutLog>> = database.workoutDao().getAllWorkoutLogs()

    suspend fun getWorkoutPlanById(id: Long): WorkoutPlan? {
        return database.workoutDao().getWorkoutPlanById(id)
    }

    suspend fun saveWorkoutPlan(plan: WorkoutPlan): Long {
        return database.workoutDao().insertWorkoutPlan(plan)
    }

    suspend fun deleteWorkoutPlan(id: Long) {
        database.workoutDao().deleteWorkoutPlanById(id)
    }

    suspend fun saveWorkoutLog(log: WorkoutLog): Long {
        // Also record a milestone memory if significant
        val id = database.workoutDao().insertWorkoutLog(log)
        database.auraMemoryDao().insertMemory(
            AuraMemory(
                category = "Achievement",
                key = "Workout Completed",
                value = "Completed ${log.workoutTitle} (${log.durationSeconds / 60}m, ${log.totalSets} sets, ${log.volumeKg.toInt()}kg volume)"
            )
        )
        return id
    }

    // Nutrition
    fun getMealsForDate(dateStr: String): Flow<List<NutritionLog>> {
        return database.nutritionDao().getMealsForDate(dateStr)
    }

    suspend fun addMeal(meal: NutritionLog): Long {
        return database.nutritionDao().insertMeal(meal)
    }

    suspend fun deleteMeal(id: Long) {
        database.nutritionDao().deleteMealById(id)
    }

    fun getWaterLog(dateStr: String): Flow<WaterLog?> {
        return database.nutritionDao().getWaterLog(dateStr)
    }

    suspend fun updateWaterGlasses(dateStr: String, delta: Int) {
        val current = database.nutritionDao().getWaterLog(dateStr).firstOrNull()
        val currentGlasses = current?.glasses ?: 0
        val newGlasses = (currentGlasses + delta).coerceIn(0, 20)
        database.nutritionDao().setWaterLog(
            WaterLog(dateStr = dateStr, glasses = newGlasses, goalGlasses = current?.goalGlasses ?: 8)
        )
    }

    // Daily Check-In
    fun getTodayCheckIn(dateStr: String): Flow<DailyCheckIn?> {
        return database.dailyCheckInDao().getCheckInForDate(dateStr)
    }

    val recentCheckIns: Flow<List<DailyCheckIn>> = database.dailyCheckInDao().getRecentCheckIns()

    suspend fun saveDailyCheckIn(checkIn: DailyCheckIn): Long {
        val id = database.dailyCheckInDao().insertCheckIn(checkIn)
        // Store memory about readiness
        database.auraMemoryDao().insertMemory(
            AuraMemory(
                category = "Daily Readiness",
                key = "Check-In ${checkIn.dateStr}",
                value = "Energy ${checkIn.energy}/5, Sleep ${checkIn.sleepHours}h, Recovery: ${checkIn.recoveryFeeling}"
            )
        )
        return id
    }

    // Chat
    val chatMessages: Flow<List<ChatMessage>> = database.chatDao().getAllMessages()

    suspend fun sendUserMessage(text: String): ChatMessage {
        val userMsg = ChatMessage(sender = "user", message = text)
        database.chatDao().insertMessage(userMsg)
        return userMsg
    }

    suspend fun sendAuraReply(response: AuraResponse): ChatMessage {
        val auraMsg = ChatMessage(
            sender = "aura",
            message = response.replyText,
            actionType = response.actionType,
            actionPayload = response.actionPayload
        )
        database.chatDao().insertMessage(auraMsg)
        return auraMsg
    }

    suspend fun clearChat() {
        database.chatDao().clearChat()
    }

    suspend fun queryAura(userMessage: String): AuraResponse {
        val profile = database.userDao().getUserProfileOnce()
        val memoriesList = database.auraMemoryDao().getAllMemoriesList()
        val recentLogs = database.workoutDao().getRecentLogsOnce()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val todayCheckIn = database.dailyCheckInDao().getCheckInForDate(todayStr).firstOrNull()
        val healthSummary = database.healthDao().getSummaryForDateOnce(todayStr)
        val healthPrivacy = database.healthDao().getPrivacySettingsOnce()
        val allowAiAccess = healthPrivacy?.allowAiAccessToFitnessData ?: true

        return aiService.generateAuraResponse(
            userMessage = userMessage,
            userProfile = profile,
            recentMemories = memoriesList,
            recentLogs = recentLogs,
            todayCheckIn = todayCheckIn,
            healthSummary = healthSummary,
            allowAiAccessToHealth = allowAiAccess
        )
    }
}
