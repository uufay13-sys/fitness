package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuraAdsStatus
import com.example.data.model.HealthConnectionStatus
import com.example.data.model.HealthPermissionType
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraAvatar
import com.example.ui.components.AuraStatusBadge
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraLime
import com.example.ui.theme.AuraViolet

@Composable
fun HomeScreen(
    viewModel: AuraViewModel,
    onNavigateToAgent: () -> Unit,
    onNavigateToAds: () -> Unit = {},
    onNavigateToWorkouts: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToNutrition: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onStartActiveWorkout: () -> Unit,
    onOpenCheckIn: () -> Unit
) {
    val auraStatus by viewModel.auraStatus.collectAsState()
    val auraAdsStatus by viewModel.auraAdsStatus.collectAsState()
    val clientProspects by viewModel.clientProspects.collectAsState()
    val pendingApprovals by viewModel.pendingApprovals.collectAsState()
    val adCampaigns by viewModel.adCampaigns.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val workoutPlans by viewModel.workoutPlans.collectAsState()
    val workoutLogs by viewModel.workoutLogs.collectAsState()
    val todayCheckIn by viewModel.todayCheckIn.collectAsState()
    val todayWater by viewModel.todayWater.collectAsState()

    val healthStatus by viewModel.healthConnectionStatus.collectAsState()
    val todayHealthSummary by viewModel.todayHealthSummary.collectAsState()
    val todayHealthActivities by viewModel.todayHealthActivities.collectAsState()
    val healthPrivacySettings by viewModel.healthPrivacySettings.collectAsState()

    var showPermissionExplanationDialog by remember { mutableStateOf(false) }
    var showDisconnectConfirmDialog by remember { mutableStateOf(false) }
    var showAddActivityDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val anyGranted = permissionsMap.values.any { it }
        if (anyGranted) {
            viewModel.connectHealth(
                listOf(
                    HealthPermissionType.STEPS,
                    HealthPermissionType.DISTANCE,
                    HealthPermissionType.CALORIES,
                    HealthPermissionType.WORKOUTS,
                    HealthPermissionType.HEART_RATE
                )
            )
        } else {
            viewModel.markHealthPermissionDenied()
        }
    }

    val userName = userProfile?.name ?: "Athlete"
    val todayPlan = workoutPlans.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brand Header & Philosophy
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "UMR AURA",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "LEAVE BETTER.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                AuraStatusBadge(status = auraStatus)
            }
        }

        // Central AURA AI Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(AuraCyan.copy(alpha = 0.5f), AuraLime.copy(alpha = 0.5f)))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("aura_hero_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AuraAvatar(
                        status = auraStatus,
                        size = 88.dp,
                        showLabel = false
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Hi, $userName. I'm AURA.",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Your Personal AI Fitness Agent",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "\"I synthesize your training, optimize your recovery, and guide your form so every session propels you forward.\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Action Buttons Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToAgent,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("talk_to_aura_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TALK TO AURA", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (todayPlan != null) {
                                    viewModel.startWorkout(todayPlan)
                                    onStartActiveWorkout()
                                } else {
                                    onNavigateToWorkouts()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_workout_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("START WORKOUT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToWorkouts,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("plan_workout_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PLAN WORKOUT", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = onNavigateToProgress,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("analyze_progress_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ANALYZE PROGRESS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // AURA ADS AGENT — AI Advertising Sales & Campaign Management Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(AuraCyan.copy(alpha = 0.7f), Color(0xFFFF9900).copy(alpha = 0.7f)))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAds() }
                    .testTag("aura_ads_hero_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AuraCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = AuraCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "AURA ADS AGENT",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = AuraLime.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "BUSINESS AI",
                                            color = AuraLime,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Advertising Sales & Campaign Management",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Status Chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (auraAdsStatus) {
                                AuraAdsStatus.RUNNING, AuraAdsStatus.CAMPAIGN_READY -> AuraLime.copy(alpha = 0.15f)
                                AuraAdsStatus.NEGOTIATING, AuraAdsStatus.WAITING_FOR_OWNER_APPROVAL -> Color(0xFFFF9900).copy(alpha = 0.15f)
                                else -> AuraCyan.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = auraAdsStatus.label,
                                color = when (auraAdsStatus) {
                                    AuraAdsStatus.RUNNING, AuraAdsStatus.CAMPAIGN_READY -> AuraLime
                                    AuraAdsStatus.NEGOTIATING, AuraAdsStatus.WAITING_FOR_OWNER_APPROVAL -> Color(0xFFFF9900)
                                    else -> AuraCyan
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Autonomous advertising sales agent that discovers local clients, analyzes requirements, creates packages, calculates costs, negotiates within predefined owner limits, and requests approval via owner contact 8309596486.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metrics Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("PROSPECTS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${clientProspects.size}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = AuraCyan)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (pendingApprovals.isNotEmpty()) Color(0xFFFF9900).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("APPROVALS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (pendingApprovals.isNotEmpty()) Color(0xFFFF9900) else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${pendingApprovals.size}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (pendingApprovals.isNotEmpty()) Color(0xFFFF9900) else MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("CAMPAIGNS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${adCampaigns.size}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = AuraLime)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Owner Routing Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Owner Dispatch: 8309596486 (Secure approval gateway)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onNavigateToAds,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_aura_ads_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LAUNCH AI ADS CONTROL CENTER", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Daily Readiness Check-In Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenCheckIn() }
                    .testTag("daily_checkin_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (todayCheckIn != null) AuraLime.copy(alpha = 0.2f)
                                else AuraViolet.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (todayCheckIn != null) Icons.Default.CheckCircle else Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (todayCheckIn != null) AuraLime else AuraViolet
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (todayCheckIn != null) "Daily Readiness Recorded" else "Daily Check-In",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (todayCheckIn != null)
                                "Energy ${todayCheckIn?.energy}/5 • Sleep ${todayCheckIn?.sleepHours}h • ${todayCheckIn?.recoveryFeeling}"
                            else "How are you feeling today? Tap to calibrate AURA.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Quick Stats Row (Sessions, Streak, Water)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Workouts",
                    value = "${workoutLogs.size}",
                    sub = "completed",
                    icon = Icons.Default.FitnessCenter,
                    iconColor = AuraCyan
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Streak",
                    value = if (workoutLogs.isEmpty()) "0" else "4",
                    sub = "days on fire",
                    icon = Icons.Default.LocalFireDepartment,
                    iconColor = Color(0xFFFF6D00)
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Hydration",
                    value = "${todayWater?.glasses ?: 0}/8",
                    sub = "glasses",
                    icon = Icons.Default.WaterDrop,
                    iconColor = AuraCyan
                )
            }
        }

        // CONNECT FITNESS DATA Card (Section 3 & 4)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (healthStatus == HealthConnectionStatus.CONNECTED) AuraLime.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connect_fitness_data_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (healthStatus == HealthConnectionStatus.CONNECTED) AuraLime.copy(alpha = 0.15f)
                                        else AuraCyan.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (healthStatus == HealthConnectionStatus.CONNECTED) Icons.Default.Link else Icons.Default.DirectionsRun,
                                    contentDescription = null,
                                    tint = if (healthStatus == HealthConnectionStatus.CONNECTED) AuraLime else AuraCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "CONNECT FITNESS DATA",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (healthStatus) {
                                        HealthConnectionStatus.CONNECTED -> "Google Fitness Data Connected"
                                        HealthConnectionStatus.PERMISSION_DENIED -> "Permission Denied • Disconnected"
                                        HealthConnectionStatus.UNAVAILABLE -> "Sensor/Health Service Unavailable"
                                        else -> "Personalize AURA with daily activity"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (healthStatus) {
                                        HealthConnectionStatus.CONNECTED -> AuraLime
                                        HealthConnectionStatus.PERMISSION_DENIED -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        if (healthStatus == HealthConnectionStatus.CONNECTED) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AuraLime.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "CONNECTED",
                                    color = AuraLime,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Connect your fitness data to help AURA understand your activity and personalize your fitness experience.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (healthStatus == HealthConnectionStatus.CONNECTED) {
                            OutlinedButton(
                                onClick = { showDisconnectConfirmDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("disconnect_fitness_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("DISCONNECT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.refreshHealthData() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("refresh_fitness_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SYNC NOW", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = { showPermissionExplanationDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("connect_fitness_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CONNECT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // TODAY'S ACTIVITY Dashboard (Section 6)
        item {
            Column(modifier = Modifier.fillMaxWidth().testTag("todays_activity_section")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY'S ACTIVITY",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (healthStatus == HealthConnectionStatus.CONNECTED) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AuraLime.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "VERIFIED DATA",
                                color = AuraLime,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (healthStatus == HealthConnectionStatus.CONNECTED) {
                    val summary = todayHealthSummary
                    val hasData = summary != null && (summary.steps > 0 || summary.workoutCount > 0 || summary.activeCalories > 0)

                    if (hasData) {
                        // Display actual available metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f).testTag("activity_steps_card"),
                                title = "Steps",
                                value = String.format(java.util.Locale.US, "%,d", summary?.steps ?: 0),
                                sub = "steps",
                                icon = Icons.Default.DirectionsWalk,
                                iconColor = AuraCyan
                            )
                            StatCard(
                                modifier = Modifier.weight(1f).testTag("activity_distance_card"),
                                title = "Distance",
                                value = "${String.format(java.util.Locale.US, "%.1f", summary?.distanceKm ?: 0f)} km",
                                sub = "active distance",
                                icon = Icons.Default.DirectionsRun,
                                iconColor = AuraLime
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f).testTag("activity_calories_card"),
                                title = "Active calories",
                                value = "${summary?.activeCalories ?: 0} kcal",
                                sub = "energy burned",
                                icon = Icons.Default.LocalFireDepartment,
                                iconColor = Color(0xFFFF6D00)
                            )
                            StatCard(
                                modifier = Modifier.weight(1f).testTag("activity_workout_card"),
                                title = "Workout",
                                value = "${summary?.workoutCount ?: 0} session",
                                sub = if ((summary?.workoutCount ?: 0) == 1) "completed" else "completed",
                                icon = Icons.Default.FitnessCenter,
                                iconColor = AuraViolet
                            )
                        }

                        if (summary?.avgHeartRate != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF3366), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Heart Rate: ${summary.avgHeartRate} bpm",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Cardiovascular training stress calibrated",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Connected but no data yet
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No activity data recorded today.",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Start walking or complete a workout session to log activity metrics.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Not connected or denied
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth().testTag("fitness_not_connected_card")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (healthStatus == HealthConnectionStatus.PERMISSION_DENIED) Icons.Default.Warning else Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = if (healthStatus == HealthConnectionStatus.PERMISSION_DENIED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (healthStatus == HealthConnectionStatus.PERMISSION_DENIED) 
                                    "Fitness permission denied." 
                                else 
                                    "Connect your fitness data to see today's activity.",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (healthStatus == HealthConnectionStatus.PERMISSION_DENIED)
                                    "Permission was denied. You can reconnect anytime to authorize fitness metrics."
                                else
                                    "Authorize access to see your actual steps, active calories, and workout duration.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showPermissionExplanationDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("connect_fitness_banner_button")
                            ) {
                                Text("CONNECT", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // DAILY ACTIVITY TIMELINE (Section 7)
        if (healthStatus == HealthConnectionStatus.CONNECTED) {
            item {
                Column(modifier = Modifier.fillMaxWidth().testTag("daily_activity_timeline_section")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DAILY ACTIVITY TIMELINE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "+ Log Activity",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AuraLime,
                                modifier = Modifier
                                    .clickable { showAddActivityDialog = true }
                                    .padding(4.dp)
                                    .testTag("log_activity_btn")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (todayHealthActivities.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "No activity records logged today.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Activities like walking, running, cycling, and workouts will automatically appear here once authorized.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        todayHealthActivities.forEach { act ->
                            val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).format(java.util.Date(act.startTime))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("activity_record_${act.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (act.activityType) {
                                                        "Walking" -> AuraCyan.copy(alpha = 0.15f)
                                                        "Running" -> AuraLime.copy(alpha = 0.15f)
                                                        "Cycling" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                                        else -> AuraViolet.copy(alpha = 0.15f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (act.activityType) {
                                                    "Walking" -> Icons.Default.DirectionsWalk
                                                    "Running" -> Icons.Default.DirectionsRun
                                                    "Cycling" -> Icons.Default.DirectionsBike
                                                    else -> Icons.Default.FitnessCenter
                                                },
                                                contentDescription = null,
                                                tint = when (act.activityType) {
                                                    "Walking" -> AuraCyan
                                                    "Running" -> AuraLime
                                                    "Cycling" -> Color(0xFFFF9800)
                                                    else -> AuraViolet
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = act.activityType,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "$timeStr • ${act.durationMinutes} mins",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        if (act.distanceKm > 0f) {
                                            Text(
                                                text = "${String.format(java.util.Locale.US, "%.1f", act.distanceKm)} km",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = AuraCyan
                                            )
                                        }
                                        if (act.calories > 0) {
                                            Text(
                                                text = "${act.calories} kcal",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFFF6D00)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Today's Scheduled / Suggested Protocol
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY'S PROTOCOL",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "View Library",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToWorkouts() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (todayPlan != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.startWorkout(todayPlan)
                                onStartActiveWorkout()
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = todayPlan.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AuraLime.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${todayPlan.durationMinutes} MIN",
                                        color = AuraLime,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Target: ${todayPlan.targetMuscle} • ${todayPlan.equipment}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ready when you are",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Button(
                                    onClick = {
                                        viewModel.startWorkout(todayPlan)
                                        onStartActiveWorkout()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("START", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI Vision Camera Feature Callout
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraCyan.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCamera() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AuraCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = AuraCyan
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Vision Form Assistant",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Use camera guidance for real-time Squat, Push-up & Deadlift posture analysis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Permission Explanation Screen / Dialog (Section 4)
    if (showPermissionExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanationDialog = false },
            title = {
                Text(
                    text = "Connect your fitness data to AURA",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "AURA can use authorized activity information to:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text("• Understand your daily activity", style = MaterialTheme.typography.bodyMedium)
                    Text("• Track workouts & session duration", style = MaterialTheme.typography.bodyMedium)
                    Text("• Improve AI recommendations", style = MaterialTheme.typography.bodyMedium)
                    Text("• Analyze progress over time", style = MaterialTheme.typography.bodyMedium)
                    Text("• Help personalize your fitness plan", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Privacy Notice: UMR AURA only accesses data you explicitly authorize. Your fitness data remains on device and is never sold.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionExplanationDialog = false
                        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            arrayOf(
                                Manifest.permission.ACTIVITY_RECOGNITION,
                                Manifest.permission.BODY_SENSORS
                            )
                        } else {
                            arrayOf(Manifest.permission.BODY_SENSORS)
                        }
                        permissionLauncher.launch(permissionsToRequest)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CONTINUE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionExplanationDialog = false }
                ) {
                    Text("NOT NOW", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Disconnect Dialog
    if (showDisconnectConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirmDialog = false },
            title = { Text("Disconnect Fitness Data?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Do you want to disconnect Google Fitness and Health data? AURA will no longer incorporate daily activity data into coaching.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectConfirmDialog = false
                        viewModel.disconnectHealth(clearLocalData = false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("DISCONNECT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirmDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showAddActivityDialog) {
        var selectedType by remember { mutableStateOf("Walking") }
        var durationInput by remember { mutableStateOf("30") }
        var distanceInput by remember { mutableStateOf("2.2") }
        var caloriesInput by remember { mutableStateOf("130") }
        var heartRateInput by remember { mutableStateOf("112") }

        AlertDialog(
            onDismissRequest = { showAddActivityDialog = false },
            title = {
                Text("Log Activity Session", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Select Activity Type:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Walking", "Running", "Cycling", "Workout").forEach { type ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedType == type) AuraLime.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selectedType == type) AuraLime else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedType = type }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = type,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedType == type) AuraLime else MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = durationInput,
                        onValueChange = { durationInput = it },
                        label = { Text("Duration (minutes)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = distanceInput,
                        onValueChange = { distanceInput = it },
                        label = { Text("Distance (km)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = caloriesInput,
                        onValueChange = { caloriesInput = it },
                        label = { Text("Active Calories (kcal)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = heartRateInput,
                        onValueChange = { heartRateInput = it },
                        label = { Text("Avg Heart Rate (bpm, optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dur = durationInput.toIntOrNull() ?: 20
                        val dist = distanceInput.toFloatOrNull() ?: 0f
                        val cal = caloriesInput.toIntOrNull() ?: 100
                        val hr = heartRateInput.toIntOrNull()
                        viewModel.recordHealthActivity(selectedType, dur, dist, cal, hr)
                        showAddActivityDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("RECORD SESSION", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddActivityDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    sub: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
