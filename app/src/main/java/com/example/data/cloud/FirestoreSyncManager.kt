package com.example.data.cloud

import android.content.Context
import com.example.data.model.TripData
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(private val context: Context) {

    suspend fun syncTrip(userId: String, trip: TripData): Result<Unit> {
        return try {
            val db = try {
                // If named database ID is provided in resources, use it; otherwise fallback to default
                val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
                if (resId != 0) {
                    FirebaseFirestore.getInstance(context.getString(resId))
                } else {
                    Firebase.firestore
                }
            } catch (e: Exception) {
                Firebase.firestore
            }

            val docRef = db.collection("users")
                .document(userId.ifEmpty { "anonymous_driver" })
                .collection("trips")
                .document(trip.id)

            val payload = mapOf(
                "tripId" to trip.id,
                "userId" to userId,
                "title" to trip.title,
                "startTimeMs" to trip.startTimeMs,
                "endTimeMs" to trip.endTimeMs,
                "durationSeconds" to trip.durationSeconds,
                "totalDistanceKm" to trip.totalDistanceKm,
                "averageSpeedKmh" to trip.averageSpeedKmh,
                "maxSpeedKmh" to trip.maxSpeedKmh,
                "estimatedFuelLiters" to trip.estimatedFuelLiters,
                "primarySource" to trip.primarySource.name,
                "samplesCount" to trip.samples.size,
                "syncedAt" to System.currentTimeMillis()
            )

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
