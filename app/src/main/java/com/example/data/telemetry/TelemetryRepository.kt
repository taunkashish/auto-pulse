package com.example.data.telemetry

import android.content.Context
import androidx.car.app.CarContext
import com.example.data.local.AutoPulseDatabase
import com.example.data.local.TripEntity
import com.example.data.model.DriveProfile
import com.example.data.model.TelemetryPoint
import com.example.data.model.TelemetrySource
import com.example.data.model.TripData
import com.example.data.model.VehicleTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class TelemetryRepository private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val database = AutoPulseDatabase.getInstance(appContext)
    private val tripDao = database.tripDao()

    val simulatorSource = SimulatorTelemetrySource()
    val gpsSource = GpsTelemetrySource(appContext)
    private var carHardwareSource: CarHardwareTelemetrySource? = null

    private val _telemetry = MutableStateFlow(VehicleTelemetry())
    val telemetry: StateFlow<VehicleTelemetry> = _telemetry.asStateFlow()

    private val _activeTrip = MutableStateFlow<TripData?>(null)
    val activeTrip: StateFlow<TripData?> = _activeTrip.asStateFlow()

    private val _isRecordingTrip = MutableStateFlow(false)
    val isRecordingTrip: StateFlow<Boolean> = _isRecordingTrip.asStateFlow()

    private var activeSource = TelemetrySource.SIMULATOR
    private var baselineOdometerKm = 14280.5
    private var tripRecordedPoints = mutableListOf<TelemetryPoint>()
    private var loopJob: Job? = null
    private var tripTimerJob: Job? = null

    init {
        simulatorSource.start()
        startUnifiedLoop()
    }

    /**
     * Attaches Android Auto CarContext when vehicle projection is active.
     */
    fun attachCarContext(carContext: CarContext) {
        val hardware = CarHardwareTelemetrySource(carContext)
        carHardwareSource = hardware
        hardware.start()
        _telemetry.value = _telemetry.value.copy(
            isHardwareConnected = true,
            oemModel = "Android Auto Connected Vehicle"
        )
    }

    fun detachCarContext() {
        carHardwareSource?.stop()
        carHardwareSource = null
        _telemetry.value = _telemetry.value.copy(isHardwareConnected = false)
    }

    fun setSource(source: TelemetrySource) {
        activeSource = source
        if (source == TelemetrySource.GPS_LOCATION) {
            gpsSource.start()
        }
    }

    fun setDriveProfile(profile: DriveProfile) {
        simulatorSource.setDriveProfile(profile)
        if (activeSource != TelemetrySource.SIMULATOR) {
            setSource(TelemetrySource.SIMULATOR)
        }
    }

    fun setThrottle(percent: Float) {
        simulatorSource.setThrottle(percent)
        if (activeSource != TelemetrySource.SIMULATOR) {
            setSource(TelemetrySource.SIMULATOR)
        }
    }

    fun setBraking(braking: Boolean) {
        simulatorSource.setBraking(braking)
    }

    private fun startUnifiedLoop() {
        loopJob = scope.launch {
            while (isActive) {
                delay(100) // 10Hz unified publish
                val sim = simulatorSource.output.value
                val gps = gpsSource.output.value
                val hw = carHardwareSource?.output?.value

                baselineOdometerKm += sim.odometerDeltaKm

                // Determine active speed based on selected source
                val (chosenSpeed, displaySpeed, odo) = when (activeSource) {
                    TelemetrySource.CAR_HARDWARE -> {
                        if (hw != null && hw.isConnected && hw.rawSpeedMps != null) {
                            val spd = hw.rawSpeedMps * 3.6f
                            val disp = (hw.displaySpeedMps ?: hw.rawSpeedMps) * 3.6f
                            val odoKm = (hw.odometerMeters?.toDouble() ?: (baselineOdometerKm * 1000.0)) / 1000.0
                            Triple(spd, disp, odoKm)
                        } else {
                            // Hardware requested but no active AA connection -> smooth fallback to simulator
                            Triple(sim.speedKmh, sim.displaySpeedKmh, baselineOdometerKm)
                        }
                    }
                    TelemetrySource.GPS_LOCATION -> {
                        val odoKm = baselineOdometerKm + gps.distanceAccumulatedKm
                        Triple(gps.speedKmh, gps.speedKmh, odoKm)
                    }
                    TelemetrySource.SIMULATOR -> {
                        Triple(sim.speedKmh, sim.displaySpeedKmh, baselineOdometerKm)
                    }
                }

                val compassDeg = hw?.compassOrientationDegrees ?: gps.bearingDegrees.takeIf { it != 0f } ?: sim.compassDegrees
                val headingStr = degreesToHeading(compassDeg)

                val fuelPct = hw?.fuelPercent ?: sim.fuelPercent
                val rangeKm = hw?.rangeRemainingMeters?.let { it / 1000f } ?: sim.rangeKm
                val isLow = hw?.isFuelLow ?: (fuelPct < 15f)

                _telemetry.value = VehicleTelemetry(
                    currentSpeedKmh = chosenSpeed,
                    displaySpeedKmh = displaySpeed,
                    gpsSpeedKmh = gps.speedKmh,
                    activeSource = activeSource,
                    odometerKm = odo,
                    fuelLevelPercent = fuelPct,
                    batteryLevelPercent = hw?.batteryPercent ?: 84f,
                    rangeRemainingKm = rangeKm,
                    engineRpm = sim.engineRpm,
                    coolantTempC = sim.coolantTempC,
                    batteryVoltage = sim.batteryVoltage,
                    compassDegrees = compassDeg,
                    compassDirection = headingStr,
                    activeGear = sim.activeGear,
                    isFuelLow = isLow,
                    isSpeedAlert = chosenSpeed > 120f,
                    isHardwareConnected = hw?.isConnected ?: false,
                    oemModel = if (hw?.isConnected == true) "CarService Vehicle" else "AutoPulse Virtual Simulator",
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    private fun degreesToHeading(deg: Float): String {
        val normalized = (deg % 360 + 360) % 360
        return when {
            normalized in 337.5..360.0 || normalized in 0.0..22.5 -> "N"
            normalized in 22.5..67.5 -> "NE"
            normalized in 67.5..112.5 -> "E"
            normalized in 112.5..157.5 -> "SE"
            normalized in 157.5..202.5 -> "S"
            normalized in 202.5..247.5 -> "SW"
            normalized in 247.5..292.5 -> "W"
            else -> "NW"
        }
    }

    // --- Active Trip Management ---

    fun startTrip(title: String = "Drive Trip", userId: String = "") {
        if (_isRecordingTrip.value) return
        tripRecordedPoints.clear()
        val newTrip = TripData(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            startTimeMs = System.currentTimeMillis(),
            primarySource = activeSource
        )
        _activeTrip.value = newTrip
        _isRecordingTrip.value = true

        tripTimerJob?.cancel()
        tripTimerJob = scope.launch {
            var seconds = 0L
            var accumulatedDistance = 0.0
            var speedSum = 0.0
            var maxSpeed = 0f

            while (isActive && _isRecordingTrip.value) {
                delay(1000) // Sample point every 1s
                seconds++
                val cur = _telemetry.value
                val speed = cur.currentSpeedKmh
                val distDelta = (speed / 3600.0) // 1 second
                accumulatedDistance += distDelta
                speedSum += speed
                if (speed > maxSpeed) maxSpeed = speed

                val point = TelemetryPoint(
                    timestamp = System.currentTimeMillis(),
                    speedKmh = speed,
                    cumulativeDistanceKm = accumulatedDistance,
                    engineRpm = cur.engineRpm,
                    fuelLevelPercent = cur.fuelLevelPercent
                )
                tripRecordedPoints.add(point)

                val avg = (speedSum / seconds).toFloat()

                _activeTrip.value = _activeTrip.value?.copy(
                    durationSeconds = seconds,
                    totalDistanceKm = accumulatedDistance,
                    averageSpeedKmh = avg,
                    maxSpeedKmh = maxSpeed,
                    estimatedFuelLiters = (accumulatedDistance * 0.075).toFloat(), // ~7.5L / 100km
                    samples = tripRecordedPoints.toList()
                )
            }
        }
    }

    fun pauseTrip() {
        _isRecordingTrip.value = false
    }

    fun resumeTrip() {
        _isRecordingTrip.value = true
    }

    suspend fun finishTrip(): TripData? {
        _isRecordingTrip.value = false
        tripTimerJob?.cancel()
        tripTimerJob = null

        val current = _activeTrip.value ?: return null
        val finishedTrip = current.copy(
            endTimeMs = System.currentTimeMillis(),
            samples = tripRecordedPoints.toList()
        )

        // Save into local Room database
        tripDao.insertTrip(TripEntity.fromDomain(finishedTrip))
        _activeTrip.value = null
        tripRecordedPoints.clear()

        return finishedTrip
    }

    fun observeAllTrips(): Flow<List<TripData>> {
        return tripDao.getAllTrips().map { list -> list.map { it.toDomain() } }
    }

    suspend fun deleteTrip(trip: TripData) {
        tripDao.deleteTrip(TripEntity.fromDomain(trip))
    }

    suspend fun markTripSynced(tripId: String, firestore: Boolean, drive: Boolean) {
        val entity = tripDao.getTripById(tripId) ?: return
        val updated = entity.copy(
            isSyncedFirestore = if (firestore) true else entity.isSyncedFirestore,
            isExportedDrive = if (drive) true else entity.isExportedDrive
        )
        tripDao.updateTrip(updated)
    }

    companion object {
        @Volatile
        private var INSTANCE: TelemetryRepository? = null

        fun getInstance(context: Context): TelemetryRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TelemetryRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
