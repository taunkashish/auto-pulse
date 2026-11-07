package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.TelemetrySource
import com.example.data.model.TripData

/**
 * Room persistence entity for offline trip history.
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationSeconds: Long,
    val totalDistanceKm: Double,
    val averageSpeedKmh: Float,
    val maxSpeedKmh: Float,
    val estimatedFuelLiters: Float,
    val primarySource: String,
    val isSyncedFirestore: Boolean,
    val isExportedDrive: Boolean,
    val samplesJson: String
) {
    fun toDomain(): TripData {
        return TripData(
            id = id,
            userId = userId,
            title = title,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            durationSeconds = durationSeconds,
            totalDistanceKm = totalDistanceKm,
            averageSpeedKmh = averageSpeedKmh,
            maxSpeedKmh = maxSpeedKmh,
            estimatedFuelLiters = estimatedFuelLiters,
            primarySource = try { TelemetrySource.valueOf(primarySource) } catch (e: Exception) { TelemetrySource.CAR_HARDWARE },
            isSyncedFirestore = isSyncedFirestore,
            isExportedDrive = isExportedDrive,
            samples = emptyList() // Details can be loaded or exported from samplesJson
        )
    }

    companion object {
        fun fromDomain(trip: TripData): TripEntity {
            return TripEntity(
                id = trip.id,
                userId = trip.userId,
                title = trip.title,
                startTimeMs = trip.startTimeMs,
                endTimeMs = trip.endTimeMs,
                durationSeconds = trip.durationSeconds,
                totalDistanceKm = trip.totalDistanceKm,
                averageSpeedKmh = trip.averageSpeedKmh,
                maxSpeedKmh = trip.maxSpeedKmh,
                estimatedFuelLiters = trip.estimatedFuelLiters,
                primarySource = trip.primarySource.name,
                isSyncedFirestore = trip.isSyncedFirestore,
                isExportedDrive = trip.isExportedDrive,
                samplesJson = trip.toJsonString()
            )
        }
    }
}
