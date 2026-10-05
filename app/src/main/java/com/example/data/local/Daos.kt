package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfileOnce(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)
}

@Dao
interface AuraMemoryDao {
    @Query("SELECT * FROM aura_memory ORDER BY updatedAt DESC")
    fun getAllMemories(): Flow<List<AuraMemory>>

    @Query("SELECT * FROM aura_memory ORDER BY updatedAt DESC")
    suspend fun getAllMemoriesList(): List<AuraMemory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AuraMemory): Long

    @Delete
    suspend fun deleteMemory(memory: AuraMemory)

    @Query("DELETE FROM aura_memory WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM aura_memory")
    suspend fun clearAllMemories()
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): Exercise?

    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup ORDER BY name ASC")
    fun getExercisesByMuscle(muscleGroup: String): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getCount(): Int
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_plans ORDER BY createdAt DESC")
    fun getAllWorkoutPlans(): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM workout_plans WHERE id = :id")
    suspend fun getWorkoutPlanById(id: Long): WorkoutPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutPlan(plan: WorkoutPlan): Long

    @Query("DELETE FROM workout_plans WHERE id = :id")
    suspend fun deleteWorkoutPlanById(id: Long)

    @Query("SELECT * FROM workout_logs ORDER BY completedAt DESC")
    fun getAllWorkoutLogs(): Flow<List<WorkoutLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(log: WorkoutLog): Long

    @Query("SELECT * FROM workout_logs ORDER BY completedAt DESC LIMIT 10")
    suspend fun getRecentLogsOnce(): List<WorkoutLog>
}

@Dao
interface NutritionDao {
    @Query("SELECT * FROM nutrition_logs WHERE dateStr = :dateStr ORDER BY timestamp DESC")
    fun getMealsForDate(dateStr: String): Flow<List<NutritionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: NutritionLog): Long

    @Query("DELETE FROM nutrition_logs WHERE id = :id")
    suspend fun deleteMealById(id: Long)

    @Query("SELECT * FROM water_logs WHERE dateStr = :dateStr")
    fun getWaterLog(dateStr: String): Flow<WaterLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setWaterLog(waterLog: WaterLog)
}

@Dao
interface DailyCheckInDao {
    @Query("SELECT * FROM daily_checkins WHERE dateStr = :dateStr LIMIT 1")
    fun getCheckInForDate(dateStr: String): Flow<DailyCheckIn?>

    @Query("SELECT * FROM daily_checkins ORDER BY timestamp DESC LIMIT 7")
    fun getRecentCheckIns(): Flow<List<DailyCheckIn>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: DailyCheckIn): Long
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()
}
