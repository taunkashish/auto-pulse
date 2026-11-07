package com.example.data.telemetry

import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.Compass
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.hardware.info.Mileage
import androidx.car.app.hardware.info.Speed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CarHardwareOutput(
    val rawSpeedMps: Float? = null,
    val displaySpeedMps: Float? = null,
    val odometerMeters: Float? = null,
    val batteryPercent: Float? = null,
    val fuelPercent: Float? = null,
    val rangeRemainingMeters: Float? = null,
    val compassOrientationDegrees: Float? = null,
    val isFuelLow: Boolean = false,
    val isConnected: Boolean = false
)

class CarHardwareTelemetrySource(private val carContext: CarContext) {

    private val _output = MutableStateFlow(CarHardwareOutput())
    val output: StateFlow<CarHardwareOutput> = _output.asStateFlow()

    private var carHardwareManager: CarHardwareManager? = null
    private var isSubscribed = false

    private val speedListener = OnCarDataAvailableListener<Speed> { speedData ->
        val raw = if (speedData.rawSpeedMetersPerSecond.status == CarValue.STATUS_SUCCESS) {
            speedData.rawSpeedMetersPerSecond.value
        } else null

        val display = if (speedData.displaySpeedMetersPerSecond.status == CarValue.STATUS_SUCCESS) {
            speedData.displaySpeedMetersPerSecond.value
        } else null

        _output.value = _output.value.copy(
            rawSpeedMps = raw,
            displaySpeedMps = display,
            isConnected = true
        )
    }

    private val mileageListener = OnCarDataAvailableListener<Mileage> { mileageData ->
        if (mileageData.odometerMeters.status == CarValue.STATUS_SUCCESS) {
            _output.value = _output.value.copy(
                odometerMeters = mileageData.odometerMeters.value,
                isConnected = true
            )
        }
    }

    private val energyListener = OnCarDataAvailableListener<EnergyLevel> { energyData ->
        val battery = if (energyData.batteryPercent.status == CarValue.STATUS_SUCCESS) {
            energyData.batteryPercent.value
        } else null

        val fuel = if (energyData.fuelPercent.status == CarValue.STATUS_SUCCESS) {
            energyData.fuelPercent.value
        } else null

        val range = if (energyData.rangeRemainingMeters.status == CarValue.STATUS_SUCCESS) {
            energyData.rangeRemainingMeters.value
        } else null

        val fuelLow = if (energyData.energyIsLow.status == CarValue.STATUS_SUCCESS) {
            energyData.energyIsLow.value ?: false
        } else false

        _output.value = _output.value.copy(
            batteryPercent = battery,
            fuelPercent = fuel,
            rangeRemainingMeters = range,
            isFuelLow = fuelLow,
            isConnected = true
        )
    }

    private val compassListener = OnCarDataAvailableListener<Compass> { compassData ->
        if (compassData.orientations.status == CarValue.STATUS_SUCCESS) {
            val orientations = compassData.orientations.value
            if (orientations != null && orientations.isNotEmpty()) {
                val degrees = (orientations[0] + 360f) % 360f
                _output.value = _output.value.copy(
                    compassOrientationDegrees = degrees,
                    isConnected = true
                )
            }
        }
    }

    fun start() {
        if (isSubscribed) return
        try {
            carHardwareManager = carContext.getCarService(CarHardwareManager::class.java)
            val carInfo = carHardwareManager?.carInfo
            val carSensors = carHardwareManager?.carSensors
            val executor = carContext.mainExecutor

            carInfo?.addSpeedListener(executor, speedListener)
            carInfo?.addMileageListener(executor, mileageListener)
            carInfo?.addEnergyLevelListener(executor, energyListener)
            carSensors?.addCompassListener(androidx.car.app.hardware.info.CarSensors.UPDATE_RATE_NORMAL, executor, compassListener)

            isSubscribed = true
            _output.value = _output.value.copy(isConnected = true)
        } catch (e: Exception) {
            _output.value = _output.value.copy(isConnected = false)
        }
    }

    fun stop() {
        if (!isSubscribed) return
        try {
            val carInfo = carHardwareManager?.carInfo
            val carSensors = carHardwareManager?.carSensors

            carInfo?.removeSpeedListener(speedListener)
            carInfo?.removeMileageListener(mileageListener)
            carInfo?.removeEnergyLevelListener(energyListener)
            carSensors?.removeCompassListener(compassListener)
        } catch (e: Exception) {
            // ignore
        }
        isSubscribed = false
        _output.value = _output.value.copy(isConnected = false)
    }
}
