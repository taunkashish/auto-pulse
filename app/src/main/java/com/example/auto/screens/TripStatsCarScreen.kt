package com.example.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.data.cloud.CloudSyncCoordinator
import com.example.data.telemetry.TelemetryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class TripStatsCarScreen(carContext: CarContext) : Screen(carContext) {

    private val repository = TelemetryRepository.getInstance(carContext)
    private val syncCoordinator = CloudSyncCoordinator(carContext)
    private var updateJob: Job? = null

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                updateJob = lifecycleScope.launch {
                    while (isActive) {
                        delay(1000)
                        invalidate()
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                updateJob?.cancel()
                updateJob = null
            }
        })
    }

    override fun onGetTemplate(): Template {
        val activeTrip = repository.activeTrip.value
        val isRecording = repository.isRecordingTrip.value

        val paneBuilder = Pane.Builder()

        if (activeTrip != null) {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(String.format(Locale.US, "Distance: %.2f km", activeTrip.totalDistanceKm))
                    .addText("Duration: ${activeTrip.formattedDuration}")
                    .build()
            )
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(String.format(Locale.US, "Avg Speed: %.1f km/h", activeTrip.averageSpeedKmh))
                    .addText(String.format(Locale.US, "Top Speed: %.1f km/h", activeTrip.maxSpeedKmh))
                    .build()
            )
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(String.format(Locale.US, "Est. Fuel: %.2f L", activeTrip.estimatedFuelLiters))
                    .addText("Logged Samples: ${activeTrip.samples.size} points")
                    .build()
            )
        } else {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("No Active Trip")
                    .addText("Press 'Start Trip' to begin logging performance telemetry.")
                    .build()
            )
        }

        // Action strip with recording controls
        val actionStripBuilder = ActionStrip.Builder()

        if (!isRecording && activeTrip == null) {
            actionStripBuilder.addAction(
                Action.Builder()
                    .setTitle("Start Trip")
                    .setOnClickListener {
                        repository.startTrip("Auto Drive #${System.currentTimeMillis() % 1000}")
                        invalidate()
                    }
                    .build()
            )
        } else if (isRecording) {
            actionStripBuilder.addAction(
                Action.Builder()
                    .setTitle("Pause")
                    .setOnClickListener {
                        repository.pauseTrip()
                        invalidate()
                    }
                    .build()
            )
            actionStripBuilder.addAction(
                Action.Builder()
                    .setTitle("Finish & Sync")
                    .setOnClickListener {
                        lifecycleScope.launch {
                            val finished = repository.finishTrip()
                            if (finished != null) {
                                syncCoordinator.syncTripInBackground(finished)
                            }
                            invalidate()
                        }
                    }
                    .build()
            )
        } else {
            actionStripBuilder.addAction(
                Action.Builder()
                    .setTitle("Resume")
                    .setOnClickListener {
                        repository.resumeTrip()
                        invalidate()
                    }
                    .build()
            )
            actionStripBuilder.addAction(
                Action.Builder()
                    .setTitle("Finish & Sync")
                    .setOnClickListener {
                        lifecycleScope.launch {
                            val finished = repository.finishTrip()
                            if (finished != null) {
                                syncCoordinator.syncTripInBackground(finished)
                            }
                            invalidate()
                        }
                    }
                    .build()
            )
        }

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Trip Performance Logger")
            .setHeaderAction(Action.BACK)
            .setActionStrip(actionStripBuilder.build())
            .build()
    }
}
