package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Domain representation of a completed or active trip.
 */
data class TripData(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "Trip #${System.currentTimeMillis() % 10000}",
    val startTimeMs: Long = System.currentTimeMillis(),
    val endTimeMs: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0L,
    val totalDistanceKm: Double = 0.0,
    val averageSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val estimatedFuelLiters: Float = 0f,
    val primarySource: TelemetrySource = TelemetrySource.CAR_HARDWARE,
    val isSyncedFirestore: Boolean = false,
    val isExportedDrive: Boolean = false,
    val samples: List<TelemetryPoint> = emptyList()
) {
    val formattedDuration: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format(Locale.US, "%02dh %02dm", hours, minutes % 60)
            } else {
                String.format(Locale.US, "%02dm %02ds", minutes, seconds)
            }
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(startTimeMs))
        }

    /**
     * Generates structured JSON formatted for Google Drive and external analytics.
     */
    fun toJsonString(): String {
        val samplesJson = samples.joinToString(separator = ",\n    ") { sample ->
            """{"timestamp": ${sample.timestamp}, "speedKmh": ${sample.speedKmh}, "distanceKm": ${sample.cumulativeDistanceKm}, "rpm": ${sample.engineRpm}, "fuelPct": ${sample.fuelLevelPercent}}"""
        }

        return """{
  "tripId": "$id",
  "userId": "$userId",
  "title": "$title",
  "startTimeMs": $startTimeMs,
  "endTimeMs": $endTimeMs,
  "durationSeconds": $durationSeconds,
  "totalDistanceKm": $totalDistanceKm,
  "averageSpeedKmh": $averageSpeedKmh,
  "maxSpeedKmh": $maxSpeedKmh,
  "estimatedFuelLiters": $estimatedFuelLiters,
  "primarySource": "${primarySource.name}",
  "samplesCount": ${samples.size},
  "samples": [
    $samplesJson
  ]
}"""
    }

    /**
     * Generates standard CSV formatted for Google Sheets, Excel, and Pandas.
     */
    fun toCsvString(): String {
        val sb = StringBuilder()
        sb.appendLine("TripId,StartTime,DurationSeconds,TotalDistanceKm,AverageSpeedKmh,MaxSpeedKmh,PrimarySource")
        sb.appendLine("\"$id\",\"$formattedDate\",$durationSeconds,$totalDistanceKm,$averageSpeedKmh,$maxSpeedKmh,\"${primarySource.label}\"")
        sb.appendLine()
        sb.appendLine("TimestampMs,SpeedKmh,CumulativeDistanceKm,EngineRpm,FuelLevelPercent")
        for (sample in samples) {
            sb.appendLine("${sample.timestamp},${sample.speedKmh},${sample.cumulativeDistanceKm},${sample.engineRpm},${sample.fuelLevelPercent}")
        }
        return sb.toString()
    }
}
