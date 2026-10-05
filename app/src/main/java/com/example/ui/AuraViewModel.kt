package com.example.ui

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AuraAiStatus
import com.example.data.api.AuraResponse
import com.example.data.health.HealthService
import com.example.data.local.AuraDatabase
import com.example.data.model.ActiveExerciseState
import com.example.data.model.AdCampaign
import com.example.data.model.AdPackage
import com.example.data.model.AdPaymentRecord
import com.example.data.model.AdQuotation
import com.example.data.model.AiAdActionLog
import com.example.data.model.AuraAdsStatus
import com.example.data.model.AuraMemory
import com.example.data.model.ChatMessage
import com.example.data.model.ClientCommunicationMessage
import com.example.data.model.ClientProspect
import com.example.data.model.DailyCheckIn
import com.example.data.model.Exercise
import com.example.data.model.HealthActivityEntity
import com.example.data.model.HealthConnectionStatus
import com.example.data.model.HealthPermissionType
import com.example.data.model.HealthPrivacySettingsEntity
import com.example.data.model.HealthSummaryEntity
import com.example.data.model.NutritionLog
import com.example.data.model.OwnerApprovalRequest
import com.example.data.model.PlanExercise
import com.example.data.model.PricingRuleEntity
import com.example.data.model.UserProfile
import com.example.data.model.WaterLog
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutPlan
import com.example.data.repository.AdsRepository
import com.example.data.repository.AuraRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class AuraUiEvent {
    data class ShowToast(val message: String) : AuraUiEvent()
    data class SpeakText(val text: String) : AuraUiEvent()
    data class NavigateTo(val route: String) : AuraUiEvent()
}

