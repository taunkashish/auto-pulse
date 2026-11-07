package com.example.data.model

/**
 * Data source providing vehicle speed and distance.
 */
enum class TelemetrySource(val label: String, val badgeText: String) {
    CAR_HARDWARE("Car Hardware", "CAR HARDWARE"),
    GPS_LOCATION("GPS / Google Maps", "GPS GROUND"),
    SIMULATOR("Telemetry Simulator", "SIMULATOR")
}

/**
 * Drive profile simulation preset.
 */
enum class DriveProfile(val label: String, val targetSpeedKmh: Float) {
    CITY_STOP_GO("Urban Stop & Go", 45f),
    HIGHWAY_CRUISE("Highway Cruise", 110f),
    MOUNTAIN_PASS("Mountain Ascent", 75f),
    MANUAL("Manual Throttle", 0f)
}

/**
 * Real-time unified vehicle telemetry snapshot.
 */
data class VehicleTelemetry(
    val currentSpeedKmh: Float = 0f,
    val displaySpeedKmh: Float = 0f,
    val gpsSpeedKmh: Float = 0f,
    val activeSource: TelemetrySource = TelemetrySource.SIMULATOR,
    val odometerKm: Double = 14280.5,
    val fuelLevelPercent: Float = 76f,
    val batteryLevelPercent: Float = 84f,
    val rangeRemainingKm: Float = 410f,
    val engineRpm: Int = 1650,
    val coolantTempC: Float = 88f,
    val batteryVoltage: Float = 13.9f,
    val compassDegrees: Float = 180f,
    val compassDirection: String = "S",
    val activeGear: String = "D",
    val isFuelLow: Boolean = false,
    val isSpeedAlert: Boolean = false,
    val isHardwareConnected: Boolean = false,
    val oemModel: String = "Connected Vehicle",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Individual point sampled during an active driving trip.
 */
data class TelemetryPoint(
    val timestamp: Long = System.currentTimeMillis(),
    val speedKmh: Float = 0f,
    val cumulativeDistanceKm: Double = 0.0,
    val engineRpm: Int = 0,
    val fuelLevelPercent: Float = 0f,
    val latitude: Double? = null,
    val longitude: Double? = null
)
