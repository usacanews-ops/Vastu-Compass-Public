package com.vastu.compass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
