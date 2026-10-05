package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.AuraViewModel
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraLime
import com.example.ui.theme.AuraViolet

@Composable
fun CameraVisionScreen(
    viewModel: AuraViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var selectedMovement by remember { mutableStateOf("Squat") }
    var useFrontCamera by remember { mutableStateOf(false) }
    var repCount by remember { mutableIntStateOf(0) }

    val movementProtocols = mapOf(
        "Squat" to listOf(
            "Head neutral & chest tall",
            "Knees tracking parallel over mid-toes",
            "Femur breaking parallel plane",
            "Core intra-abdominal pressure locked"
        ),
        "Push-up" to listOf(
            "Shoulder-to-heel straight rigid plank",
            "Elbows angled 45 degrees, not flared",
            "Chest descending within 2 inches of ground",
            "Full scapular protraction at apex"
        ),
        "Deadlift" to listOf(
            "Bar stays in vertical line close to shins",
            "Hips hinge backward, neutral lumbar spine",
            "Lats locked down like squeezing oranges",
            "Avoid hyperextending at lockout"
        ),
        "Plank" to listOf(
            "Elbows directly stacked below shoulders",
            "Pelvis neutral (no sagging or piking)",
            "Glutes and quadriceps squeezed tight",
            "Rhythmic nasal diaphragmatic breathing"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080C14))
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AURA VISION",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                }
                Text(
                    text = "Kinematic form analysis & real-time feedback",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (hasCameraPermission) {
                IconButton(onClick = { useFrontCamera = !useFrontCamera }) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = AuraCyan
                    )
                }
            }
        }

        // Movement selector tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            movementProtocols.keys.forEach { movement ->
                val isSelected = selectedMovement == movement
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) AuraCyan else Color(0xFF1E293B),
                    modifier = Modifier.clickable {
                        selectedMovement = movement
                        repCount = 0
                    }
                ) {
                    Text(
                        text = movement.uppercase(),
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Camera Preview / Permission Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF111827))
                .border(1.dp, AuraCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                // Live CameraX View
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val cameraSelector = if (useFrontCamera)
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            else
                                CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview
                                )
                            } catch (e: Exception) {
                                // fallback
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )

                // Biomechanical Pose Overlay Guidelines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)

                    // Vertical Centerline (Spine Alignment)
                    drawLine(
                        color = AuraCyan.copy(alpha = 0.6f),
                        start = Offset(w * 0.5f, h * 0.1f),
                        end = Offset(w * 0.5f, h * 0.9f),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = dashEffect
                    )

                    // Target Depth Bounds
                    drawRect(
                        color = AuraLime.copy(alpha = 0.25f),
                        topLeft = Offset(w * 0.15f, h * 0.25f),
                        size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.55f),
                        style = Stroke(width = 2.dp.toPx(), pathEffect = dashEffect)
                    )

                    // Kinetic Nodes
                    drawCircle(color = AuraCyan, radius = 6.dp.toPx(), center = Offset(w * 0.5f, h * 0.22f))
                    drawCircle(color = AuraLime, radius = 6.dp.toPx(), center = Offset(w * 0.35f, h * 0.55f))
                    drawCircle(color = AuraLime, radius = 6.dp.toPx(), center = Offset(w * 0.65f, h * 0.55f))
                }

                // Rep counter overlay badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraLime),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ESTIMATED REPS: ",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$repCount",
                            color = AuraLime,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Manual rep adjustment buttons
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { repCount = (repCount - 1).coerceAtLeast(0) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Minus", tint = Color.White)
                    }
                    IconButton(
                        onClick = { repCount++ },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AuraLime)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Rep", tint = Color.Black)
                    }
                }
            } else {
                // Request Permission View
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = null,
                        tint = AuraCyan,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Camera Access Required",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Activate camera for real-time visual alignment, joint trajectory cues, and automated rep monitoring.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ENABLE CAMERA ASSISTANT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live Biomechanical Checklist Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AuraLime, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AURA FORM DIRECTIVES ($selectedMovement):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = AuraLime
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                movementProtocols[selectedMovement]?.forEach { directive ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AuraCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = directive,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Mandatory Safety Disclaimer (Prompt Requirement)
        Surface(
            color = Color(0xFF1C1917),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF78350F), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Safety Warning",
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI camera analysis is an estimate and is not a replacement for a qualified fitness professional or medical professional.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFDE68A)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
