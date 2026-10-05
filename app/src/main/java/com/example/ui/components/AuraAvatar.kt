package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.AuraAiStatus
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraCyanGlow
import com.example.ui.theme.AuraLime
import com.example.ui.theme.AuraLimeGlow
import com.example.ui.theme.AuraStatusListening
import com.example.ui.theme.AuraStatusOffline
import com.example.ui.theme.AuraStatusReady
import com.example.ui.theme.AuraStatusResponding
import com.example.ui.theme.AuraStatusThinking
import com.example.ui.theme.AuraStatusWorking
import com.example.ui.theme.AuraViolet

@Composable
fun AuraAvatar(
    status: AuraAiStatus,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    showLabel: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AuraBreathing")

    // Breathing pulse scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Outer ring rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAngle"
    )

    // Dynamic color depending on status
    val primaryColor = when (status) {
        AuraAiStatus.READY -> AuraLime
        AuraAiStatus.LISTENING -> AuraCyan
        AuraAiStatus.THINKING -> AuraViolet
        AuraAiStatus.RESPONDING -> AuraCyanGlow
        AuraAiStatus.WORKING -> Color(0xFFFFB800)
        AuraAiStatus.OFFLINE -> Color(0xFF64748B)
    }

    val secondaryColor = when (status) {
        AuraAiStatus.READY -> AuraCyan
        AuraAiStatus.LISTENING -> AuraLime
        AuraAiStatus.THINKING -> AuraCyan
        AuraAiStatus.RESPONDING -> AuraLimeGlow
        AuraAiStatus.WORKING -> AuraViolet
        AuraAiStatus.OFFLINE -> Color(0xFF334155)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size)
        ) {
            // Animated Glowing Outer Pulse Ring
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(if (status == AuraAiStatus.OFFLINE) 1f else pulseScale)
                    .rotate(rotationAngle)
            ) {
                val radius = this.size.minDimension / 2
                val center = Offset(this.size.width / 2, this.size.height / 2)

                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(primaryColor, secondaryColor, primaryColor)
                    ),
                    radius = radius - 4.dp.toPx(),
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Energy orbital nodes
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = Offset(center.x + (radius - 4.dp.toPx()), center.y)
                )
                drawCircle(
                    color = secondaryColor,
                    radius = 3.dp.toPx(),
                    center = Offset(center.x - (radius - 4.dp.toPx()), center.y)
                )
            }

            // Core Avatar Orb
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size * 0.72f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                primaryColor.copy(alpha = 0.35f),
                                Color(0xFF0F172A).copy(alpha = 0.95f),
                                Color(0xFF080C14)
                            )
                        )
                    )
                    .border(1.5.dp, primaryColor.copy(alpha = 0.6f), CircleShape)
            ) {
                // Futuristic central icon or wave
                when (status) {
                    AuraAiStatus.LISTENING -> {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Listening",
                            tint = primaryColor,
                            modifier = Modifier.size(size * 0.35f)
                        )
                    }
                    AuraAiStatus.THINKING -> {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Thinking",
                            tint = primaryColor,
                            modifier = Modifier.size(size * 0.38f)
                        )
                    }
                    AuraAiStatus.RESPONDING -> {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Responding",
                            tint = primaryColor,
                            modifier = Modifier.size(size * 0.35f)
                        )
                    }
                    AuraAiStatus.WORKING -> {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Working",
                            tint = primaryColor,
                            modifier = Modifier.size(size * 0.35f)
                        )
                    }
                    AuraAiStatus.OFFLINE -> {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Offline",
                            tint = primaryColor,
                            modifier = Modifier.size(size * 0.35f)
                        )
                    }
                    else -> {
                        // READY - Stylized AURA AI Emblem
                        Text(
                            text = "A",
                            color = primaryColor,
                            fontSize = (size.value * 0.35f).sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(8.dp))
            AuraStatusBadge(status = status)
        }
    }
}

@Composable
fun AuraStatusBadge(
    status: AuraAiStatus,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText) = when (status) {
        AuraAiStatus.READY -> Pair(AuraStatusReady, "READY")
        AuraAiStatus.LISTENING -> Pair(AuraStatusListening, "LISTENING")
        AuraAiStatus.THINKING -> Pair(AuraStatusThinking, "THINKING")
        AuraAiStatus.RESPONDING -> Pair(AuraStatusResponding, "RESPONDING")
        AuraAiStatus.WORKING -> Pair(AuraStatusWorking, "WORKING")
        AuraAiStatus.OFFLINE -> Pair(AuraStatusOffline, "OFFLINE")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = statusColor.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "AURA: $statusText",
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
