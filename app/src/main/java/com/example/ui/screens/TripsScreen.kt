package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.cloud.DriveExportHelper
import com.example.data.model.TripData
import com.example.ui.theme.CockpitBackground
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import java.util.Locale

@Composable
fun TripsScreen(
    trips: List<TripData>,
    onDeleteTrip: (TripData) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current

    val totalDistance = trips.sumOf { it.totalDistanceKm }
    val totalSeconds = trips.sumOf { it.durationSeconds }
    val avgSpeed = if (trips.isNotEmpty()) trips.map { it.averageSpeedKmh }.average().toFloat() else 0f

    val totalHours = totalSeconds / 3600
    val totalMinutes = (totalSeconds % 3600) / 60
    val formattedTotalTime = if (totalHours > 0) "${totalHours}h ${totalMinutes}m" else "${totalMinutes}m"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBackground)
            .padding(16.dp)
            .testTag("trips_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Driving Sessions & History",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${trips.size} total recorded trip sessions in Room database",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Aggregate Summary Metrics Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                border = BorderStroke(1.dp, CockpitSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LIFETIME TELEMETRY AGGREGATES",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AggregateItem(
                            label = "DISTANCE",
                            value = String.format(Locale.US, "%.1f", totalDistance),
                            unit = "km",
                            icon = Icons.Default.Route,
                            tint = NeonCyan
                        )
                        AggregateItem(
                            label = "DURATION",
                            value = formattedTotalTime,
                            unit = "",
                            icon = Icons.Default.Timer,
                            tint = NeonEmerald
                        )
                        AggregateItem(
                            label = "AVG SPEED",
                            value = String.format(Locale.US, "%.0f", avgSpeed),
                            unit = "km/h",
                            icon = Icons.Default.Speed,
                            tint = Color(0xFF818CF8)
                        )
                        AggregateItem(
                            label = "SESSIONS",
                            value = "${trips.size}",
                            unit = "",
                            icon = Icons.Default.DirectionsCar,
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "PAST DRIVING SESSIONS",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (trips.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No past driving sessions found",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Record a trip from the cockpit to populate your driving history.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(trips, key = { it.id }) { trip ->
                TripDetailCard(
                    trip = trip,
                    onExportJson = {
                        val intent = DriveExportHelper.exportToJson(context, trip)
                        context.startActivity(intent)
                    },
                    onExportCsv = {
                        val intent = DriveExportHelper.exportToCsv(context, trip)
                        context.startActivity(intent)
                    },
                    onDelete = { onDeleteTrip(trip) }
                )
            }
        }
    }
}

@Composable
private fun AggregateItem(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    tint: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun TripDetailCard(
    trip: TripData,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
        border = BorderStroke(1.dp, CockpitSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trip.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = trip.formattedDate,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metric Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "TOTAL DISTANCE", color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.US, "%.2f km", trip.totalDistanceKm),
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Text(text = "DURATION", color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = trip.formattedDuration,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Text(text = "AVERAGE SPEED", color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.US, "%.1f km/h", trip.averageSpeedKmh),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Text(text = "TOP SPEED", color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.US, "%.0f km/h", trip.maxSpeedKmh),
                        color = NeonEmerald,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges row: Source and Cloud sync status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "SOURCE: ${trip.primarySource.label.uppercase()}",
                        color = Color(0xFFCBD5E1),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Firestore badge
                Box(
                    modifier = Modifier
                        .background(
                            if (trip.isSyncedFirestore) NeonCyan.copy(alpha = 0.15f) else Color(0xFF1E293B),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (trip.isSyncedFirestore) "FIRESTORE: SYNCED" else "FIRESTORE: LOCAL ONLY",
                        color = if (trip.isSyncedFirestore) NeonCyan else Color(0xFF64748B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Export Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onExportJson,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Drive JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonEmerald),
                    border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Drive CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Trip",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
