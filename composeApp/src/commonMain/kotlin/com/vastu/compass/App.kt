package com.vastu.compass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.data.VastuRepository
import com.vastu.compass.ui.*

// Theme Colors
val BeigeBackground = Color(0xFFFAF3E0)
val Saffron = Color(0xFFFF9933)
val VastuGreen = Color(0xFF388E3C)

sealed class Screen {
    object Compass : Screen()
    object Zones : Screen()
    data class ItemDetail(val itemId: String) : Screen()
    object Problems : Screen()
    object More : Screen()
    object Consult : Screen() // New Screen
}

@Composable
fun App() {
    LaunchedEffect(Unit) {
        VastuRepository.initialize()
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Compass) }

    Scaffold(
        bottomBar = {
            Column {
                // Persistent Consult Button on every screen
                if (currentScreen !is Screen.Consult) {
                    Button(
                        onClick = { currentScreen = Screen.Consult },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Saffron, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = "Consult")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CONSULT AN EXPERT", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
                BottomNavigation(backgroundColor = Color.White, contentColor = Color(0xFF3E2723)) {
                    BottomNavigationItem(
                        selected = currentScreen is Screen.Compass,
                        onClick = { currentScreen = Screen.Compass },
                        icon = { Icon(Icons.Default.CompassCalibration, contentDescription = "Compass") },
                        label = { Text("Compass") }
                    )
                    BottomNavigationItem(
                        selected = currentScreen is Screen.Zones,
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
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding).fillMaxSize(), color = BeigeBackground) {
            when (val screen = currentScreen) {
                is Screen.Compass -> CompassScreen()
                is Screen.Zones -> ZonesScreen(onItemClick = { itemId -> currentScreen = Screen.ItemDetail(itemId) })
                is Screen.ItemDetail -> ItemDetailScreen(itemId = screen.itemId, onBack = { currentScreen = Screen.Zones })
                is Screen.Problems -> ProblemsScreen()
                is Screen.More -> MoreScreen()
                is Screen.Consult -> ConsultScreen(onBack = { currentScreen = Screen.Compass })
            }
        }
    }
}
