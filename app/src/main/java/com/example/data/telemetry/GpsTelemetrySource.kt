package com.example.data.telemetry

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GpsOutput(
    val speedKmh: Float = 0f,
    val distanceAccumulatedKm: Double = 0.0,
    val bearingDegrees: Float = 0f,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val hasGpsFix: Boolean = false
)

class GpsTelemetrySource(private val context: Context) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val _output = MutableStateFlow(GpsOutput())
    val output: StateFlow<GpsOutput> = _output.asStateFlow()

    private var lastLocation: Location? = null
    private var totalDistanceKm = 0.0
    private var isListening = false

    @SuppressLint("MissingPermission")
    fun start() {
        if (isListening || locationManager == null) return
        try {
            val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (hasGps) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L, // 500ms
                    1f,   // 1 meter
                    this,
                    Looper.getMainLooper()
                )
                isListening = true
            } else if (hasNetwork) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    2f,
                    this,
                    Looper.getMainLooper()
                )
                isListening = true
            }
        } catch (e: SecurityException) {
            // Permission not granted by user yet
            _output.value = _output.value.copy(hasGpsFix = false)
        }
    }

    fun stop() {
        if (!isListening) return
        try {
            locationManager?.removeUpdates(this)
        } catch (e: Exception) {
            // ignore
        }
        isListening = false
    }

    override fun onLocationChanged(location: Location) {
        val speedKmh = if (location.hasSpeed()) {
            location.speed * 3.6f // m/s to km/h
        } else {
            0f
        }

        lastLocation?.let { prev ->
            val distMeters = calculateHaversineDistanceMeters(
                prev.latitude, prev.longitude,
                location.latitude, location.longitude
            )
            // Filter stationary GPS drift
            if (distMeters > 2.0 && speedKmh > 1.0) {
                totalDistanceKm += (distMeters / 1000.0)
            }
        }
        lastLocation = location

        _output.value = GpsOutput(
            speedKmh = speedKmh,
            distanceAccumulatedKm = totalDistanceKm,
            bearingDegrees = if (location.hasBearing()) location.bearing else _output.value.bearingDegrees,
            latitude = location.latitude,
            longitude = location.longitude,
            hasGpsFix = true
        )
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {
        _output.value = _output.value.copy(hasGpsFix = false)
    }

    private fun calculateHaversineDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
