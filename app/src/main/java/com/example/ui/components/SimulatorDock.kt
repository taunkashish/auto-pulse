package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DriveProfile
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRose
import java.util.Locale

@Composable
fun SimulatorDock(
    onThrottleChanged: (Float) -> Unit,
    onBrakingChanged: (Boolean) -> Unit,
    onProfileSelected: (DriveProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var throttleVal by remember { mutableFloatStateOf(0f) }
    var selectedProfile by remember { mutableStateOf(DriveProfile.MANUAL) }
    var isBrakingActive by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("simulator_dock"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
        border = BorderStroke(1.dp, CockpitSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VEHICLE PHYSICS SIMULATOR CONTROLS",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Throttle: ${throttleVal.toInt()}%",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Preset drive cycle chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProfileChip(
                    profile = DriveProfile.CITY_STOP_GO,
                    label = "Urban Stop",
                    isSelected = selectedProfile == DriveProfile.CITY_STOP_GO,
                    onClick = {
                        selectedProfile = DriveProfile.CITY_STOP_GO
                        onProfileSelected(DriveProfile.CITY_STOP_GO)
                    },
                    modifier = Modifier.weight(1f)
                )
                ProfileChip(
                    profile = DriveProfile.HIGHWAY_CRUISE,
                    label = "Highway 110",
                    isSelected = selectedProfile == DriveProfile.HIGHWAY_CRUISE,
                    onClick = {
                        selectedProfile = DriveProfile.HIGHWAY_CRUISE
                        onProfileSelected(DriveProfile.HIGHWAY_CRUISE)
                    },
                    modifier = Modifier.weight(1f)
                )
                ProfileChip(
                    profile = DriveProfile.MOUNTAIN_PASS,
                    label = "Mountain",
                    isSelected = selectedProfile == DriveProfile.MOUNTAIN_PASS,
                    onClick = {
                        selectedProfile = DriveProfile.MOUNTAIN_PASS
                        onProfileSelected(DriveProfile.MOUNTAIN_PASS)
                    },
                    modifier = Modifier.weight(1f)
                )
                ProfileChip(
                    profile = DriveProfile.MANUAL,
                    label = "Manual",
                    isSelected = selectedProfile == DriveProfile.MANUAL,
                    onClick = {
                        selectedProfile = DriveProfile.MANUAL
                        onProfileSelected(DriveProfile.MANUAL)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Throttle Slider and Brake Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = throttleVal,
                        onValueChange = {
                            throttleVal = it
                            selectedProfile = DriveProfile.MANUAL
                            onThrottleChanged(it)
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("throttle_slider")
                    )
                }

                Button(
                    onClick = {
                        isBrakingActive = !isBrakingActive
                        onBrakingChanged(isBrakingActive)
                        if (isBrakingActive) throttleVal = 0f
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBrakingActive) NeonRose else Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("brake_button")
                ) {
                    Text(
                        text = if (isBrakingActive) "BRAKE (ON)" else "BRAKE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileChip(
    profile: DriveProfile,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0xFF162032)
    val border = if (isSelected) NeonCyan else Color(0xFF243248)

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(8.dp))
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
