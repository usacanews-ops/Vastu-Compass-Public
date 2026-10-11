package com.vastu.compass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.vastu.compass.sensor.sensorContext
import com.vastu.compass.storage.storageContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Context bindings required by expect/actual sensor and storage Singletons
        sensorContext = applicationContext
        storageContext = applicationContext

        setContent {
            App()
        }
    }
}
