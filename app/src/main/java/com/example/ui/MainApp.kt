package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.ActiveWorkoutScreen
import com.example.ui.screens.AuraAdsScreen
import com.example.ui.screens.AuraAgentScreen
import com.example.ui.screens.CameraVisionScreen
import com.example.ui.screens.DailyCheckInDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NutritionScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.WorkoutScreen
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraLime

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Workouts : Screen("workouts", "Workout", Icons.Default.FitnessCenter)
    object AuraAgent : Screen("aura_agent", "AURA AI", Icons.Default.AutoAwesome)
    object AuraAds : Screen("aura_ads", "Ads Agent", Icons.Default.Campaign)
    object Progress : Screen("progress", "Progress", Icons.Default.Analytics)
    object Nutrition : Screen("nutrition", "Nutrition", Icons.Default.Restaurant)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object ActiveWorkout : Screen("active_workout", "Active", Icons.Default.FitnessCenter)
    object CameraVision : Screen("camera_vision", "Vision", Icons.Default.Videocam)
}

@Composable
fun MainApp(viewModel: AuraViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showCheckInDialog by remember { mutableStateOf(false) }

    val userProfile by viewModel.userProfile.collectAsState()
    val activeSession by viewModel.activeWorkout.collectAsState()

    // Listen for UI events (Toast, NavigateTo)
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is AuraUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is AuraUiEvent.NavigateTo -> {
                    when (event.route) {
                        "active_workout" -> currentScreen = Screen.ActiveWorkout
                        "workouts" -> currentScreen = Screen.Workouts
                        "progress" -> currentScreen = Screen.Progress
                        "aura_agent" -> currentScreen = Screen.AuraAgent
                        "aura_ads" -> currentScreen = Screen.AuraAds
                        "camera_vision" -> currentScreen = Screen.CameraVision
                    }
                }
                else -> {}
            }
        }
    }

    // Back button handling
    BackHandler(enabled = currentScreen != Screen.Home) {
        when (currentScreen) {
            Screen.ActiveWorkout -> {
                // Keep active session in background or show dialog
                currentScreen = Screen.Workouts
            }
            Screen.CameraVision -> currentScreen = Screen.Home
            Screen.AuraAds -> currentScreen = Screen.Home
            else -> currentScreen = Screen.Home
        }
    }

    // Onboarding check
    if (userProfile != null && !userProfile!!.isOnboarded) {
        OnboardingScreen(
            onComplete = { profile ->
                viewModel.saveUserProfile(profile)
            }
        )
        return
    }

    val navItems = listOf(
        Screen.Home,
        Screen.AuraAds,
        Screen.AuraAgent,
        Screen.Workouts,
        Screen.Progress,
        Screen.Profile
    )

    val showBottomBar = currentScreen != Screen.ActiveWorkout && currentScreen != Screen.CameraVision

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.testTag("main_navigation_bar")
                ) {
                    navItems.forEach { screen ->
                        val isSelected = currentScreen == screen
                        val isAura = screen == Screen.AuraAgent
                        val isAds = screen == Screen.AuraAds

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            icon = {
                                if (isAura || isAds) {
                                    val iconBg = if (isAds) {
                                        if (isSelected) AuraLime else AuraLime.copy(alpha = 0.2f)
                                    } else {
                                        if (isSelected) AuraCyan else AuraCyan.copy(alpha = 0.2f)
                                    }
                                    val iconTint = if (isSelected) Color.Black else if (isAds) AuraLime else AuraCyan
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(iconBg),
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title,
                                            tint = iconTint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = if (isAura) AuraCyan.copy(alpha = 0.2f) else if (isAds) AuraLime.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAgent = { currentScreen = Screen.AuraAgent },
                    onNavigateToAds = { currentScreen = Screen.AuraAds },
                    onNavigateToWorkouts = { currentScreen = Screen.Workouts },
                    onNavigateToProgress = { currentScreen = Screen.Progress },
                    onNavigateToNutrition = { currentScreen = Screen.Nutrition },
                    onNavigateToCamera = { currentScreen = Screen.CameraVision },
                    onStartActiveWorkout = { currentScreen = Screen.ActiveWorkout },
                    onOpenCheckIn = { showCheckInDialog = true }
                )
                Screen.AuraAds -> AuraAdsScreen(
                    viewModel = viewModel
                )
                Screen.Workouts -> WorkoutScreen(
                    viewModel = viewModel,
                    onStartWorkout = { currentScreen = Screen.ActiveWorkout },
                    onNavigateToAgent = { currentScreen = Screen.AuraAgent }
                )
                Screen.AuraAgent -> AuraAgentScreen(
                    viewModel = viewModel,
                    onNavigateToWorkouts = { currentScreen = Screen.Workouts },
                    onNavigateToProgress = { currentScreen = Screen.Progress },
                    onStartActiveWorkout = { currentScreen = Screen.ActiveWorkout },
                    onNavigateToAds = { currentScreen = Screen.AuraAds }
                )
                Screen.Progress -> ProgressScreen(
                    viewModel = viewModel,
                    onNavigateToAgent = { currentScreen = Screen.AuraAgent }
                )
                Screen.Nutrition -> NutritionScreen(
                    viewModel = viewModel,
                    onNavigateToAgent = { currentScreen = Screen.AuraAgent }
                )
                Screen.Profile -> ProfileScreen(
                    viewModel = viewModel
                )
                Screen.ActiveWorkout -> ActiveWorkoutScreen(
                    viewModel = viewModel,
                    onFinishAndExit = { currentScreen = Screen.Progress }
                )
                Screen.CameraVision -> CameraVisionScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
            }
        }
    }

    if (showCheckInDialog) {
        DailyCheckInDialog(
            onDismiss = { showCheckInDialog = false },
            onSubmit = { energy, sleep, recovery, mood, notes ->
                viewModel.submitDailyCheckIn(energy, sleep, recovery, mood, notes)
                showCheckInDialog = false
            }
        )
    }
}
