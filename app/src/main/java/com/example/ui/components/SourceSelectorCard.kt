package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TelemetrySource
import com.example.data.model.VehicleTelemetry
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import java.util.Locale

@Composable
fun SourceSelectorCard(
    telemetry: VehicleTelemetry,
    onSourceSelected: (TelemetrySource) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("source_selector_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CockpitSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DATA SOURCE FOR SPEED & DISTANCE",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                // Active status pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (telemetry.isHardwareConnected) NeonEmerald else NeonCyan,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = if (telemetry.isHardwareConnected) "CAN Bus Live" else "Sensor Ready",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SourceOptionChip(
                    title = "Car Hardware",
                    subtitle = if (telemetry.isHardwareConnected) "Direct CAN Cluster" else "Default / Ready",
                    icon = Icons.Default.DirectionsCar,
                    isSelected = telemetry.activeSource == TelemetrySource.CAR_HARDWARE,
                    onClick = { onSourceSelected(TelemetrySource.CAR_HARDWARE) },
                    modifier = Modifier.weight(1f)
                )

                SourceOptionChip(
                    title = "GPS / Maps",
                    subtitle = String.format(Locale.US, "%.0f km/h", telemetry.gpsSpeedKmh),
                    icon = Icons.Default.LocationOn,
                    isSelected = telemetry.activeSource == TelemetrySource.GPS_LOCATION,
                    onClick = { onSourceSelected(TelemetrySource.GPS_LOCATION) },
                    modifier = Modifier.weight(1f)
                )

                SourceOptionChip(
                    title = "Simulator",
                    subtitle = "Physics Test",
                    icon = Icons.Default.Tune,
                    isSelected = telemetry.activeSource == TelemetrySource.SIMULATOR,
                    onClick = { onSourceSelected(TelemetrySource.SIMULATOR) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SourceOptionChip(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) NeonCyan.copy(alpha = 0.16f) else Color(0xFF162032)
    val borderColor = if (isSelected) NeonCyan else Color(0xFF243248)

    Column(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) NeonCyan else Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
        Text(
            text = subtitle,
            color = if (isSelected) NeonCyan else Color(0xFF64748B),
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1
        )
    }
}
