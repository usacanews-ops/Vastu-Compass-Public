package com.vastu.compass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.data.VastuRepository
import kotlinx.coroutines.launch

// Core Theme Colors
val BeigeBackground = Color(0xFFFDFBF7)
val Saffron = Color(0xFFFF8F00)
val VastuGreen = Color(0xFF2E7D32)

@Composable
fun App() {
    val coroutineScope = rememberCoroutineScope()

    // Trigger background initialization on app launch without blocking the UI
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
            // App loads instantly. Main navigation renders right away.
            MainNavigationHost()
        }
    }
}
