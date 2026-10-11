package com.vastu.compass

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vastu.compass.data.VastuRepository
import com.vastu.compass.ui.CompassScreen
import com.vastu.compass.ui.ItemDetailScreen
import com.vastu.compass.ui.ProblemsScreen
import com.vastu.compass.ui.ZonesScreen
import com.vastu.compass.ui.MoreScreen

sealed class Screen {
    object Compass : Screen()
    object Zones : Screen()
    data class ItemDetail(val itemId: String) : Screen()
    object Problems : Screen()
    object More : Screen()
}

@Composable
fun App() {
    LaunchedEffect(Unit) {
        VastuRepository.initialize()
        VastuRepository.syncWithGitHub()
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Compass) }

    Scaffold(
        bottomBar = {
            BottomNavigation(backgroundColor = Color.White, contentColor = Color(0xFF3E2723)) {
                BottomNavigationItem(
                    selected = currentScreen is Screen.Compass,
                    onClick = { currentScreen = Screen.Compass },
                    icon = { Icon(Icons.Default.CompassCalibration, contentDescription = "Compass") },
                    label = { Text("Compass") }
                )
                BottomNavigationItem(
                    selected = currentScreen is Screen.Zones || currentScreen is Screen.ItemDetail,
                    onClick = { currentScreen = Screen.Zones },
                    icon = { Icon(Icons.Default.GridOn, contentDescription = "Zones") },
                    label = { Text("Zones") }
                )
                BottomNavigationItem(
                    selected = currentScreen is Screen.Problems,
                    onClick = { currentScreen = Screen.Problems },
                    icon = { Icon(Icons.Default.Warning, contentDescription = "Problems") },
                    label = { Text("Problems") }
                )
                BottomNavigationItem(
                    selected = currentScreen is Screen.More,
                    onClick = { currentScreen = Screen.More },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                    label = { Text("More") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val screen = currentScreen) {
                is Screen.Compass -> CompassScreen()
                is Screen.Zones -> ZonesScreen(onItemClick = { itemId -> currentScreen = Screen.ItemDetail(itemId) })
                is Screen.ItemDetail -> ItemDetailScreen(itemId = screen.itemId, onBack = { currentScreen = Screen.Zones })
                is Screen.Problems -> ProblemsScreen()
                is Screen.More -> MoreScreen() // Now points to our new UI
            }
        }
    }
}
