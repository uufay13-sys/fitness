package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.AuraAiStatus
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraAvatar
import com.example.ui.components.WorkoutTimer
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraLime
import com.example.ui.theme.AuraViolet

@Composable
fun ActiveWorkoutScreen(
    viewModel: AuraViewModel,
    onFinishAndExit: () -> Unit
) {
    val session by viewModel.activeWorkout.collectAsState()
    var userNotes by remember { mutableStateOf("") }

    if (session == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No active session in progress.", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onFinishAndExit) {
                    Text("Return to Hub")
                }
            }
        }
        return
    }

    val activeSession = session!!
    val currentExerciseState = activeSession.exercises.getOrNull(activeSession.currentExerciseIndex)

    var currentWeight by remember(activeSession.currentExerciseIndex) {
        mutableFloatStateOf(currentExerciseState?.currentWeightKg ?: 20f)
    }
    var currentReps by remember(activeSession.currentExerciseIndex) {
        mutableIntStateOf(currentExerciseState?.currentReps ?: 10)
    }

    // Format elapsed session time (MM:SS)
    val minutes = activeSession.elapsedSeconds / 60
    val seconds = activeSession.elapsedSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top HUD Bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = activeSession.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "TOTAL ELAPSED: $formattedTime",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = AuraCyan
                    )
                }

                // Ask if workout is done button
                OutlinedButton(
                    onClick = { viewModel.requestEndWorkoutPrompt() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("end_workout_early_btn")
                ) {
                    Text("CONCLUDE SESSION", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AURA Live Tactical Coaching Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraCyan.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraAvatar(
                        status = if (activeSession.isResting) AuraAiStatus.WORKING else AuraAiStatus.RESPONDING,
                        size = 34.dp,
                        showLabel = false
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AURA LIVE COACH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = activeSession.auraLiveCoachMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Center Area: Reusable WorkoutTimer in Rest Mode OR Active Set Tracking Card with Timer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (activeSession.isResting) {
                // Full Rest Interval Mode with Audio/Visual Timer component
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WorkoutTimer(
                        isResting = true,
                        secondsRemaining = activeSession.restSecondsRemaining,
                        totalRestSeconds = activeSession.restTotalSeconds,
                        activeSetDurationSeconds = activeSession.currentSetDurationSeconds,
                        isPaused = activeSession.isTimerPaused,
                        onTogglePause = { viewModel.toggleTimerPause() },
                        onAdjustRest = { delta -> viewModel.adjustRestSeconds(delta) },
                        onSkipRest = { viewModel.skipRest() },
                        size = 190.dp
                    )
                }
            } else if (currentExerciseState != null) {
                // Exercise Execution View with Integrated Set Timer component
                val ex = currentExerciseState.exercise
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EXERCISE ${activeSession.currentExerciseIndex + 1} OF ${activeSession.exercises.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AuraLime.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "SET ${currentExerciseState.completedSets + 1} OF ${currentExerciseState.targetSets}",
                                    color = AuraLime,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = ex.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "${ex.muscleGroup} • Target: ${currentExerciseState.targetReps} reps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Integrated Reusable Timer in Active Set Mode (Time Under Tension)
                        WorkoutTimer(
                            isResting = false,
                            secondsRemaining = activeSession.restSecondsRemaining,
                            totalRestSeconds = activeSession.restTotalSeconds,
                            activeSetDurationSeconds = activeSession.currentSetDurationSeconds,
                            isPaused = activeSession.isTimerPaused,
                            onTogglePause = { viewModel.toggleTimerPause() },
                            onAdjustRest = { delta -> viewModel.adjustRestSeconds(delta) },
                            onSkipRest = { viewModel.skipRest() },
                            size = 120.dp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Weight Adjuster
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Weight (KG):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { currentWeight = (currentWeight - 2.5f).coerceAtLeast(0f) }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease Weight")
                                }
                                Text(
                                    text = "${currentWeight.toInt()} kg",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { currentWeight += 2.5f }) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase Weight")
                                }
                            }
                        }

                        // Reps Adjuster
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reps Performed:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { currentReps = (currentReps - 1).coerceAtLeast(1) }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease Reps")
                                }
                                Text(
                                    text = "$currentReps",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = AuraLime,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { currentReps += 1 }) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase Reps")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = ex.instructions,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Bottom Controls: COMPLETE SET / ADVANCE
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    viewModel.completeSet(currentWeight, currentReps)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("complete_set_button")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("COMPLETE SET", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.addSetToCurrent() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ ADD SET", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.advanceExercise() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("NEXT MOVEMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Proactive AURA Prompt: Agent asks the user if workout is done or not!
    if (activeSession.promptIsWorkoutDone) {
        AlertDialog(
            onDismissRequest = { /* forces explicit user answer */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuraAvatar(status = AuraAiStatus.RESPONDING, size = 34.dp, showLabel = false)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AURA: Workout Done or Not?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "All scheduled movements are completed! Are you done with today's session, or would you like to keep going and train more?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("• Completed Sets: ${activeSession.completedSetsCount}", fontSize = 12.sp)
                            Text("• Total Volume: ${activeSession.totalVolumeKg.toInt()} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AuraLime)
                            Text("• Session Time: $formattedTime", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = userNotes,
                        onValueChange = { userNotes = it },
                        placeholder = { Text("Session notes or achievements...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.userRespondedWorkoutDone(true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraLime,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("workout_done_confirm_yes")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("YES, I'M DONE (SAVE)", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.userRespondedWorkoutDone(false)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("workout_done_confirm_no")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("NOT DONE — KEEP TRAINING", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        )
    }

    // Finished Celebration Dialog
    if (activeSession.isFinished) {
        AlertDialog(
            onDismissRequest = {
                viewModel.exitWorkout()
                onFinishAndExit()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AuraLime)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WORKOUT COMPLETE", fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column {
                    Text(
                        text = "LEAVE BETTER.",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = AuraLime
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your performance metrics have been recorded in the local Room database.")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Duration: $formattedTime")
                    Text("• Total Sets: ${activeSession.completedSetsCount}")
                    Text("• Volume: ${activeSession.totalVolumeKg.toInt()} kg")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.exitWorkout()
                        onFinishAndExit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraLime, contentColor = Color.Black)
                ) {
                    Text("RETURN TO PROGRESS HUB", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
