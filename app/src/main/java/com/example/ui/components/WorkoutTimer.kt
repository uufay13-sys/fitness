package com.example.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraLime
import com.example.ui.theme.AuraViolet

/**
 * Reusable WorkoutTimer component for Workout Mode.
 *
 * Provides:
 * - Dynamic visual cues (smooth circular progress arc, color shifting, final 5s pulse warning).
 * - Audible cues (tone generator beeps on 3, 2, 1s and completion fanfare).
 * - Haptic cues (tactile pulses).
 * - Dual-mode operation:
 *     1. Rest Period Countdown (with +15s, +30s, -15s, skip controls).
 *     2. Active Set Duration (Time Under Tension stopwatch).
 */
@Composable
fun WorkoutTimer(
    isResting: Boolean,
    secondsRemaining: Int,
    totalRestSeconds: Int,
    activeSetDurationSeconds: Int,
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onAdjustRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 210.dp
) {
    val context = LocalContext.current
    var isAudioMuted by remember { mutableStateOf(false) }

    // Audio Tone Generator & Vibrator for cues
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        } catch (e: Exception) {
            null
        }
    }

    val vibrator = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                toneGenerator?.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Audible and tactile cues on countdown
    var lastAlertedSecond by remember { mutableIntStateOf(-1) }

    LaunchedEffect(secondsRemaining, isResting, isPaused) {
        if (isResting && !isPaused && secondsRemaining != lastAlertedSecond) {
            lastAlertedSecond = secondsRemaining
            if (secondsRemaining in 1..3 && !isAudioMuted) {
                // 3, 2, 1 warning beeps
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(100)
                    }
                } catch (e: Exception) {
                    // ignore
                }
            } else if (secondsRemaining == 0 && !isAudioMuted) {
                // Completion fanfare tone
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 300)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(
                            VibrationEffect.createWaveform(longArrayOf(0, 150, 80, 200), -1)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(300)
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    // Pulse animation for critical final 5 seconds or active set rhythm
    val infiniteTransition = rememberInfiniteTransition(label = "TimerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = if (isResting && secondsRemaining in 1..5) 0.96f else 0.99f,
        targetValue = if (isResting && secondsRemaining in 1..5) 1.05f else 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isResting && secondsRemaining in 1..5) 450 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAnimation"
    )

    // Dynamic Ring Color
    val ringColor by animateColorAsState(
        targetValue = when {
            !isResting -> AuraCyan
            secondsRemaining <= 5 -> Color(0xFFEF4444) // Urgent Red Alert
            secondsRemaining <= 15 -> Color(0xFFFFB800) // Amber Warning
            else -> AuraLime // Fresh Rest
        },
        label = "RingColor"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ringColor.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("workout_timer_component")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Mode Tag & Audio Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ringColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ringColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ringColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isResting) "REST INTERVAL" else "ACTIVE SET DURATION",
                            color = ringColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isAudioMuted = !isAudioMuted },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isAudioMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isAudioMuted) "Unmute Beeps" else "Mute Beeps",
                            tint = if (isAudioMuted) Color.Gray else ringColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Circular Visual Timer Dial
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size)
                    .scale(pulseScale)
            ) {
                val progress = if (isResting) {
                    if (totalRestSeconds > 0) secondsRemaining.toFloat() / totalRestSeconds.toFloat() else 0f
                } else {
                    // Pulsing sweep for active set
                    (activeSetDurationSeconds % 60) / 60f
                }

                // Outer animated arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 10.dp.toPx()
                    val diameter = this.size.minDimension - strokeW
                    val topLeft = Offset(strokeW / 2, strokeW / 2)
                    val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                    // Background Track
                    drawArc(
                        color = Color(0xFF1E293B),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // Active Countdown / Duration Arc
                    val sweep = if (isResting) progress * 360f else (progress * 360f).coerceAtLeast(15f)
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(ringColor.copy(alpha = 0.7f), ringColor, ringColor.copy(alpha = 0.7f))
                        ),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                // Center Display Digits
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isResting) {
                        Text(
                            text = "${secondsRemaining}s",
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isPaused) "PAUSED" else if (secondsRemaining <= 5) "READY FOR SET!" else "RECOVER & HYDRATE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = if (secondsRemaining <= 5) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val setMins = activeSetDurationSeconds / 60
                        val setSecs = activeSetDurationSeconds % 60
                        val formattedSet = String.format("%02d:%02d", setMins, setSecs)

                        Text(
                            text = formattedSet,
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                            color = AuraCyan
                        )
                        Text(
                            text = if (isPaused) "SET PAUSED" else "TIME UNDER TENSION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Bar for Timer
            if (isResting) {
                // Rest period quick interval adjustments (+15s, +30s, -15s)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onAdjustRest(-15) }
                    ) {
                        Text(
                            text = "-15s",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onAdjustRest(15) }
                    ) {
                        Text(
                            text = "+15s",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onAdjustRest(30) }
                    ) {
                        Text(
                            text = "+30s",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pause / Skip Rest buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onTogglePause,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPaused) "RESUME" else "PAUSE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onSkipRest,
                        colors = ButtonDefaults.buttonColors(containerColor = AuraLime, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("timer_skip_rest_btn")
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SKIP REST", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            } else {
                // Active Set Duration controls (Pause / Resume set stopwatch)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = onTogglePause,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPaused) "RESUME SET TIMER" else "PAUSE SET TIMER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
