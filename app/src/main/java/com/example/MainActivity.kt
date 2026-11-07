package com.example

import android.Manifest
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.cloud.CloudSyncCoordinator
import com.example.data.cloud.UserProfile
import com.example.data.telemetry.TelemetryRepository
import com.example.ui.components.DigitalSpeedometer
import com.example.ui.components.MetricTile
import com.example.ui.components.SimulatorDock
import com.example.ui.components.SourceSelectorCard
import com.example.ui.components.TripRecorderCard
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TripsScreen
import com.example.ui.theme.AutoPulseTheme
import com.example.ui.theme.CockpitBackground
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import kotlinx.coroutines.launch
import java.util.Locale

enum class AppNavScreen(val label: String) {
    COCKPIT("Cockpit"),
    TRIPS("Trips"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AutoPulseTheme {
                AutoPulseApp()
            }
        }
    }
}

@Composable
fun AutoPulseApp() {
    val context = LocalContext.current
    val repository = remember { TelemetryRepository.getInstance(context) }
    val syncCoordinator = remember { CloudSyncCoordinator(context) }
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(AppNavScreen.COCKPIT) }

    // Location permissions launcher for GPS Ground Speed
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* GPS source auto-adjusts based on permission status */ }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val telemetry by repository.telemetry.collectAsState()
    val activeTrip by repository.activeTrip.collectAsState()
    val isRecordingTrip by repository.isRecordingTrip.collectAsState()
    val allTrips by repository.observeAllTrips().collectAsState(initial = emptyList())

    val userProfile by syncCoordinator.authManager.currentUser.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBackground),
        containerColor = CockpitBackground,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar"),
                containerColor = CockpitSurface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppNavScreen.COCKPIT,
                    onClick = { currentScreen = AppNavScreen.COCKPIT },
                    icon = {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = "Cockpit")
                    },
                    label = { Text("Cockpit", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF003840),
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_cockpit")
                )

                NavigationBarItem(
                    selected = currentScreen == AppNavScreen.TRIPS,
                    onClick = { currentScreen = AppNavScreen.TRIPS },
                    icon = {
                        Icon(imageVector = Icons.Default.Route, contentDescription = "History")
                    },
                    label = { Text("History", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF003840),
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_trips")
                )

                NavigationBarItem(
                    selected = currentScreen == AppNavScreen.SETTINGS,
                    onClick = { currentScreen = AppNavScreen.SETTINGS },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    },
                    label = { Text("Settings", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF003840),
                        selectedTextColor = NeonCyan,
                        indicatorColor = NeonCyan,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            AppNavScreen.COCKPIT -> {
                CockpitContent(
                    innerPadding = innerPadding,
                    telemetry = telemetry,
                    activeTrip = activeTrip,
                    isRecordingTrip = isRecordingTrip,
                    allTripsCount = allTrips.size,
                    userProfile = userProfile,
                    onSourceSelected = { repository.setSource(it) },
                    onStartTrip = {
                        val title = "Trip #${allTrips.size + 1}"
                        val uid = userProfile?.id ?: ""
                        repository.startTrip(title = title, userId = uid)
                    },
                    onPauseTrip = { repository.pauseTrip() },
                    onResumeTrip = { repository.resumeTrip() },
                    onFinishTrip = {
                        scope.launch {
                            val finished = repository.finishTrip()
                            if (finished != null) {
                                syncCoordinator.syncTripInBackground(finished)
                            }
                        }
                    },
                    onThrottleChanged = { repository.setThrottle(it) },
                    onBrakingChanged = { repository.setBraking(it) },
                    onProfileSelected = { repository.setDriveProfile(it) },
                    onSignIn = { syncCoordinator.authManager.signIn(it) },
                    onOpenProfile = { currentScreen = AppNavScreen.SETTINGS }
                )
            }

            AppNavScreen.TRIPS -> {
                TripsScreen(
                    trips = allTrips,
                    onDeleteTrip = { trip ->
                        scope.launch {
                            repository.deleteTrip(trip)
                        }
                    },
                    onNavigateBack = { currentScreen = AppNavScreen.COCKPIT },
                    modifier = Modifier
                        .padding(innerPadding)
                        .windowInsetsPadding(WindowInsets.statusBars)
                )
            }

            AppNavScreen.SETTINGS -> {
                SettingsScreen(
                    syncCoordinator = syncCoordinator,
                    onNavigateBack = { currentScreen = AppNavScreen.COCKPIT },
                    modifier = Modifier
                        .padding(innerPadding)
                        .windowInsetsPadding(WindowInsets.statusBars)
                )
            }
        }
    }
}

