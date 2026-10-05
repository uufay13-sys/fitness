package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuraMemory
import com.example.data.model.HealthConnectionStatus
import com.example.data.model.HealthPermissionType
import com.example.data.model.UserProfile
import com.example.ui.AuraViewModel
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraLime
import com.example.ui.theme.AuraViolet

@Composable
fun ProfileScreen(
    viewModel: AuraViewModel
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var showResetMemoryConfirm by remember { mutableStateOf(false) }

    val healthStatus by viewModel.healthConnectionStatus.collectAsState()
    val healthSettings by viewModel.healthPrivacySettings.collectAsState()
    val todayHealthSummary by viewModel.todayHealthSummary.collectAsState()

    var showDeleteHealthConfirm by remember { mutableStateOf(false) }
    var showDisconnectHealthConfirm by remember { mutableStateOf(false) }
    var showConnectExplanationDialog by remember { mutableStateOf(false) }

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

    // Contact Form state
    var contactName by remember { mutableStateOf(userProfile?.name ?: "") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactMessage by remember { mutableStateOf("") }
    var contactSending by remember { mutableStateOf(false) }
    var contactSentSuccess by remember { mutableStateOf(false) }
    var contactError by remember { mutableStateOf<String?>(null) }

    val currentProfile = userProfile ?: UserProfile()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "ATHLETE PROFILE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Customized AI telemetry & system preferences",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Profile Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().testTag("user_profile_card")
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
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(AuraCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentProfile.name.take(1).uppercase(),
                                    color = AuraCyan,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentProfile.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${currentProfile.fitnessGoal} • ${currentProfile.experienceLevel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(onClick = { showEditProfileDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ProfileStatItem("Age", "${currentProfile.age} yrs")
                        ProfileStatItem("Height", "${currentProfile.heightCm.toInt()} cm")
                        ProfileStatItem("Weight", "${currentProfile.weightKg.toInt()} kg")
                        ProfileStatItem("Duration", "${currentProfile.workoutDurationMinutes} min")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Equipment: ${currentProfile.equipment} • Diet: ${currentProfile.dietaryPreference}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Dark / Light Theme Toggle Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isDarkTheme) "Dark Aesthetic Mode" else "Light Mode",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Persistent theme preference",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { viewModel.toggleTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AuraCyan,
                            checkedTrackColor = AuraCyan.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        // AURA MEMORY Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().testTag("aura_memory_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = AuraCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AURA MEMORY",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { showAddMemoryDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ADD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "AURA stores goals, injury flags, and personal preferences to shape future workout programming.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (memories.isEmpty()) {
                        Text(
                            text = "No custom memories saved. Add goals or injury limitations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            memories.forEach { mem ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = AuraCyan.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = mem.category.uppercase(),
                                                        color = AuraCyan,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = mem.key,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = mem.value,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(onClick = { viewModel.deleteMemory(mem) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showResetMemoryConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESET AURA MEMORY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // HEALTH & FITNESS DATA PRIVACY (Section 8)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().testTag("health_privacy_settings_card")
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AuraCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "HEALTH & FITNESS DATA",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Privacy controls & Google Health access",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (healthStatus) {
                                HealthConnectionStatus.CONNECTED -> AuraLime.copy(alpha = 0.15f)
                                HealthConnectionStatus.PERMISSION_DENIED -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = when (healthStatus) {
                                    HealthConnectionStatus.CONNECTED -> "CONNECTED"
                                    HealthConnectionStatus.PERMISSION_DENIED -> "DENIED"
                                    HealthConnectionStatus.UNAVAILABLE -> "UNAVAILABLE"
                                    else -> "DISCONNECTED"
                                },
                                color = when (healthStatus) {
                                    HealthConnectionStatus.CONNECTED -> AuraLime
                                    HealthConnectionStatus.PERMISSION_DENIED -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "UMR AURA only uses your fitness data to personalize your coaching experience. Your data is never sold or shared.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Connection status & authorized permissions
                    Text(
                        text = "AUTHORIZED PERMISSIONS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val permissions = listOf(
                            "Steps & Walking Activity" to (healthStatus == HealthConnectionStatus.CONNECTED),
                            "Distance & Active Movement" to (healthStatus == HealthConnectionStatus.CONNECTED),
                            "Active Calories Expended" to (healthStatus == HealthConnectionStatus.CONNECTED),
                            "Workout Sessions Telemetry" to (healthStatus == HealthConnectionStatus.CONNECTED),
                            "Heart Rate & Sleep Readiness" to (healthStatus == HealthConnectionStatus.CONNECTED)
                        )
                        permissions.forEach { (name, isGranted) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    text = if (isGranted) "Authorized" else "Not Authorized",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isGranted) AuraLime else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Last sync time
                    val lastSync = healthSettings?.lastSyncedTimestamp ?: 0L
                    val lastSyncFormatted = if (lastSync > 0) {
                        java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.US).format(java.util.Date(lastSync))
                    } else "Not synced yet"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Last Sync", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(lastSyncFormatted, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Option to PAUSE AI ACCESS TO FITNESS DATA
                    val allowAiAccess = healthSettings?.allowAiAccessToFitnessData ?: true
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI Fitness Data Access",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (allowAiAccess) "AURA uses authorized activity data to calibrate recommendations."
                                    else "AI access paused. AURA will not incorporate health telemetry.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = allowAiAccess,
                                onCheckedChange = { viewModel.setAiAccessToHealth(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AuraLime
                                ),
                                modifier = Modifier.testTag("pause_ai_access_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons
                    if (healthStatus == HealthConnectionStatus.CONNECTED) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.refreshHealthData() },
                                modifier = Modifier.weight(1f).testTag("sync_health_data_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SYNC NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showDisconnectHealthConfirm = true },
                                modifier = Modifier.weight(1f).testTag("disconnect_health_settings_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DISCONNECT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showDeleteHealthConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("delete_local_fitness_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DELETE LOCAL FITNESS DATA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { showConnectExplanationDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("connect_health_settings_button")
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CONNECT FITNESS DATA", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Contact Section & Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().testTag("contact_section_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "CONTACT UMR AURA",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Philosophy: LEAVE BETTER. Inquire about enterprise coaching, partnership, or custom programming.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("contact_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("contact_email_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Mobile Phone Number") },
                        placeholder = { Text("e.g. +1 555-0199") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("contact_phone_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contactMessage,
                        onValueChange = { contactMessage = it },
                        label = { Text("Message / Inquiry") },
                        modifier = Modifier.fillMaxWidth().testTag("contact_message_input"),
                        minLines = 3
                    )

                    if (contactError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = contactError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    if (contactSentSuccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AuraLime)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Message recorded! Our coaching team will reach out via $contactPhone or $contactEmail.",
                                color = AuraLime,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (contactName.isBlank() || contactEmail.isBlank() || contactMessage.isBlank()) {
                                contactError = "Please fill out name, email, and message."
                            } else {
                                contactError = null
                                contactSending = true
                                // Record message
                                contactSentSuccess = true
                                contactSending = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("submit_contact_button"),
                        enabled = !contactSending
                    ) {
                        if (contactSending) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SUBMIT INQUIRY", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Safety & Medical Disclaimer
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Medical Notice",
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "HEALTH & MEDICAL DISCLAIMER",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "UMR AURA and AURA AI Agent provide personalized exercise and nutritional guidance for informational and performance purposes only. AURA does not diagnose disease or prescribe medications. Consult a physician before starting any training program.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentProfile = currentProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updated ->
                viewModel.saveUserProfile(updated)
                showEditProfileDialog = false
            }
        )
    }

    // Add Memory Dialog
    if (showAddMemoryDialog) {
        AddMemoryDialog(
            onDismiss = { showAddMemoryDialog = false },
            onAdd = { cat, key, value ->
                viewModel.addMemory(cat, key, value)
                showAddMemoryDialog = false
            }
        )
    }

    // Reset Memory Confirmation Dialog
    if (showResetMemoryConfirm) {
        AlertDialog(
            onDismissRequest = { showResetMemoryConfirm = false },
            title = { Text("Reset AURA Memory?", fontWeight = FontWeight.Bold) },
            text = { Text("This will clear all stored goals, injury tags, and personal preferences from AURA's local database.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetMemories()
                        showResetMemoryConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("CLEAR ALL MEMORIES")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetMemoryConfirm = false }) { Text("CANCEL") }
            }
        )
    }

    // Delete Health Data Confirmation Dialog
    if (showDeleteHealthConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteHealthConfirm = false },
            title = { Text("Delete Local Fitness Data?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This action will permanently delete all cached step records, activity metrics, and health summaries from this device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLocalHealthData()
                        showDeleteHealthConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("DELETE DATA")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteHealthConfirm = false }) { Text("CANCEL") }
            }
        )
    }

    // Disconnect Health Confirmation Dialog
    if (showDisconnectHealthConfirm) {
        AlertDialog(
            onDismissRequest = { showDisconnectHealthConfirm = false },
            title = { Text("Disconnect Fitness Data?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Disconnecting will pause AURA's live activity coaching. Would you also like to delete cached local fitness records?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.disconnectHealth(clearLocalData = false)
                        showDisconnectHealthConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("DISCONNECT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectHealthConfirm = false }) { Text("CANCEL") }
            }
        )
    }

    // Connect Explanation Dialog (from Settings)
    if (showConnectExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showConnectExplanationDialog = false },
            title = { Text("Connect your fitness data to AURA", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AURA can use authorized activity information to:")
                    Text("• Understand your daily activity")
                    Text("• Track workouts & active duration")
                    Text("• Improve recommendations")
                    Text("• Analyze progress")
                    Text("• Help personalize your fitness plan")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConnectExplanationDialog = false
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black)
                ) {
                    Text("CONTINUE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectExplanationDialog = false }) { Text("NOT NOW") }
            }
        )
    }
}

@Composable
fun ProfileStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EditProfileDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf(currentProfile.name) }
    var ageStr by remember { mutableStateOf(currentProfile.age.toString()) }
    var heightStr by remember { mutableStateOf(currentProfile.heightCm.toInt().toString()) }
    var weightStr by remember { mutableStateOf(currentProfile.weightKg.toInt().toString()) }
    var goal by remember { mutableStateOf(currentProfile.fitnessGoal) }
    var experience by remember { mutableStateOf(currentProfile.experienceLevel) }
    var equipment by remember { mutableStateOf(currentProfile.equipment) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Athlete Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ageStr,
                        onValueChange = { ageStr = it },
                        label = { Text("Age") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightStr,
                        onValueChange = { heightStr = it },
                        label = { Text("Height (cm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text("Fitness Goal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    label = { Text("Available Equipment") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val age = ageStr.toIntOrNull() ?: currentProfile.age
                    val height = heightStr.toFloatOrNull() ?: currentProfile.heightCm
                    val weight = weightStr.toFloatOrNull() ?: currentProfile.weightKg
                    onSave(
                        currentProfile.copy(
                            name = name,
                            age = age,
                            heightCm = height,
                            weightKg = weight,
                            fitnessGoal = goal,
                            equipment = equipment
                        )
                    )
                }
            ) {
                Text("SAVE PROFILE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onAdd: (category: String, key: String, value: String) -> Unit
) {
    var category by remember { mutableStateOf("Goal") }
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    val categories = listOf("Goal", "Injury/Limitation", "Preference", "Habit", "Achievement")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add AURA Memory", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (category == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                color = if (category == cat) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Key / Title (e.g., Lower Back Caution)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Details (e.g., Avoid heavy axial loading on squats)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (key.isNotBlank() && value.isNotBlank()) {
                        onAdd(category, key, value)
                    }
                }
            ) {
                Text("STORE MEMORY")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
