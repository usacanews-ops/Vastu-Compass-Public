package com.vastu.compass.sensor

import kotlinx.coroutines.flow.Flow

interface CompassSensor {
    val azimuthDegrees: Flow<Float>
}

expect fun getCompassSensor(): CompassSensor
