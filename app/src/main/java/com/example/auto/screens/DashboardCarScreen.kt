package com.example.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.data.model.TelemetrySource
import com.example.data.telemetry.TelemetryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class DashboardCarScreen(carContext: CarContext) : Screen(carContext) {

    private val repository = TelemetryRepository.getInstance(carContext)
    private var updateJob: Job? = null

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                updateJob = lifecycleScope.launch {
                    while (isActive) {
                        delay(1000) // 1Hz throttle for Android Auto head unit template refresh
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
        val telem = repository.telemetry.value
        val activeTrip = repository.activeTrip.value
        val isRecording = repository.isRecordingTrip.value

        val paneBuilder = Pane.Builder()

        // 1. Current Speed Row (Glanceable Primary Metric)
        val speedText = String.format(Locale.US, "%.0f km/h", telem.currentSpeedKmh)
        val speedSubtext = "[${telem.activeSource.badgeText}] • Gear: ${telem.activeGear} • RPM: ${telem.engineRpm}"
        paneBuilder.addRow(
            Row.Builder()
                .setTitle(speedText)
                .addText(speedSubtext)
                .build()
        )

        // 2. Mileage & Trip Distance Row
        val odoText = String.format(Locale.US, "Odometer: %.1f km", telem.odometerKm)
        val tripSub = if (isRecording && activeTrip != null) {
            String.format(Locale.US, "Trip: %.2f km in %s", activeTrip.totalDistanceKm, activeTrip.formattedDuration)
        } else {
            "Trip recorder idle"
        }
        paneBuilder.addRow(
            Row.Builder()
                .setTitle(odoText)
                .addText(tripSub)
                .build()
        )

        // 3. Fuel & Battery Range Row
        val fuelText = String.format(Locale.US, "Energy: %.0f%% • Range: %.0f km", telem.fuelLevelPercent, telem.rangeRemainingKm)
        val statusSub = if (telem.isFuelLow) "WARNING: Low Energy Level" else "Powertrain Normal"
        paneBuilder.addRow(
            Row.Builder()
                .setTitle(fuelText)
                .addText(statusSub)
                .build()
        )

        // 4. Heading & Connection
        val headingTitle = "Heading: ${telem.compassDirection} (${String.format(Locale.US, "%.0f", telem.compassDegrees)}°)"
        val sourceStatus = if (telem.isHardwareConnected) "Vehicle CAN Connected" else "Virtual Simulator Active"
        paneBuilder.addRow(
            Row.Builder()
                .setTitle(headingTitle)
                .addText(sourceStatus)
                .build()
        )

        // Action Strip with Screen Navigation and Source Switch
        val actionStripBuilder = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle(if (telem.activeSource == TelemetrySource.CAR_HARDWARE) "Source: CAR" else "Source: GPS")
                    .setOnClickListener {
                        val next = if (telem.activeSource == TelemetrySource.CAR_HARDWARE) {
                            TelemetrySource.GPS_LOCATION
                        } else {
                            TelemetrySource.CAR_HARDWARE
                        }
                        repository.setSource(next)
                        invalidate()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Trips")
                    .setOnClickListener {
                        screenManager.push(TripStatsCarScreen(carContext))
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Diagnostics")
                    .setOnClickListener {
                        screenManager.push(DiagnosticsCarScreen(carContext))
                    }
                    .build()
            )

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("AutoPulse Cockpit")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(actionStripBuilder.build())
            .build()
    }
}
