package com.example.auto

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.auto.screens.DashboardCarScreen
import com.example.data.telemetry.TelemetryRepository

class AutoPulseSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        val repository = TelemetryRepository.getInstance(carContext)
        repository.attachCarContext(carContext)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                repository.detachCarContext()
            }
        })

        return DashboardCarScreen(carContext)
    }
}
