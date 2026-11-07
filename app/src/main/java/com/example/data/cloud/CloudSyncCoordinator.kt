package com.example.data.cloud

import android.content.Context
import com.example.data.local.AutoPulseDatabase
import com.example.data.model.TripData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class ServiceSyncState(
    val isEnabled: Boolean = true,
    val statusText: String = "Ready / Standby",
    val lastSyncTimeMs: Long? = null,
    val isHealthy: Boolean = true,
    val totalSyncedCount: Int = 0
)

class CloudSyncCoordinator(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val firestoreSyncManager = FirestoreSyncManager(context)
    private val tripDao = AutoPulseDatabase.getInstance(context).tripDao()

    val authManager = GoogleAuthManager(context)

    private val _isFirestoreEnabled = MutableStateFlow(true)
    val isFirestoreEnabled: StateFlow<Boolean> = _isFirestoreEnabled.asStateFlow()

    private val _isDriveEnabled = MutableStateFlow(true)
    val isDriveEnabled: StateFlow<Boolean> = _isDriveEnabled.asStateFlow()

    private val _isAutoSyncEnabled = MutableStateFlow(true)
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    private val _firestoreStatus = MutableStateFlow(ServiceSyncState(isEnabled = true, statusText = "Ready for Firestore cloud sync"))
    val firestoreStatus: StateFlow<ServiceSyncState> = _firestoreStatus.asStateFlow()

    private val _driveStatus = MutableStateFlow(ServiceSyncState(isEnabled = true, statusText = "Ready for Google Drive export"))
    val driveStatus: StateFlow<ServiceSyncState> = _driveStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncSummary = MutableStateFlow("All enabled cloud services are active")
    val lastSyncSummary: StateFlow<String> = _lastSyncSummary.asStateFlow()

    fun toggleFirestore(enabled: Boolean) {
        _isFirestoreEnabled.value = enabled
        _firestoreStatus.value = _firestoreStatus.value.copy(
            isEnabled = enabled,
            statusText = if (enabled) "Enabled (Ready)" else "Disabled by user"
        )
    }

    fun toggleDrive(enabled: Boolean) {
        _isDriveEnabled.value = enabled
        _driveStatus.value = _driveStatus.value.copy(
            isEnabled = enabled,
            statusText = if (enabled) "Enabled (Ready)" else "Disabled by user"
        )
    }

    fun toggleAutoSync(enabled: Boolean) {
        _isAutoSyncEnabled.value = enabled
    }

    /**
     * Executes non-blocking background sync for any enabled cloud database.
     * Failure in Firestore does not block Drive, and vice versa.
     */
    fun syncTripInBackground(trip: TripData) {
        if (!_isAutoSyncEnabled.value) return

        scope.launch {
            _isSyncing.value = true
            _lastSyncSummary.value = "Syncing ${trip.title}..."

            val user = authManager.currentUser.value
            val userId = user?.id ?: "local_driver"

            // 1. Firebase Firestore sync task
            val firestoreJob = async {
                if (_isFirestoreEnabled.value) {
                    _firestoreStatus.value = _firestoreStatus.value.copy(statusText = "Syncing to Firestore collection...")
                    val result = firestoreSyncManager.syncTrip(userId, trip)
                    if (result.isSuccess) {
                        try {
                            val entity = tripDao.getTripById(trip.id)
                            if (entity != null) {
                                tripDao.updateTrip(entity.copy(isSyncedFirestore = true))
                            }
                        } catch (e: Exception) { /* local update ignore */ }

                        _firestoreStatus.value = _firestoreStatus.value.copy(
                            statusText = "Synchronized to Firestore (/users/$userId/trips)",
                            lastSyncTimeMs = System.currentTimeMillis(),
                            isHealthy = true,
                            totalSyncedCount = _firestoreStatus.value.totalSyncedCount + 1
                        )
                        "Firestore: Synced"
                    } else {
                        val errMsg = result.exceptionOrNull()?.localizedMessage ?: "Not provisioned"
                        _firestoreStatus.value = _firestoreStatus.value.copy(
                            statusText = "Standby / Local cache ($errMsg)",
                            isHealthy = false
                        )
                        "Firestore: Standby ($errMsg)"
                    }
                } else {
                    _firestoreStatus.value = _firestoreStatus.value.copy(statusText = "Disabled by user")
                    "Firestore: Disabled"
                }
            }

            // 2. Google Drive export task
            val driveJob = async {
                if (_isDriveEnabled.value) {
                    _driveStatus.value = _driveStatus.value.copy(statusText = "Generating Drive JSON/CSV exports...")
                    try {
                        val exportDir = File(context.filesDir, "drive_exports").apply { mkdirs() }
                        val file = File(exportDir, "${trip.id}.json")
                        FileOutputStream(file).use { out ->
                            out.write(trip.toJsonString().toByteArray())
                        }
                        val entity = tripDao.getTripById(trip.id)
                        if (entity != null) {
                            tripDao.updateTrip(entity.copy(isExportedDrive = true))
                        }
                        _driveStatus.value = _driveStatus.value.copy(
                            statusText = "Drive package ready (${trip.id.take(8)}.json)",
                            lastSyncTimeMs = System.currentTimeMillis(),
                            isHealthy = true,
                            totalSyncedCount = _driveStatus.value.totalSyncedCount + 1
                        )
                        "Google Drive: Ready"
                    } catch (e: Exception) {
                        _driveStatus.value = _driveStatus.value.copy(
                            statusText = "Drive export error: ${e.message}",
                            isHealthy = false
                        )
                        "Google Drive: Error"
                    }
                } else {
                    _driveStatus.value = _driveStatus.value.copy(statusText = "Disabled by user")
                    "Google Drive: Disabled"
                }
            }

            val fResult = firestoreJob.await()
            val dResult = driveJob.await()

            _isSyncing.value = false
            _lastSyncSummary.value = "$fResult | $dResult"
        }
    }

    /**
     * Manually triggers sync for all trips in Room that have not been synced yet.
     */
    fun syncAllUnsyncedNow(onComplete: (Int) -> Unit = {}) {
        scope.launch {
            _isSyncing.value = true
            _lastSyncSummary.value = "Checking local trips for unsynced records..."
            val unsynced = tripDao.getUnsyncedFirestoreTrips()

            var count = 0
            for (entity in unsynced) {
                val trip = entity.toDomain()
                syncTripInBackground(trip)
                count++
            }

            _isSyncing.value = false
            _lastSyncSummary.value = "Synchronized $count past trip session(s)"
            onComplete(count)
        }
    }
}
