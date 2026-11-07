package com.example.auto.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.data.telemetry.TelemetryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class DiagnosticsCarScreen(carContext: CarContext) : Screen(carContext) {

    private val repository = TelemetryRepository.getInstance(carContext)
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
        val telem = repository.telemetry.value
        val paneBuilder = Pane.Builder()

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("CarService Connection")
                .addText(if (telem.isHardwareConnected) "Live OEM CarHardwareManager Active" else "Offline / Simulation Provider Active")
                .build()
        )

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Coolant & Battery")
                .addText(String.format(Locale.US, "Coolant: %.1f°C • Voltage: %.1fV", telem.coolantTempC, telem.batteryVoltage))
                .build()
        )

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Tachometer & Powertrain")
                .addText(String.format(Locale.US, "RPM: %d • Gear: %s", telem.engineRpm, telem.activeGear))
                .build()
        )

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Sensors & Orientation")
                .addText("Heading: ${telem.compassDirection} (${String.format(Locale.US, "%.0f°", telem.compassDegrees)}) • GPS Speed: ${String.format(Locale.US, "%.0f km/h", telem.gpsSpeedKmh)}")
                .build()
        )

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Vehicle Diagnostics & Health")
            .setHeaderAction(Action.BACK)
            .build()
    }
}
