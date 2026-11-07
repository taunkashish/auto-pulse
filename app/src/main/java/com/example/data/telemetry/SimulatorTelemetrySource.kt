package com.example.data.telemetry

import com.example.data.model.DriveProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class SimulatorOutput(
    val speedKmh: Float = 0f,
    val displaySpeedKmh: Float = 0f,
    val engineRpm: Int = 850,
    val fuelPercent: Float = 76.5f,
    val rangeKm: Float = 412f,
    val activeGear: String = "P",
    val coolantTempC: Float = 89f,
    val batteryVoltage: Float = 14.1f,
    val compassDegrees: Float = 180f,
    val odometerDeltaKm: Double = 0.0
)

class SimulatorTelemetrySource {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    private val _output = MutableStateFlow(SimulatorOutput())
    val output: StateFlow<SimulatorOutput> = _output.asStateFlow()

    private var currentSpeed = 0f
    private var targetSpeed = 0f
    private var throttlePercent = 0f
    private var isBraking = false
    private var currentProfile = DriveProfile.MANUAL
    private var simTimeSeconds = 0
    private var fuelLevel = 76.5f
    private var compassHeading = 180f

    fun start() {
        if (job != null) return
        job = scope.launch {
            while (isActive) {
                delay(100) // 10Hz physics loop
                updatePhysics(0.1f)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun setDriveProfile(profile: DriveProfile) {
        currentProfile = profile
        if (profile != DriveProfile.MANUAL) {
            targetSpeed = profile.targetSpeedKmh
            isBraking = false
        }
    }

    fun setThrottle(percent: Float) {
        throttlePercent = percent.coerceIn(0f, 100f)
        currentProfile = DriveProfile.MANUAL
        targetSpeed = throttlePercent * 1.8f // 100% throttle = 180 km/h
        isBraking = false
    }

    fun setBraking(braking: Boolean) {
        isBraking = braking
        if (braking) {
            targetSpeed = 0f
            throttlePercent = 0f
            currentProfile = DriveProfile.MANUAL
        }
    }

    private fun updatePhysics(dt: Float) {
        simTimeSeconds++

        // Autonomic drive profile variations
        when (currentProfile) {
            DriveProfile.CITY_STOP_GO -> {
                // Periodic stops at red lights and acceleration
                val cyclePhase = (simTimeSeconds / 10) % 40
                targetSpeed = if (cyclePhase < 12) 0f else 48f + (sin(cyclePhase.toDouble()) * 8).toFloat()
            }
            DriveProfile.HIGHWAY_CRUISE -> {
                targetSpeed = 112f + (cos(simTimeSeconds * 0.05) * 4f).toFloat()
            }
            DriveProfile.MOUNTAIN_PASS -> {
                targetSpeed = 70f + (sin(simTimeSeconds * 0.1) * 15f).toFloat()
            }
            DriveProfile.MANUAL -> {
                // Keep targetSpeed as set
            }
        }

        // Acceleration or braking physics
        if (isBraking) {
            currentSpeed = (currentSpeed - 35f * dt).coerceAtLeast(0f)
        } else if (currentSpeed < targetSpeed) {
            val accelRate = (24f - (currentSpeed * 0.08f)).coerceAtLeast(4f)
            currentSpeed = (currentSpeed + accelRate * dt).coerceAtMost(targetSpeed)
        } else if (currentSpeed > targetSpeed) {
            currentSpeed = (currentSpeed - 12f * dt).coerceAtLeast(targetSpeed)
        }

        // Small jitter for realism
        val speedJitter = if (currentSpeed > 5f) (Random.nextFloat() - 0.5f) * 0.4f else 0f
        val actualSpeed = (currentSpeed + speedJitter).coerceAtLeast(0f)
        val displaySpeed = if (actualSpeed > 1f) actualSpeed + 1.5f else 0f

        // Calculate gear & engine RPM
        val (gear, rpm) = calculateGearAndRpm(actualSpeed)

        // Fuel depletion (higher at high RPM/speed)
        val consumptionFactor = (rpm / 3000f) * 0.0001f * dt
        fuelLevel = (fuelLevel - consumptionFactor).coerceAtLeast(0f)
        val range = fuelLevel * 5.4f

        // Compass heading subtle turn
        if (actualSpeed > 2f) {
            compassHeading = (compassHeading + sin(simTimeSeconds * 0.03).toFloat() * 0.4f + 360f) % 360f
        }

        // Distance delta in km
        val distanceDeltaKm = (actualSpeed / 3600.0) * dt

        _output.value = SimulatorOutput(
            speedKmh = actualSpeed,
            displaySpeedKmh = displaySpeed,
            engineRpm = rpm,
            fuelPercent = fuelLevel,
            rangeKm = range,
            activeGear = gear,
            coolantTempC = 88f + (rpm * 0.002f),
            batteryVoltage = if (actualSpeed > 1f) 14.2f else 12.6f,
            compassDegrees = compassHeading,
            odometerDeltaKm = distanceDeltaKm
        )
    }

    private fun calculateGearAndRpm(speed: Float): Pair<String, Int> {
        if (speed < 0.5f) return Pair("P", 800)
        return when {
            speed < 25f -> Pair("1", (900 + (speed / 25f) * 2200).toInt())
            speed < 45f -> Pair("2", (1400 + ((speed - 25f) / 20f) * 2000).toInt())
            speed < 70f -> Pair("3", (1600 + ((speed - 45f) / 25f) * 2000).toInt())
            speed < 95f -> Pair("4", (1800 + ((speed - 70f) / 25f) * 1800).toInt())
            speed < 125f -> Pair("5", (1900 + ((speed - 95f) / 30f) * 1700).toInt())
            else -> Pair("6", (2100 + ((speed - 125f) / 50f) * 1800).coerceAtMost(5800f).toInt())
        }
    }
}
