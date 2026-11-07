package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TripData
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import java.util.Locale

@Composable
fun TripRecorderCard(
    activeTrip: TripData?,
    isRecording: Boolean,
    onStartTrip: () -> Unit,
    onPauseTrip: () -> Unit,
    onResumeTrip: () -> Unit,
    onFinishTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trip_recorder_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CockpitSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with pulsing recording badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isRecording) NeonRose else Color(0xFF64748B),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (isRecording) "RECORDING ACTIVE TRIP" else if (activeTrip != null) "TRIP PAUSED" else "TRIP PERFORMANCE LOGGER",
                        color = if (isRecording) NeonRose else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (activeTrip != null) {
                    Text(
                        text = activeTrip.formattedDuration,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats grid during trip
            if (activeTrip != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TripStatColumn(
                        label = "DISTANCE",
                        value = String.format(Locale.US, "%.2f", activeTrip.totalDistanceKm),
                        unit = "km"
                    )
                    TripStatColumn(
                        label = "AVG SPEED",
                        value = String.format(Locale.US, "%.1f", activeTrip.averageSpeedKmh),
                        unit = "km/h"
                    )
                    TripStatColumn(
                        label = "TOP SPEED",
                        value = String.format(Locale.US, "%.0f", activeTrip.maxSpeedKmh),
                        unit = "km/h"
                    )
                    TripStatColumn(
                        label = "EST. FUEL",
                        value = String.format(Locale.US, "%.2f", activeTrip.estimatedFuelLiters),
                        unit = "L"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeTrip == null) {
                    Button(
                        onClick = onStartTrip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_trip_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = Color(0xFF003840),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "START NEW TRIP",
                            color = Color(0xFF003840),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    if (isRecording) {
                        OutlinedButton(
                            onClick = onPauseTrip,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pause_trip_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Pause", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = onResumeTrip,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("resume_trip_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Resume", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onFinishTrip,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("finish_trip_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Finish & Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TripStatColumn(label: String, value: String, unit: String) {
    Column {
        Text(text = label, color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = " $unit",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}
