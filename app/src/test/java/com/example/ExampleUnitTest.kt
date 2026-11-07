package com.example

import com.example.data.model.TelemetryPoint
import com.example.data.model.TelemetrySource
import com.example.data.model.TripData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTripDataJsonGeneration() {
        val sample = TelemetryPoint(
            timestamp = 1700000000000L,
            speedKmh = 105.4f,
            cumulativeDistanceKm = 12.8,
            engineRpm = 2200,
            fuelLevelPercent = 74.2f
        )
        val trip = TripData(
            id = "test-trip-123",
            userId = "user_456",
            title = "Highway Cruise",
            startTimeMs = 1700000000000L,
            endTimeMs = 1700000600000L,
            durationSeconds = 600L,
            totalDistanceKm = 18.5,
            averageSpeedKmh = 110.2f,
            maxSpeedKmh = 124.0f,
            estimatedFuelLiters = 1.38f,
            primarySource = TelemetrySource.CAR_HARDWARE,
            samples = listOf(sample)
        )

        val json = trip.toJsonString()
        assertTrue(json.contains("\"tripId\": \"test-trip-123\""))
        assertTrue(json.contains("\"totalDistanceKm\": 18.5"))
        assertTrue(json.contains("\"primarySource\": \"CAR_HARDWARE\""))
        assertTrue(json.contains("\"speedKmh\": 105.4"))
    }

    @Test
    fun testTripDataCsvGeneration() {
        val sample = TelemetryPoint(
            timestamp = 1700000000000L,
            speedKmh = 80f,
            cumulativeDistanceKm = 5.0,
            engineRpm = 1800,
            fuelLevelPercent = 80f
        )
        val trip = TripData(
            id = "test-csv-789",
            title = "City Commute",
            durationSeconds = 300L,
            totalDistanceKm = 5.0,
            averageSpeedKmh = 60f,
            maxSpeedKmh = 85f,
            primarySource = TelemetrySource.GPS_LOCATION,
            samples = listOf(sample)
        )

        val csv = trip.toCsvString()
        assertTrue(csv.contains("TripId,StartTime,DurationSeconds,TotalDistanceKm,AverageSpeedKmh,MaxSpeedKmh,PrimarySource"))
        assertTrue(csv.contains("\"test-csv-789\""))
        assertTrue(csv.contains("TimestampMs,SpeedKmh,CumulativeDistanceKm,EngineRpm,FuelLevelPercent"))
        assertTrue(csv.contains("1700000000000,80.0,5.0,1800,80.0"))
    }

    @Test
    fun testFormattedDuration() {
        val shortTrip = TripData(durationSeconds = 85L)
        assertEquals("01m 25s", shortTrip.formattedDuration)

        val longTrip = TripData(durationSeconds = 3720L) // 1h 2m
        assertEquals("01h 02m", longTrip.formattedDuration)
    }
}
