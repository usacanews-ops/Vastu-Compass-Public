package com.vastu.compass.sensor

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLHeading
import platform.darwin.NSObject

class IosCompassSensor : CompassSensor {
    private val locationManager = CLLocationManager()
    override val azimuthDegrees: Flow<Float> = callbackFlow {
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
                val deg = if (didUpdateHeading.trueHeading >= 0) didUpdateHeading.trueHeading.toFloat() else didUpdateHeading.magneticHeading.toFloat()
                trySend(deg)
            }
        }
        locationManager.delegate = delegate
        locationManager.startUpdatingHeading()
        awaitClose {
            locationManager.stopUpdatingHeading()
            locationManager.delegate = null
        }
    }
}
actual fun getCompassSensor(): CompassSensor = IosCompassSensor()
