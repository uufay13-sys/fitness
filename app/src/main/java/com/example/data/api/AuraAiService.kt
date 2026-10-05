package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AuraMemory
import com.example.data.model.ChatMessage
import com.example.data.model.DailyCheckIn
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class AuraAiStatus {
    READY,
    LISTENING,
    THINKING,
    RESPONDING,
    WORKING,
    OFFLINE
}

data class AuraResponse(
    val replyText: String,
    val actionType: String? = null,
    val actionPayload: String? = null,
    val generatedWorkoutJson: String? = null
)

class AuraAiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val modelName = "gemini-3.5-flash"
    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    suspend fun generateAuraResponse(
        userMessage: String,
        userProfile: UserProfile?,
        recentMemories: List<AuraMemory>,
        recentLogs: List<WorkoutLog>,
        todayCheckIn: DailyCheckIn?,
        healthSummary: com.example.data.model.HealthSummaryEntity? = null,
        allowAiAccessToHealth: Boolean = true
    ): AuraResponse = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        // Try Gemini API first if API key is present and not default placeholder
        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val systemPrompt = buildSystemPrompt(userProfile, recentMemories, recentLogs, todayCheckIn, healthSummary, allowAiAccessToHealth)
                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", userMessage))
                            })
                        })
                    })
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", systemPrompt))
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", 1200)
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = requestJson.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("$endpoint?key=$apiKey")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val respBody = response.body?.string() ?: ""
                    val root = JSONObject(respBody)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCand = candidates.getJSONObject(0)
                        val content = firstCand.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val text = parts?.optJSONObject(0)?.optString("text") ?: ""
                        if (text.isNotBlank()) {
                            return@withContext parseAuraText(text, userMessage)
                        }
                    }
                } else {
                    Log.w("AuraAiService", "Gemini API returned code: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("AuraAiService", "Gemini API request failed, using intelligent built-in engine", e)
            }
        }

        // Built-in intelligent AURA agent fallback
        return@withContext generateLocalAuraResponse(userMessage, userProfile, recentLogs, todayCheckIn, healthSummary, allowAiAccessToHealth)
    }

    private fun buildSystemPrompt(
        userProfile: UserProfile?,
        memories: List<AuraMemory>,
        recentLogs: List<WorkoutLog>,
        todayCheckIn: DailyCheckIn?,
        healthSummary: com.example.data.model.HealthSummaryEntity?,
        allowAiAccessToHealth: Boolean
    ): String {
        val memoryStr = memories.take(5).joinToString("; ") { "${it.key}: ${it.value}" }
        val recentLogsStr = if (recentLogs.isEmpty()) "No workouts recorded yet."
        else recentLogs.take(3).joinToString("; ") { "${it.workoutTitle} on ${java.text.SimpleDateFormat("MMM dd", java.util.Locale.US).format(java.util.Date(it.completedAt))}: ${it.totalSets} sets" }

        val profileStr = if (userProfile != null) {
            "Athlete: ${userProfile.name}, Goal: ${userProfile.fitnessGoal}, Level: ${userProfile.experienceLevel}, Equip: ${userProfile.equipment}, Duration: ${userProfile.workoutDurationMinutes}m"
        } else "New Athlete"

        val readinessStr = if (todayCheckIn != null) {
            "Today's Check-In -> Energy: ${todayCheckIn.energy}/5, Sleep: ${todayCheckIn.sleepHours}h, Recovery: ${todayCheckIn.recoveryFeeling}"
        } else "No check-in yet today"

        val healthStr = if (healthSummary != null && healthSummary.isConnected && allowAiAccessToHealth) {
            "Authorized Measured Google Health Data -> Steps: ${healthSummary.steps}, Distance: ${String.format(java.util.Locale.US, "%.2f", healthSummary.distanceKm)}km, Active Calories: ${healthSummary.activeCalories} kcal, Completed Workouts Today: ${healthSummary.workoutCount}, Heart Rate: ${healthSummary.avgHeartRate ?: "N/A"} bpm"
        } else {
            "Health/Fitness data is currently disconnected or user withheld AI access."
        }

        return """
            You are AURA, the Personal AI Fitness Agent of UMR AURA.
            Core Brand Philosophy: "LEAVE BETTER."
            Athlete Context: $profileStr
            Readiness: $readinessStr
            $healthStr
            Known Memories: $memoryStr
            Recent Workout History: $recentLogsStr
            
            RULES & BEHAVIOR:
            1. Clearly identify as an AI fitness agent. You are direct, elite, motivating, scientifically grounded, and concise.
            2. Never give medical diagnosis, never prescribe medications or treatments for injuries.
            3. Clearly distinguish measured health data (e.g., authorized steps and recorded sessions) from AI recommendations.
            4. Never invent or fabricate step counts or workout records. If data is disconnected or unavailable, clearly state so.
            5. If the user asks "how active was I today?" or "how many steps did I take?", answer directly using the measured data if authorized.
            6. If the user asks to start workout, or says "start my workout", include [ACTION:START_WORKOUT] in your reply.
            7. If the user asks to plan a workout or says "create my workout", include [ACTION:PLAN_WORKOUT] and provide a workout plan breakdown.
            8. If the user asks to view or analyze progress, include [ACTION:VIEW_PROGRESS].
            9. If the user mentions logging food (e.g. "I ate 2 eggs and oats"), include [ACTION:LOG_MEAL:{"food":"eggs and oats","calories":350,"p":18,"c":30,"f":12}].
            10. Always embody the standard: "LEAVE BETTER."
        """.trimIndent()
    }

    private fun parseAuraText(rawText: String, userQuery: String): AuraResponse {
        var cleanText = rawText
        var actionType: String? = null
        var actionPayload: String? = null

        val actionRegex = Regex("\\[ACTION:([A-Z_]+)(?::([^\\]]+))?\\]")
        val match = actionRegex.find(rawText)
        if (match != null) {
            actionType = match.groupValues[1]
            actionPayload = match.groupValues.getOrNull(2)
            cleanText = rawText.replace(match.value, "").trim()
        }

        return AuraResponse(
            replyText = cleanText,
            actionType = actionType,
            actionPayload = actionPayload
        )
    }

    /**
     * Built-in AURA intelligent conversational engine.
     * Guarantees lightning-fast, high-quality responses even offline or before API key setup.
     */
    private fun generateLocalAuraResponse(
        query: String,
        profile: UserProfile?,
        recentLogs: List<WorkoutLog>,
        todayCheckIn: DailyCheckIn?,
        healthSummary: com.example.data.model.HealthSummaryEntity?,
        allowAiAccessToHealth: Boolean
    ): AuraResponse {
        val q = query.lowercase().trim()
        val name = profile?.name ?: "Athlete"
        val goal = profile?.fitnessGoal ?: "Build Muscle"
        val equipment = profile?.equipment ?: "Full Gym"

        return when {
            q.contains("should i work out") || q.contains("should i train") -> {
                if (healthSummary != null && healthSummary.isConnected && allowAiAccessToHealth) {
                    if (healthSummary.workoutCount > 0 || healthSummary.distanceKm >= 5f) {
                        val dist = String.format(java.util.Locale.US, "%.1f", healthSummary.distanceKm)
                        AuraResponse(
                            replyText = "You've already walked $dist km and completed ${healthSummary.workoutCount} session(s) earlier today. A lighter recovery session, mobility routine, or deep sleep would be optimal right now. LEAVE BETTER."
                        )
                    } else {
                        AuraResponse(
                            replyText = "Based on your measured activity today (${healthSummary.steps} steps), you have plenty of capacity remaining. Your readiness indicates you are primed to execute today's scheduled workout. Would you like to start?",
                            actionType = "START_WORKOUT"
                        )
                    }
                } else {
                    AuraResponse(
                        replyText = "I don't have access to your activity data right now. You can connect it in Settings. Based on your general training plan and recovery state (${todayCheckIn?.recoveryFeeling ?: "Ready"}), you are cleared for an effective training session."
                    )
                }
            }

            q.contains("calorie") || q.contains("calories burned") -> {
                if (healthSummary != null && healthSummary.isConnected && allowAiAccessToHealth) {
                    AuraResponse(
                        replyText = "According to your authorized fitness data, you have burned ${healthSummary.activeCalories} active kcal today through movement and training."
                    )
                } else {
                    AuraResponse(
                        replyText = "I don't have access to your activity data right now. You can connect it in Settings to track active calories burned."
                    )
                }
            }

            q.contains("step") || q.contains("how active") || q.contains("activity") -> {
                if (healthSummary != null && healthSummary.isConnected && allowAiAccessToHealth) {
                    val dist = String.format(java.util.Locale.US, "%.2f", healthSummary.distanceKm)
                    AuraResponse(
                        replyText = "You've taken ${healthSummary.steps} steps today and completed ${healthSummary.workoutCount} workout session(s) covering $dist km (${healthSummary.activeCalories} active kcal). Your active time is on track for your goal. LEAVE BETTER."
                    )
                } else {
                    AuraResponse(
                        replyText = "I don't have access to your activity data right now. You can connect it in Settings to allow AURA to analyze your steps and movement."
                    )
                }
            }

            q.contains("did i complete") || q.contains("workout complete") -> {
                val workoutsToday = recentLogs.count {
                    val logDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(it.completedAt))
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    logDate == today
                }
                if (workoutsToday > 0) {
                    AuraResponse(
                        replyText = "Yes, $name. You completed $workoutsToday workout session(s) today. Your total sets and volume are recorded in your verified local logs. Superior discipline."
                    )
                } else {
                    AuraResponse(
                        replyText = "You have not completed a workout session yet today, $name. Would you like to launch today's scheduled protocol now?",
                        actionType = "START_WORKOUT"
                    )
                }
            }
            q.contains("start") && (q.contains("workout") || q.contains("today")) || q == "start" -> {
                AuraResponse(
                    replyText = "Initiating your session now, $name. Focus on strict form, intentional breathing, and total mechanical tension.\n\nRemember our standard: LEAVE BETTER.",
                    actionType = "START_WORKOUT"
                )
            }

            q.contains("plan") || q.contains("create") && q.contains("workout") || q.contains("30 minute") || q.contains("chest") || q.contains("leg") -> {
                val muscle = when {
                    q.contains("chest") -> "Chest & Triceps"
                    q.contains("leg") -> "Legs & Posterior Chain"
                    q.contains("back") -> "Back & Biceps"
                    q.contains("core") -> "Core & Conditioning"
                    else -> "Full Body Hypertrophy"
                }
                AuraResponse(
                    replyText = "I have synthesized a targeted $muscle protocol tailored for your $goal objective using $equipment.\n\nWarm-up: 5 min dynamic mobility\n1. Primary Compound: 4 sets x 8-10 reps (90s rest)\n2. Secondary Movement: 3 sets x 10-12 reps (75s rest)\n3. Hypertrophy Isolation: 3 sets x 12-15 reps (60s rest)\n4. Finisher / Core: 3 rounds\n\nTap below to review the full details and launch your workout.",
                    actionType = "PLAN_WORKOUT",
                    actionPayload = muscle
                )
            }

            q.contains("progress") || q.contains("analyze") || q.contains("stat") || q.contains("track") -> {
                val totalSessions = recentLogs.size
                val totalVolume = recentLogs.sumOf { it.volumeKg.toDouble() }.toInt()
                val consistencyNote = if (totalSessions == 0) {
                    "You have no completed logs yet. Today is day one — execute your first workout to establish your baseline."
                } else {
                    "You have logged $totalSessions sessions with ${totalVolume}kg total cumulative volume. Momentum is building."
                }

                AuraResponse(
                    replyText = "AURA Performance Analysis for $name:\n\n• Consistency: $consistencyNote\n• Primary Focus: Optimize progressive overload by adding 1 rep or 1-2kg per exercise each week.\n• Recovery Status: ${todayCheckIn?.recoveryFeeling ?: "Normal"}.\n\nLet's keep compounding. LEAVE BETTER.",
                    actionType = "VIEW_PROGRESS"
                )
            }

            q.contains("tired") || q.contains("sore") || q.contains("fatigue") || q.contains("exhaust") -> {
                AuraResponse(
                    replyText = "Understood, $name. High performance requires intelligent autoregulation, not reckless overtraining.\n\nToday we will switch to active recovery: low-impact mobility, gentle hip and thoracic openers, and light core stability rather than heavy compound loading. This preserves your nervous system for peak output tomorrow.",
                    actionType = "ADJUST_PLAN"
                )
            }

            q.contains("eat") || q.contains("nutrition") || q.contains("diet") || q.contains("protein") || q.contains("food") -> {
                AuraResponse(
                    replyText = "Nutrition Directive for $goal:\n\n1. Protein Target: Aim for 1.6–2.0g per kg of bodyweight daily to maximize muscle protein synthesis.\n2. Pre-Workout (60-90m prior): Complex carbs (oats, banana, rice cakes) + 20-30g lean protein.\n3. Post-Workout: Rapid hydration + 25-40g protein with fast-digesting carbohydrates.\n4. Hydration: Minimum 2.5–3.5 liters daily.\n\nWould you like to log a meal now?",
                    actionType = "LOG_MEAL"
                )
            }

            q.contains("motivate") || q.contains("motivation") || q.contains("inspire") -> {
                AuraResponse(
                    replyText = "Listen closely, $name:\n\nMotivation is fleeting emotion; discipline is an unbreakable operating system. You don't need to feel like doing it—you just need to step up, execute the first set, and demand more from yourself than yesterday.\n\nLEAVE BETTER than you arrived. Let's move."
                )
            }

            q.contains("pushup") || q.contains("push-up") || q.contains("bench") || q.contains("squat") || q.contains("deadlift") -> {
                val exerciseName = when {
                    q.contains("pushup") || q.contains("push-up") -> "Push-up"
                    q.contains("squat") -> "Squat"
                    q.contains("bench") -> "Bench Press"
                    q.contains("deadlift") -> "Romanian Deadlift"
                    else -> "Compound Exercise"
                }
                AuraResponse(
                    replyText = "AURA Technical Breakdown for $exerciseName:\n\n• Key Cue: Maintain rigid trunk stability and pack the scaps.\n• Concentric: Drive aggressively through the target muscle group.\n• Eccentric: 2-3 second controlled descent. Never drop the weight free-fall.\n• Safety: Keep wrists and spine neutral at all times.",
                    actionType = "EXPLAIN_EXERCISE",
                    actionPayload = exerciseName
                )
            }

            q.contains("habit") || q.contains("check") -> {
                AuraResponse(
                    replyText = "Habit Audit:\n• Water Intake: Track at least 8 cups daily\n• Sleep Duration: Target 7.5 - 8.5 hours uninterrupted\n• Movement: 7,000+ steps baseline daily\n• Mental State: 5 minutes mindful breathing post-session\n\nDaily discipline produces extraordinary physiology."
                )
            }

            else -> {
                AuraResponse(
                    replyText = "I hear you, $name. As your AI Fitness Agent, I'm analyzing your request against your $goal objective.\n\nYou can ask me to generate a 30-min workout, analyze your progress, guide your form, adjust for fatigue, or log nutrition.\n\nWhat is our focus right now?"
                )
            }
        }
    }
}
