package com.vastu.compass

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vastu.compass.data.VastuRepository
import com.vastu.compass.ui.CompassScreen
import kotlinx.coroutines.launch

// Core Theme Colors
val BeigeBackground = Color(0xFFFDFBF7)
val Saffron = Color(0xFFFF8F00)
val VastuGreen = Color(0xFF2E7D32)

@Composable
fun App() {
    val coroutineScope = rememberCoroutineScope()

    // Initialize repository in background without blocking the UI
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            VastuRepository.initialize()
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BeigeBackground
        ) {
            CompassScreen()
        }
    }
}
