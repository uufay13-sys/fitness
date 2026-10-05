package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraLime

@Composable
fun DailyCheckInDialog(
    onDismiss: () -> Unit,
    onSubmit: (energy: Int, sleep: Float, recovery: String, mood: String, notes: String) -> Unit
) {
    var energy by remember { mutableIntStateOf(4) }
    var sleepHoursStr by remember { mutableStateOf("7.5") }
    var recovery by remember { mutableStateOf("Fresh") }
    var mood by remember { mutableStateOf("Energized") }
    var notes by remember { mutableStateOf("") }

    val recoveryOptions = listOf("Fresh", "Moderate", "Sore", "Exhausted")
    val moodOptions = listOf("Energized", "Good", "Neutral", "Tired")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = AuraLime)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Daily Readiness Check-In", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "How are you feeling today?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Energy 1-5
                Text("Energy Level (1 to 5):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 1..5) {
                        val isSelected = energy == i
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AuraLime else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { energy = i }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.Black else Color.Gray,
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                                Text(
                                    text = "$i",
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Sleep hours
                OutlinedTextField(
                    value = sleepHoursStr,
                    onValueChange = { sleepHoursStr = it },
                    label = { Text("Sleep Duration (Hours)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Recovery feel
                Text("Muscle Recovery State:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    recoveryOptions.forEach { opt ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (recovery == opt) AuraCyan else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { recovery = opt }
                        ) {
                            Text(
                                text = opt,
                                color = if (recovery == opt) Color.Black else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // General mood
                Text("General Mental State:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    moodOptions.forEach { m ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (mood == m) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { mood = m }
                        ) {
                            Text(
                                text = m,
                                color = if (mood == m) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Any pain or notes for AURA?") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sleep = sleepHoursStr.toFloatOrNull() ?: 7.0f
                    onSubmit(energy, sleep, recovery, mood, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("RECORD CHECK-IN", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}