@Composable
private fun CockpitContent(
    innerPadding: PaddingValues,
    telemetry: com.example.data.model.VehicleTelemetry,
    activeTrip: com.example.data.model.TripData?,
    isRecordingTrip: Boolean,
    allTripsCount: Int,
    userProfile: UserProfile?,
    onSourceSelected: (com.example.data.model.TelemetrySource) -> Unit,
    onStartTrip: () -> Unit,
    onPauseTrip: () -> Unit,
    onResumeTrip: () -> Unit,
    onFinishTrip: () -> Unit,
    onThrottleChanged: (Float) -> Unit,
    onBrakingChanged: (Boolean) -> Unit,
    onProfileSelected: (com.example.data.model.DriveProfile) -> Unit,
    onSignIn: (Activity) -> Unit,
    onOpenProfile: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar / Brand Header with Sign In only (redundant buttons removed)
        item {
            CockpitHeader(
                isCarConnected = telemetry.isHardwareConnected,
                userProfile = userProfile,
                onSignIn = { (context as? Activity)?.let { onSignIn(it) } },
                onOpenProfile = onOpenProfile
            )
        }

        // Digital Speedometer (Live Cockpit Dial)
        item {
            DigitalSpeedometer(telemetry = telemetry)
        }

        // Source Switcher (Car Hardware vs GPS / Google Maps vs Simulator)
        item {
            SourceSelectorCard(
                telemetry = telemetry,
                onSourceSelected = onSourceSelected
            )
        }

        // Real-Time Diagnostic Metric Tiles (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricTile(
                        title = "Odometer Mileage",
                        value = String.format(Locale.US, "%.1f", telemetry.odometerKm),
                        unit = "km",
                        icon = Icons.Default.Speed,
                        iconTint = NeonCyan,
                        subtext = "Total vehicle distance",
                        modifier = Modifier.weight(1f)
                    )

                    MetricTile(
                        title = "Energy Level & Range",
                        value = String.format(Locale.US, "%.0f", telemetry.fuelLevelPercent),
                        unit = "%",
                        icon = Icons.Default.BatteryChargingFull,
                        iconTint = if (telemetry.isFuelLow) NeonRose else NeonEmerald,
                        progress = telemetry.fuelLevelPercent / 100f,
                        progressColor = if (telemetry.isFuelLow) NeonRose else NeonEmerald,
                        subtext = "Range: ${String.format(Locale.US, "%.0f km", telemetry.rangeRemainingKm)}",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricTile(
                        title = "Coolant & Voltage",
                        value = String.format(Locale.US, "%.0f", telemetry.coolantTempC),
                        unit = "°C",
                        icon = Icons.Default.Thermostat,
                        iconTint = NeonAmber,
                        subtext = "Battery: ${String.format(Locale.US, "%.1fV", telemetry.batteryVoltage)}",
                        modifier = Modifier.weight(1f)
                    )

                    MetricTile(
                        title = "Compass Heading",
                        value = telemetry.compassDirection,
                        unit = String.format(Locale.US, "(%.0f°)", telemetry.compassDegrees),
                        icon = Icons.Default.Explore,
                        iconTint = NeonCyan,
                        subtext = "Display Speed: ${String.format(Locale.US, "%.0f km/h", telemetry.displaySpeedKmh)}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Trip Performance Recorder Bar
        item {
            TripRecorderCard(
                activeTrip = activeTrip,
                isRecording = isRecordingTrip,
                onStartTrip = onStartTrip,
                onPauseTrip = onPauseTrip,
                onResumeTrip = onResumeTrip,
                onFinishTrip = onFinishTrip
            )
        }

        // Interactive Physics Simulator Controls (Throttle & Brake)
        item {
            SimulatorDock(
                onThrottleChanged = onThrottleChanged,
                onBrakingChanged = onBrakingChanged,
                onProfileSelected = onProfileSelected
            )
        }
    }
}

@Composable
private fun CockpitHeader(
    isCarConnected: Boolean,
    userProfile: UserProfile?,
    onSignIn: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Brand Logo & Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(NeonCyan.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_autopulse_logo),
                    contentDescription = "AutoPulse Logo",
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "AUTOPULSE",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (isCarConnected) NeonEmerald else Color(0xFF94A3B8),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCarConnected) "Auto Connected" else "Auto Ready",
                        color = if (isCarConnected) NeonEmerald else Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Top-right action: Sign-In / Account status button
        if (userProfile != null) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { onOpenProfile() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("header_profile_chip"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = userProfile.displayName.split(" ").firstOrNull() ?: "Driver",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("header_login_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Sign In",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sign In",
                    color = Color(0xFF0F172A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
