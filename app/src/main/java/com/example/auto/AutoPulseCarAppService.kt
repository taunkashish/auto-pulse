package com.example.auto

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Main Android Auto Service registered with the system to project
 * vehicle telemetry on the car's head unit display.
 */
class AutoPulseCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // Allows both verified OEM hosts and Desktop Head Unit (DHU) during testing
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return AutoPulseSession()
    }
}
