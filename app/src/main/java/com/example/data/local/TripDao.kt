package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startTimeMs DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getTripById(id: String): TripEntity?

    @Query("SELECT * FROM trips WHERE isSyncedFirestore = 0")
    suspend fun getUnsyncedFirestoreTrips(): List<TripEntity>

    @Query("SELECT SUM(totalDistanceKm) FROM trips")
    fun getTotalDistanceAllTrips(): Flow<Double?>

    @Query("SELECT SUM(durationSeconds) FROM trips")
    fun getTotalDurationAllTrips(): Flow<Long?>

    @Query("SELECT AVG(averageSpeedKmh) FROM trips")
    fun getAverageSpeedAllTrips(): Flow<Float?>

    @Query("SELECT COUNT(*) FROM trips")
    fun getTotalTripsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTripById(tripId: String)

    @Query("DELETE FROM trips")
    suspend fun clearAll()
}
