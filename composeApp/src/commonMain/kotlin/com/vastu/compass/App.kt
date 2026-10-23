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

// Core Theme Colors used across the app
val BeigeBackground = Color(0xFFFDFBF7)
val Saffron = Color(0xFFFF8F00)
val VastuGreen = Color(0xFF2E7D32)

@Composable
fun App() {
    val vastuData by VastuRepository.dataFlow.collectAsState()
    val isSyncing by VastuRepository.isSyncing.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Trigger initialization on first app launch
    LaunchedEffect(Unit) {
        if (vastuData == null) {
            coroutineScope.launch {
                VastuRepository.initialize()
            }
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BeigeBackground
        ) {
            // Show loading indicator if JSON data has not arrived yet
            if (vastuData == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BeigeBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Saffron,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading Vastu Sacred Geometry...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3E2723)
                        )
                        if (isSyncing) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Syncing with live server...",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            } else {
                // Data is fully loaded — Render your main navigation/screens here
                // (e.g., your bottom navigation bar, home dashboard, or screen router)
                MainNavigationHost()
            }
        }
    }
}

@Composable
fun MainNavigationHost() {
    // Placeholder or your existing screen router/navigator container
    // When vastuData is loaded, your screens (CompassScreen, MapScreen, ZonesScreen, etc.) 
    // will now safely have access to the hybrid data package.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Vastu Hybrid Data Loaded Successfully!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = VastuGreen
        )
    }
}
