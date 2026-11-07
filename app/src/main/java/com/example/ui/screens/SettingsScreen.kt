package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.cloud.CloudSyncCoordinator
import com.example.data.cloud.ServiceSyncState
import com.example.data.cloud.UserProfile
import com.example.ui.theme.CockpitBackground
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceBorder
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    syncCoordinator: CloudSyncCoordinator,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val userProfile by syncCoordinator.authManager.currentUser.collectAsState()
    val isFirestoreEnabled by syncCoordinator.isFirestoreEnabled.collectAsState()
    val isDriveEnabled by syncCoordinator.isDriveEnabled.collectAsState()
    val isAutoSyncEnabled by syncCoordinator.isAutoSyncEnabled.collectAsState()
    val firestoreStatus by syncCoordinator.firestoreStatus.collectAsState()
    val driveStatus by syncCoordinator.driveStatus.collectAsState()
    val isSyncing by syncCoordinator.isSyncing.collectAsState()
    val lastSummary by syncCoordinator.lastSyncSummary.collectAsState()

    var syncFeedbackMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBackground)
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Cloud Synchronization Settings",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage Firebase Firestore & Google Drive services",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 1. Google Account Authentication Card
        item {
            GoogleAccountSettingsCard(
                userProfile = userProfile,
                onSignIn = { (context as? Activity)?.let { syncCoordinator.authManager.signIn(it) } },
                onSignOut = { syncCoordinator.authManager.signOut() }
            )
        }

        // Section Title: Individual Services
        item {
            Text(
                text = "INDIVIDUAL CLOUD SERVICES",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // 2. Firebase Firestore Database Service Card
        item {
            ServiceStatusCard(
                serviceName = "Firebase Firestore Database",
                serviceDescription = "Real-time NoSQL cloud database for instant multi-device trip query and analytics.",
                serviceIcon = Icons.Default.Storage,
                isEnabled = isFirestoreEnabled,
                statusState = firestoreStatus,
                accentColor = NeonCyan,
                onToggle = { syncCoordinator.toggleFirestore(it) },
                testTagPrefix = "firestore"
            )
        }

        // 3. Google Drive Cloud Storage Service Card
        item {
            ServiceStatusCard(
                serviceName = "Google Drive Cloud Storage",
                serviceDescription = "Exports structured JSON payloads and CSV datasets formatted for Google Sheets and spreadsheets.",
                serviceIcon = Icons.Default.CloudQueue,
                isEnabled = isDriveEnabled,
                statusState = driveStatus,
                accentColor = NeonEmerald,
                onToggle = { syncCoordinator.toggleDrive(it) },
                testTagPrefix = "google_drive"
            )
        }

        // Section Title: Sync Automation & Actions
        item {
            Text(
                text = "SYNC POLICIES & MANUAL CONTROLS",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // 4. Auto-Sync and Manual Trigger Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                border = BorderStroke(1.dp, CockpitSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Auto-sync toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Sync on Trip Completion",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Automatically upload trip summaries when finishing a driving session",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isAutoSyncEnabled,
                            onCheckedChange = { syncCoordinator.toggleAutoSync(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("auto_sync_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sync All Unsynced Trips Now
                    Button(
                        onClick = {
                            syncCoordinator.syncAllUnsyncedNow { count ->
                                syncFeedbackMessage = "Completed sync check: $count unsynced session(s) processed."
                            }
                        },
                        enabled = !isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sync_all_now_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF003840),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Synchronizing...", color = Color(0xFF003840), fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = Color(0xFF003840),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync All Unsynced Trips Now", color = Color(0xFF003840), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (syncFeedbackMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = syncFeedbackMessage ?: "",
                            color = NeonEmerald,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 5. Architecture & Non-Blocking Info Note
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Decoupled Cloud Architecture",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "AutoPulse executes Firebase Firestore and Google Drive synchronizations in separate asynchronous tasks. If one cloud provider is offline, rate-limited, or unconfigured, the other service continues without delay or data loss.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceStatusCard(
    serviceName: String,
    serviceDescription: String,
    serviceIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean,
    statusState: ServiceSyncState,
    accentColor: Color,
    onToggle: (Boolean) -> Unit,
    testTagPrefix: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
        border = BorderStroke(1.dp, if (isEnabled) accentColor.copy(alpha = 0.3f) else CockpitSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Service Title and Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = serviceIcon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = serviceName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEnabled) "Active Sync Enabled" else "Sync Disabled",
                            color = if (isEnabled) accentColor else Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentColor.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("${testTagPrefix}_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = serviceDescription,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time Status Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        when {
                                            !isEnabled -> Color(0xFF64748B)
                                            statusState.isHealthy -> NeonEmerald
                                            else -> NeonAmber
                                        },
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "STATUS: ${statusState.statusText}",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Text(
                            text = "${statusState.totalSyncedCount} synced",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (statusState.lastSyncTimeMs != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val formattedTime = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(statusState.lastSyncTimeMs))
                        Text(
                            text = "Last synced: $formattedTime",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleAccountSettingsCard(
    userProfile: UserProfile?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
        border = BorderStroke(1.dp, CockpitSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AUTHENTICATION & PROFILE",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (userProfile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(NeonCyan.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = userProfile.displayName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userProfile.email,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Authenticated with Google",
                                color = NeonEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Sign Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Guest Driver Mode (Offline)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Trips are stored locally in Room SQLite database. Sign in with Google to enable cloud sync.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onSignIn,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.height(42.dp).testTag("settings_google_sign_in_button")
                    ) {
                        Text("Sign In", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
