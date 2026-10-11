package com.vastu.compass

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vastu.compass.data.VastuRepository
import com.vastu.compass.ui.CompassScreen

@Composable
fun App() {
    LaunchedEffect(Unit) {
        VastuRepository.initialize()
        VastuRepository.syncWithGitHub()
    }
    Scaffold(
        bottomBar = {
            BottomNavigation(backgroundColor = Color.White) {
                BottomNavigationItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.CompassCalibration, contentDescription = "Compass") },
                    label = { Text("Compass") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            CompassScreen()
        }
    }
}