data class ActiveWorkoutSession(
    val planId: Long? = null,
    val title: String,
    val exercises: List<ActiveExerciseState>,
    val currentExerciseIndex: Int = 0,
    val elapsedSeconds: Long = 0,
    val currentSetDurationSeconds: Int = 0,
    val isResting: Boolean = false,
    val isTimerPaused: Boolean = false,
    val restSecondsRemaining: Int = 0,
    val restTotalSeconds: Int = 60,
    val auraLiveCoachMessage: String = "Focus on clean execution and progressive overload.",
    val isFinished: Boolean = false,
    val promptIsWorkoutDone: Boolean = false,
    val totalVolumeKg: Float = 0f,
    val completedSetsCount: Int = 0
)

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    // Today Date
    val todayDateStr: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val database = AuraDatabase.getDatabase(application, viewModelScope)
    val repository = AuraRepository(database)
    val healthService = HealthService(application, database.healthDao(), viewModelScope)

    // Health Flows
    val healthConnectionStatus: StateFlow<HealthConnectionStatus> = healthService.connectionStatus
    val todayHealthSummary: StateFlow<HealthSummaryEntity?> = healthService.getTodaySummary(todayDateStr)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val todayHealthActivities: StateFlow<List<HealthActivityEntity>> = healthService.getTodayActivities(todayDateStr)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentHealthSummaries: StateFlow<List<HealthSummaryEntity>> = healthService.getRecentSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val healthPrivacySettings: StateFlow<HealthPrivacySettingsEntity?> = healthService.privacySettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val adsRepository = AdsRepository(database.adsDao())

    // AURA ADS AGENT Status & Flows
    private val _auraAdsStatus = MutableStateFlow(AuraAdsStatus.READY)
    val auraAdsStatus: StateFlow<AuraAdsStatus> = _auraAdsStatus.asStateFlow()

    val clientProspects: StateFlow<List<ClientProspect>> = adsRepository.prospects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adPackages: StateFlow<List<AdPackage>> = adsRepository.packages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pricingRules: StateFlow<PricingRuleEntity?> = adsRepository.pricingRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val adQuotations: StateFlow<List<AdQuotation>> = adsRepository.quotations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val approvalRequests: StateFlow<List<OwnerApprovalRequest>> = adsRepository.approvalRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingApprovals: StateFlow<List<OwnerApprovalRequest>> = adsRepository.pendingApprovals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adCampaigns: StateFlow<List<AdCampaign>> = adsRepository.campaigns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adPayments: StateFlow<List<AdPaymentRecord>> = adsRepository.payments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adActionLogs: StateFlow<List<AiAdActionLog>> = adsRepository.actionLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentCommunications: StateFlow<List<ClientCommunicationMessage>> = adsRepository.recentCommunications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Events
    private val _uiEvents = MutableSharedFlow<AuraUiEvent>()
    val uiEvents: SharedFlow<AuraUiEvent> = _uiEvents.asSharedFlow()

    // Status
    private val _auraStatus = MutableStateFlow(AuraAiStatus.READY)
    val auraStatus: StateFlow<AuraAiStatus> = _auraStatus.asStateFlow()

    // Database Flows
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val memories: StateFlow<List<AuraMemory>> = repository.memories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exercises: StateFlow<List<Exercise>> = repository.exercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workoutPlans: StateFlow<List<WorkoutPlan>> = repository.workoutPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workoutLogs: StateFlow<List<WorkoutLog>> = repository.workoutLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayMeals: StateFlow<List<NutritionLog>> = repository.getMealsForDate(todayDateStr)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayWater: StateFlow<WaterLog?> = repository.getWaterLog(todayDateStr)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayCheckIn: StateFlow<DailyCheckIn?> = repository.getTodayCheckIn(todayDateStr)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active Workout state
    private val _activeWorkout = MutableStateFlow<ActiveWorkoutSession?>(null)
    val activeWorkout: StateFlow<ActiveWorkoutSession?> = _activeWorkout.asStateFlow()

    private var workoutTimerJob: Job? = null
    private var restTimer: CountDownTimer? = null

    // Theme state
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    init {
        viewModelScope.launch {
            userProfile.collect { profile ->
                if (profile != null) {
                    _isDarkTheme.value = profile.isDarkTheme
                }
            }
        }
    }

    fun toggleTheme() {
        val newTheme = !_isDarkTheme.value
        _isDarkTheme.value = newTheme
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            repository.saveUserProfile(current.copy(isDarkTheme = newTheme))
        }
    }

    fun setAuraStatus(status: AuraAiStatus) {
        _auraStatus.value = status
    }

    fun sendUserMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendUserMessage(text)
            _auraStatus.value = AuraAiStatus.THINKING
            delay(400) // smooth pacing

            try {
                val response = repository.queryAura(text)
                _auraStatus.value = AuraAiStatus.RESPONDING
                repository.sendAuraReply(response)

                _uiEvents.emit(AuraUiEvent.SpeakText(response.replyText))

                // Handle action if triggered
                handleAgentAction(response)
            } catch (e: Exception) {
                _auraStatus.value = AuraAiStatus.OFFLINE
                repository.sendAuraReply(
                    AuraResponse(
                        replyText = "I encountered a connection interruption. However, I'm ready with your stored routines and offline logs. Remember: LEAVE BETTER."
                    )
                )
            } finally {
                delay(800)
                _auraStatus.value = AuraAiStatus.READY
            }
        }
    }

    private suspend fun handleAgentAction(response: AuraResponse) {
        when (response.actionType) {
            "START_WORKOUT" -> {
                // Find or start first available plan
                val plans = workoutPlans.value
                val plan = plans.firstOrNull()
                if (plan != null) {
                    startWorkout(plan)
                    _uiEvents.emit(AuraUiEvent.NavigateTo("active_workout"))
                }
            }
            "PLAN_WORKOUT" -> {
                _uiEvents.emit(AuraUiEvent.NavigateTo("workouts"))
            }
            "VIEW_PROGRESS" -> {
                _uiEvents.emit(AuraUiEvent.NavigateTo("progress"))
            }
            "LOG_MEAL" -> {
                // Parse meal payload if available
                response.actionPayload?.let { jsonStr ->
                    try {
                        val obj = JSONObject(jsonStr)
                        val food = obj.optString("food", "Healthy Meal")
                        val cal = obj.optInt("calories", 350)
                        val p = obj.optDouble("p", 25.0).toFloat()
                        val c = obj.optDouble("c", 30.0).toFloat()
                        val f = obj.optDouble("f", 10.0).toFloat()
                        repository.addMeal(
                            NutritionLog(
                                dateStr = todayDateStr,
                                mealType = "Snack",
                                foodName = food,
                                calories = cal,
                                proteinG = p,
                                carbsG = c,
                                fatG = f
                            )
                        )
                        _uiEvents.emit(AuraUiEvent.ShowToast("Logged meal: $food"))
                    } catch (e: Exception) {
                        // ignore malformed
                    }
                }
            }
        }
    }

    fun executeQuickAction(actionTitle: String) {
        when (actionTitle) {
            "START TODAY'S WORKOUT" -> {
                val plans = workoutPlans.value
                if (plans.isNotEmpty()) {
                    startWorkout(plans.first())
                    viewModelScope.launch {
                        _uiEvents.emit(AuraUiEvent.NavigateTo("active_workout"))
                    }
                } else {
                    sendUserMessage("Start today's workout")
                }
            }
            "CREATE MY WORKOUT" -> sendUserMessage("Create an optimal workout for me based on my profile")
            "PLAN MY WEEK" -> sendUserMessage("Plan my workout week for maximum progress")
            "ANALYZE MY PROGRESS" -> analyzeProgressWithAura()
            "EXPLAIN AN EXERCISE" -> sendUserMessage("Explain proper bench press and squat form cues")
            "CHANGE MY WORKOUT" -> sendUserMessage("My muscles are fatigued, adapt today's workout")
            "WHAT SHOULD I EAT?" -> sendUserMessage("What should I eat today for my fitness goal?")
            "CHECK MY HABITS" -> sendUserMessage("Audit my current fitness and recovery habits")
            "HOW ACTIVE WAS I TODAY?" -> sendUserMessage("How active was I today?")
            "SHOULD I WORK OUT TODAY?" -> sendUserMessage("Should I work out today?")
            "HOW MANY CALORIES BURNED?" -> sendUserMessage("How many calories did I burn?")
            "MOTIVATE ME" -> sendUserMessage("Motivate me right now with our LEAVE BETTER philosophy")
        }
    }

    fun analyzeProgressWithAura() {
        viewModelScope.launch {
            val logs = workoutLogs.value
            val checkins = database.dailyCheckInDao().getRecentCheckIns().firstOrNull() ?: emptyList()
            val totalLogs = logs.size
            val volume = logs.sumOf { it.volumeKg.toDouble() }.toInt()

            val prompt = "Analyze my fitness progress. Stored logs: $totalLogs completed sessions, total volume: ${volume}kg. Recent check-in count: ${checkins.size}. Give me an executive AURA performance assessment, consistency grade, and one high-impact directive."
            sendUserMessage(prompt)
        }
    }

    // Workout generator
    fun generateWorkout(
        title: String,
        goal: String,
        muscle: String,
        durationMinutes: Int,
        equipment: String,
        difficulty: String
    ) {
        viewModelScope.launch {
            _auraStatus.value = AuraAiStatus.WORKING
            val allEx = exercises.value
            val filteredEx = allEx.filter {
                it.muscleGroup.contains(muscle, ignoreCase = true) ||
                        muscle.contains(it.muscleGroup, ignoreCase = true) ||
                        muscle.contains("Full Body", ignoreCase = true)
            }.ifEmpty { allEx.take(4) }

            val planExercises = filteredEx.take(4).map { ex ->
                PlanExercise(
                    exerciseId = ex.id,
                    name = ex.name,
                    targetMuscles = ex.muscleGroup,
                    sets = ex.defaultSets,
                    reps = ex.defaultReps,
                    restSeconds = ex.restSeconds,
                    instructions = ex.instructions
                )
            }

            val jsonArray = JSONArray()
            planExercises.forEach { pe ->
                jsonArray.put(JSONObject().apply {
                    put("exerciseId", pe.exerciseId)
                    put("name", pe.name)
                    put("targetMuscles", pe.targetMuscles)
                    put("sets", pe.sets)
                    put("reps", pe.reps)
                    put("restSeconds", pe.restSeconds)
                    put("instructions", pe.instructions)
                })
            }

            val warmup = "5 mins dynamic $muscle activation, light arm/hip mobility, elevated heart rate warmup."
            val cooldown = "5 mins static stretching for $muscle and deep diaphragm reset."

            val newPlan = WorkoutPlan(
                title = title.ifBlank { "AURA $muscle Protocol" },
                targetMuscle = muscle,
                durationMinutes = durationMinutes,
                difficulty = difficulty,
                equipment = equipment,
                warmupText = warmup,
                exercisesJson = jsonArray.toString(),
                cooldownText = cooldown,
                safetyNotes = "Maintain strict core bracing and control the negative. LEAVE BETTER.",
                isCustom = true
            )

            repository.saveWorkoutPlan(newPlan)
            _auraStatus.value = AuraAiStatus.READY
            _uiEvents.emit(AuraUiEvent.ShowToast("Created workout: ${newPlan.title}"))
        }
    }

    // Active Workout Flow
    fun startWorkout(plan: WorkoutPlan) {
        val exercisesList = mutableListOf<ActiveExerciseState>()

        try {
            val jsonArray = JSONArray(plan.exercisesJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val exId = obj.optString("exerciseId", "")
                val exName = obj.optString("name", "Exercise")
                val targetMuscles = obj.optString("targetMuscles", plan.targetMuscle)
                val sets = obj.optInt("sets", 3)
                val reps = obj.optString("reps", "10")
                val rest = obj.optInt("restSeconds", 60)
                val instr = obj.optString("instructions", "Perform with controlled tempo.")

                val fullEx = exercises.value.find { it.id == exId } ?: Exercise(
                    id = exId.ifBlank { "ex_$i" },
                    name = exName,
                    muscleGroup = targetMuscles,
                    secondaryMuscles = "",
                    difficulty = plan.difficulty,
                    equipment = plan.equipment,
                    instructions = instr,
                    commonMistakes = "Rushing repetitions, compromising posture.",
                    safetyNotes = "Keep core active.",
                    defaultSets = sets,
                    defaultReps = reps,
                    restSeconds = rest
                )

                exercisesList.add(
                    ActiveExerciseState(
                        exercise = fullEx,
                        targetSets = sets,
                        targetReps = reps,
                        restSeconds = rest,
                        currentWeightKg = 25f,
                        currentReps = 10
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
            exercises.value.take(3).forEach { ex ->
                exercisesList.add(
                    ActiveExerciseState(
                        exercise = ex,
                        targetSets = ex.defaultSets,
                        targetReps = ex.defaultReps,
                        restSeconds = ex.restSeconds
                    )
                )
            }
        }

        _activeWorkout.value = ActiveWorkoutSession(
            planId = plan.id,
            title = plan.title,
            exercises = exercisesList,
            currentExerciseIndex = 0,
            elapsedSeconds = 0,
            auraLiveCoachMessage = "Welcome to ${plan.title}. Execute every repetition with purpose. LEAVE BETTER."
        )

        // Start elapsed timer
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(1000)
                _activeWorkout.value?.let { current ->
                    if (!current.isFinished && !current.isTimerPaused && !current.promptIsWorkoutDone) {
                        val newElapsed = current.elapsedSeconds + 1
                        if (current.isResting) {
                            val newRest = current.restSecondsRemaining - 1
                            if (newRest <= 0) {
                                // Rest completed
                                _activeWorkout.value = current.copy(
                                    elapsedSeconds = newElapsed,
                                    isResting = false,
                                    restSecondsRemaining = 0,
                                    currentSetDurationSeconds = 0,
                                    auraLiveCoachMessage = "Rest finished! Step back up for your next set."
                                )
                                viewModelScope.launch {
                                    _uiEvents.emit(AuraUiEvent.SpeakText("Rest complete. Next set ready."))
                                }
                            } else {
                                _activeWorkout.value = current.copy(
                                    elapsedSeconds = newElapsed,
                                    restSecondsRemaining = newRest
                                )
                            }
                        } else {
                            // Active exercise duration (Time Under Tension)
                            _activeWorkout.value = current.copy(
                                elapsedSeconds = newElapsed,
                                currentSetDurationSeconds = current.currentSetDurationSeconds + 1
                            )
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            _uiEvents.emit(AuraUiEvent.SpeakText("Starting workout: ${plan.title}. Let's make every rep count."))
        }
    }

    fun completeSet(weightKg: Float, reps: Int) {
        val current = _activeWorkout.value ?: return
        val currentExIndex = current.currentExerciseIndex
        val exerciseState = current.exercises.getOrNull(currentExIndex) ?: return

        val newCompletedSets = exerciseState.completedSets + 1
        exerciseState.completedSets = newCompletedSets
        exerciseState.currentWeightKg = weightKg
        exerciseState.currentReps = reps

        val setVolume = weightKg * reps
        val newTotalVolume = current.totalVolumeKg + setVolume
        val newTotalSets = current.completedSetsCount + 1

        val isExerciseDone = newCompletedSets >= exerciseState.targetSets
        val allExercisesDone = current.exercises.all {
            if (it == exerciseState) newCompletedSets >= it.targetSets
            else it.completedSets >= it.targetSets
        }

        if (allExercisesDone) {
            // AURA Agent proactively asks user if workout is done or not
            _activeWorkout.value = current.copy(
                isResting = false,
                currentSetDurationSeconds = 0,
                promptIsWorkoutDone = true,
                auraLiveCoachMessage = "All planned exercises complete! Are you done with today's workout, or do you want to keep going?",
                totalVolumeKg = newTotalVolume,
                completedSetsCount = newTotalSets
            )
            viewModelScope.launch {
                _uiEvents.emit(AuraUiEvent.SpeakText("All scheduled exercises are complete! Are you done with today's workout, or would you like to add another set?"))
            }
            return
        }

        val coachTip = if (isExerciseDone) {
            "${exerciseState.exercise.name} completed! Superior effort. Prepare for the next movement."
        } else {
            "Set $newCompletedSets of ${exerciseState.targetSets} locked in at ${weightKg}kg. Recover fully."
        }

        val restDuration = exerciseState.restSeconds

        _activeWorkout.value = current.copy(
            isResting = true,
            currentSetDurationSeconds = 0,
            restSecondsRemaining = restDuration,
            restTotalSeconds = restDuration,
            auraLiveCoachMessage = coachTip,
            totalVolumeKg = newTotalVolume,
            completedSetsCount = newTotalSets
        )

        viewModelScope.launch {
            _uiEvents.emit(AuraUiEvent.SpeakText(coachTip))
        }
    }

    fun toggleTimerPause() {
        _activeWorkout.value?.let { current ->
            _activeWorkout.value = current.copy(isTimerPaused = !current.isTimerPaused)
        }
    }

    fun adjustRestSeconds(delta: Int) {
        _activeWorkout.value?.let { current ->
            if (current.isResting) {
                val newRest = (current.restSecondsRemaining + delta).coerceIn(5, 600)
                val newTotal = (current.restTotalSeconds + delta).coerceAtLeast(newRest)
                _activeWorkout.value = current.copy(
                    restSecondsRemaining = newRest,
                    restTotalSeconds = newTotal
                )
            }
        }
    }

    fun requestEndWorkoutPrompt() {
        _activeWorkout.value?.let { current ->
            _activeWorkout.value = current.copy(promptIsWorkoutDone = true)
            viewModelScope.launch {
                _uiEvents.emit(AuraUiEvent.SpeakText("Are you done with your workout session now, or would you like to keep training?"))
            }
        }
    }

    fun userRespondedWorkoutDone(isDone: Boolean) {
        val current = _activeWorkout.value ?: return
        if (isDone) {
            finishWorkout("Completed scheduled session.")
        } else {
            // User wants to keep training! Add extra set to current movement
            val currentEx = current.exercises.getOrNull(current.currentExerciseIndex)
            val updatedList = current.exercises.toMutableList()
            if (currentEx != null) {
                updatedList[current.currentExerciseIndex] = currentEx.copy(
                    targetSets = currentEx.targetSets + 1
                )
            }
            _activeWorkout.value = current.copy(
                exercises = updatedList,
                isResting = false,
                currentSetDurationSeconds = 0,
                promptIsWorkoutDone = false,
                auraLiveCoachMessage = "Extra set loaded! Lock in your form and execute with purpose."
            )
            viewModelScope.launch {
                _uiEvents.emit(AuraUiEvent.SpeakText("Extra set loaded! Let's push for more. LEAVE BETTER."))
            }
        }
    }

    fun skipRest() {
        _activeWorkout.value?.let { current ->
            _activeWorkout.value = current.copy(
                isResting = false,
                restSecondsRemaining = 0,
                currentSetDurationSeconds = 0
            )
        }
    }

    fun advanceExercise() {
        val current = _activeWorkout.value ?: return
        val nextIndex = current.currentExerciseIndex + 1
        if (nextIndex < current.exercises.size) {
            val nextEx = current.exercises[nextIndex].exercise
            val message = "Next up: ${nextEx.name}. Focus on ${nextEx.muscleGroup} engagement."
            _activeWorkout.value = current.copy(
                currentExerciseIndex = nextIndex,
                isResting = false,
                auraLiveCoachMessage = message
            )
            viewModelScope.launch {
                _uiEvents.emit(AuraUiEvent.SpeakText(message))
            }
        } else {
            finishWorkout("Completed scheduled session.")
        }
    }

    fun addSetToCurrent() {
        val current = _activeWorkout.value ?: return
        val currentEx = current.exercises.getOrNull(current.currentExerciseIndex) ?: return
        val updatedExercises = current.exercises.toMutableList()
        updatedExercises[current.currentExerciseIndex] = currentEx.copy(
            targetSets = currentEx.targetSets + 1
        )
        _activeWorkout.value = current.copy(exercises = updatedExercises)
    }

    fun finishWorkout(userNotes: String = "") {
        val current = _activeWorkout.value ?: return
        workoutTimerJob?.cancel()
        restTimer?.cancel()

        viewModelScope.launch {
            val log = WorkoutLog(
                planId = current.planId,
                workoutTitle = current.title,
                startedAt = System.currentTimeMillis() - (current.elapsedSeconds * 1000),
                completedAt = System.currentTimeMillis(),
                durationSeconds = current.elapsedSeconds,
                totalSets = current.completedSetsCount,
                totalReps = current.exercises.sumOf { it.completedSets * it.currentReps },
                volumeKg = current.totalVolumeKg,
                exercisesCompletedCount = current.exercises.count { it.completedSets > 0 },
                userNotes = userNotes
            )

            repository.saveWorkoutLog(log)
            healthService.syncWorkoutToHealth(log, todayDateStr)

            _activeWorkout.value = current.copy(
                isFinished = true,
                auraLiveCoachMessage = "Outstanding performance. Session logged. You leave better today."
            )

            _uiEvents.emit(AuraUiEvent.SpeakText("Workout complete. You lifted a total volume of ${current.totalVolumeKg.toInt()} kilograms. LEAVE BETTER."))
            _uiEvents.emit(AuraUiEvent.ShowToast("Workout recorded successfully!"))
        }
    }

    fun exitWorkout() {
        workoutTimerJob?.cancel()
        restTimer?.cancel()
        _activeWorkout.value = null
    }

    // Nutrition
    fun addMeal(type: String, name: String, cal: Int, p: Float, c: Float, f: Float) {
        viewModelScope.launch {
            repository.addMeal(
                NutritionLog(
                    dateStr = todayDateStr,
                    mealType = type,
                    foodName = name,
                    calories = cal,
                    proteinG = p,
                    carbsG = c,
                    fatG = f
                )
            )
            _uiEvents.emit(AuraUiEvent.ShowToast("Added $name to $type"))
        }
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch {
            repository.deleteMeal(id)
        }
    }

    fun updateWater(delta: Int) {
        viewModelScope.launch {
            repository.updateWaterGlasses(todayDateStr, delta)
        }
    }

    // Check-in
    fun submitDailyCheckIn(energy: Int, sleepHours: Float, recovery: String, mood: String, notes: String) {
        viewModelScope.launch {
            val checkIn = DailyCheckIn(
                dateStr = todayDateStr,
                energy = energy,
                sleepHours = sleepHours,
                recoveryFeeling = recovery,
                mood = mood,
                notes = notes
            )
            repository.saveDailyCheckIn(checkIn)
            _uiEvents.emit(AuraUiEvent.ShowToast("Daily Check-In Recorded!"))

            // AURA feedback
            sendUserMessage("I just checked in: Energy $energy/5, Sleep ${sleepHours}h, Recovery: $recovery. Give me today's coaching plan.")
        }
    }

    // Memories
    fun addMemory(category: String, key: String, value: String) {
        viewModelScope.launch {
            repository.addMemory(category, key, value)
            _uiEvents.emit(AuraUiEvent.ShowToast("Memory saved"))
        }
    }

    fun deleteMemory(memory: AuraMemory) {
        viewModelScope.launch {
            repository.deleteMemory(memory)
            _uiEvents.emit(AuraUiEvent.ShowToast("Memory removed"))
        }
    }

    fun resetMemories() {
        viewModelScope.launch {
            repository.clearMemories()
            _uiEvents.emit(AuraUiEvent.ShowToast("All AURA memories cleared"))
        }
    }

    // User Profile
    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
            _uiEvents.emit(AuraUiEvent.ShowToast("Profile updated"))
        }
    }

    // Health Ecosystem Integration
    fun connectHealth(authorizedPermissions: List<HealthPermissionType>) {
        viewModelScope.launch {
            healthService.connect(authorizedPermissions, todayDateStr)
            _uiEvents.emit(AuraUiEvent.ShowToast("Connected to Google Health ecosystem"))
            _uiEvents.emit(AuraUiEvent.SpeakText("Fitness data connected. AURA is now calibrated with your real activity data."))
        }
    }

    fun markHealthPermissionDenied() {
        healthService.markPermissionDenied()
        viewModelScope.launch {
            _uiEvents.emit(AuraUiEvent.ShowToast("Fitness permission denied"))
        }
    }

    fun disconnectHealth(clearLocalData: Boolean) {
        viewModelScope.launch {
            healthService.disconnect(clearLocalData)
            _uiEvents.emit(AuraUiEvent.ShowToast(if (clearLocalData) "Disconnected & fitness data cleared" else "Disconnected from Health data"))
        }
    }

    fun setAiAccessToHealth(enabled: Boolean) {
        viewModelScope.launch {
            healthService.setAiAccessEnabled(enabled)
            _uiEvents.emit(AuraUiEvent.ShowToast(if (enabled) "AI access to fitness data resumed" else "AI access to fitness data paused"))
        }
    }

    fun deleteLocalHealthData() {
        viewModelScope.launch {
            healthService.deleteLocalFitnessData()
            _uiEvents.emit(AuraUiEvent.ShowToast("Local fitness data deleted"))
        }
    }

    fun refreshHealthData() {
        viewModelScope.launch {
            healthService.refreshDailySummary(todayDateStr)
            _uiEvents.emit(AuraUiEvent.ShowToast("Activity data refreshed"))
        }
    }

    fun recordHealthActivity(
        activityType: String,
        durationMinutes: Int,
        distanceKm: Float,
        calories: Int,
        avgHeartRate: Int? = null
    ) {
        viewModelScope.launch {
            healthService.recordActivity(
                dateStr = todayDateStr,
                activityType = activityType,
                durationMinutes = durationMinutes,
                distanceKm = distanceKm,
                calories = calories,
                avgHeartRate = avgHeartRate
            )
            _uiEvents.emit(AuraUiEvent.ShowToast("Logged $activityType activity ($durationMinutes min)"))
        }
    }

    // AURA ADS AGENT Methods
    fun setAuraAdsStatus(status: AuraAdsStatus) {
        _auraAdsStatus.value = status
    }

    fun addClientProspect(
        businessName: String,
        category: String,
        contactPerson: String,
        phone: String,
        email: String,
        city: String,
        audience: String,
        objective: String,
        platforms: String,
        budget: Double,
        durationDays: Int,
        hasCreatives: Boolean
    ) {
        viewModelScope.launch {
            _auraAdsStatus.value = AuraAdsStatus.ANALYZING
            val prospect = ClientProspect(
                businessName = businessName,
                category = category,
                contactPerson = contactPerson,
                contactPhone = phone,
                contactEmail = email,
                cityLocation = city,
                targetAudience = audience,
                adObjective = objective,
                preferredPlatforms = platforms,
                approxBudget = budget,
                expectedDurationDays = durationDays,
                hasCreatives = hasCreatives,
                status = "PROSPECT"
            )
            adsRepository.addProspect(prospect)
            delay(400)
            _auraAdsStatus.value = AuraAdsStatus.READY
            _uiEvents.emit(AuraUiEvent.ShowToast("Added prospect: $businessName"))
        }
    }

    fun updateProspectStatus(id: Long, status: String) {
        viewModelScope.launch {
            adsRepository.updateProspectStatus(id, status)
            _uiEvents.emit(AuraUiEvent.ShowToast("Prospect status: $status"))
        }
    }

    fun generateClientProposal(prospect: ClientProspect, onGenerated: (String) -> Unit) {
        viewModelScope.launch {
            _auraAdsStatus.value = AuraAdsStatus.APPROACHING_CLIENT
            val rules = adsRepository.getPricingRulesOnce()
            val proposal = adsRepository.aiService.generateClientProposal(prospect, rules)
            _auraAdsStatus.value = AuraAdsStatus.READY
            adsRepository.logAiAction("PROPOSAL_COMPOSED", "Drafted approach proposal for ${prospect.businessName}")
            onGenerated(proposal)
        }
    }

    fun calculateAndCreateQuotation(
        prospect: ClientProspect,
        adSpend: Double,
        durationDays: Int,
        requiresCreative: Boolean,
        discountPercent: Double
    ) {
        viewModelScope.launch {
            _auraAdsStatus.value = AuraAdsStatus.PREPARING_QUOTE
            val quote = adsRepository.calculateAndGenerateQuotation(
                prospect = prospect,
                adSpend = adSpend,
                durationDays = durationDays,
                requiresCreative = requiresCreative,
                customDiscountPercent = discountPercent
            )
            _auraAdsStatus.value = AuraAdsStatus.WAITING_FOR_OWNER_APPROVAL
            _uiEvents.emit(AuraUiEvent.ShowToast("Quotation ${quote.quotationCode} prepared. Approval requested from owner (8309596486)."))
            _uiEvents.emit(AuraUiEvent.SpeakText("Quotation generated. Approval notification dispatched to owner contact 8309596486."))
        }
    }

    fun approveQuotationByOwner(approvalId: Long, quotationId: Long) {
        viewModelScope.launch {
            adsRepository.approveOwnerQuotation(approvalId, quotationId)
            _auraAdsStatus.value = AuraAdsStatus.CAMPAIGN_READY
            _uiEvents.emit(AuraUiEvent.ShowToast("Quotation approved by owner! Campaign workspace created."))
            _uiEvents.emit(AuraUiEvent.SpeakText("Campaign proposal approved. Payment pending from client."))
        }
    }

    fun rejectQuotationByOwner(approvalId: Long, quotationId: Long, reason: String) {
        viewModelScope.launch {
            adsRepository.rejectOwnerQuotation(approvalId, quotationId, reason)
            _auraAdsStatus.value = AuraAdsStatus.READY
            _uiEvents.emit(AuraUiEvent.ShowToast("Quotation rejected: $reason"))
        }
    }

    fun verifyPayment(payment: AdPaymentRecord, ref: String) {
        viewModelScope.launch {
            adsRepository.verifyPayment(payment, ref)
            _auraAdsStatus.value = AuraAdsStatus.CAMPAIGN_READY
            _uiEvents.emit(AuraUiEvent.ShowToast("Payment verified! Campaign READY."))
            _uiEvents.emit(AuraUiEvent.SpeakText("Payment confirmed. Campaign marked ready for launch."))
        }
    }

    fun updateCampaignStatus(campaignId: Long, status: String) {
        viewModelScope.launch {
            adsRepository.updateCampaignStatus(campaignId, status)
            if (status == "RUNNING") _auraAdsStatus.value = AuraAdsStatus.RUNNING
            else if (status == "COMPLETED") _auraAdsStatus.value = AuraAdsStatus.COMPLETED
            _uiEvents.emit(AuraUiEvent.ShowToast("Campaign status: $status"))
        }
    }

    fun launchCampaign(campaignId: Long) {
        viewModelScope.launch {
            val result = adsRepository.launchCampaign(campaignId)
            result.onSuccess {
                _auraAdsStatus.value = AuraAdsStatus.RUNNING
                _uiEvents.emit(AuraUiEvent.ShowToast("Campaign launched! Media buy active."))
                _uiEvents.emit(AuraUiEvent.SpeakText("Campaign is now live and running."))
            }.onFailure {
                _uiEvents.emit(AuraUiEvent.ShowToast("Cannot launch: ${it.message}"))
            }
        }
    }

    fun pauseCampaign(campaignId: Long, reason: String = "Operator paused") {
        viewModelScope.launch {
            val result = adsRepository.pauseCampaign(campaignId, reason)
            result.onSuccess {
                _uiEvents.emit(AuraUiEvent.ShowToast("Campaign paused"))
            }.onFailure {
                _uiEvents.emit(AuraUiEvent.ShowToast("Cannot pause: ${it.message}"))
            }
        }
    }

    fun resumeCampaign(campaignId: Long) {
        viewModelScope.launch {
            val result = adsRepository.resumeCampaign(campaignId)
            result.onSuccess {
                _auraAdsStatus.value = AuraAdsStatus.RUNNING
                _uiEvents.emit(AuraUiEvent.ShowToast("Campaign resumed"))
            }.onFailure {
                _uiEvents.emit(AuraUiEvent.ShowToast("Cannot resume: ${it.message}"))
            }
        }
    }

    fun completeCampaign(campaignId: Long) {
        viewModelScope.launch {
            val result = adsRepository.completeCampaign(campaignId)
            result.onSuccess {
                _auraAdsStatus.value = AuraAdsStatus.COMPLETED
                _uiEvents.emit(AuraUiEvent.ShowToast("Campaign marked COMPLETED"))
            }.onFailure {
                _uiEvents.emit(AuraUiEvent.ShowToast("Cannot complete: ${it.message}"))
            }
        }
    }

    fun cancelCampaign(campaignId: Long, reason: String) {
        viewModelScope.launch {
            val result = adsRepository.cancelCampaign(campaignId, reason)
            result.onSuccess {
                _uiEvents.emit(AuraUiEvent.ShowToast("Campaign cancelled"))
            }.onFailure {
                _uiEvents.emit(AuraUiEvent.ShowToast("Cannot cancel: ${it.message}"))
            }
        }
    }

    fun simulateCampaignMetrics(campaignId: Long) {
        viewModelScope.launch {
            val imp = (250..1200).random().toLong()
            val clicks = (imp * (15..35).random() / 1000).coerceAtLeast(3)
            val conv = (clicks * (5..15).random() / 100).coerceAtLeast(1)
            val spend = clicks * 14.5
            adsRepository.recordCampaignMetrics(campaignId, imp, clicks, conv, spend)
            _uiEvents.emit(AuraUiEvent.ShowToast("Simulated live delivery: +$imp impr, +$clicks clicks"))
        }
    }

    fun savePricingRules(rules: PricingRuleEntity) {
        viewModelScope.launch {
            adsRepository.savePricingRules(rules)
            _uiEvents.emit(AuraUiEvent.ShowToast("Pricing rules & Autonomous mode saved"))
        }
    }

    fun sendClientMessage(prospectId: Long, message: String, channel: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            adsRepository.sendCommunication(prospectId, "OWNER", channel, message)
            adsRepository.logAiAction("COMMUNICATION_SENT", "Dispatched $channel message to client prospect.")
            _uiEvents.emit(AuraUiEvent.ShowToast("Message sent"))
        }
    }

    fun executeAdsCommand(command: String, onReply: (String) -> Unit) {
        if (command.isBlank()) return
        viewModelScope.launch {
            _auraAdsStatus.value = AuraAdsStatus.DISCUSSING
            val rules = adsRepository.getPricingRulesOnce()
            val prospects = clientProspects.value
            val campaigns = adCampaigns.value
            val quotations = adQuotations.value

            val response = adsRepository.aiService.processCommand(command, prospects, campaigns, quotations, rules)
            _auraAdsStatus.value = AuraAdsStatus.READY
            adsRepository.logAiAction("AI_COMMAND", "Processed command: '$command'")
            onReply(response.replyText)
            _uiEvents.emit(AuraUiEvent.SpeakText(response.replyText.take(120)))
        }
    }

    override fun onCleared() {
        super.onCleared()
        workoutTimerJob?.cancel()
        restTimer?.cancel()
        healthService.bridge.stopStepMonitoring()
    }
}
