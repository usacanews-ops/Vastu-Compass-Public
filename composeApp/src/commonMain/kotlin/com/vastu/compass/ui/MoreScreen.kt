package com.vastu.compass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.data.VastuRepository
import kotlinx.coroutines.launch

@Composable
fun MoreScreen() {
    val pkg by VastuRepository.dataFlow.collectAsState()
    val scope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SETTINGS & PROFILE", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B), letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(32.dp))

        // Sync & Data Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = 4.dp,
            backgroundColor = Color.White
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Database Version: ${pkg?.version ?: "Offline"}", 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.SemiBold, 
                    color = Color(0xFF334155)
                )
                Text(
                    text = "Last Updated: ${pkg?.lastUpdated ?: "N/A"}", 
                    fontSize = 12.sp, 
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            isSyncing = true
                            VastuRepository.syncWithGitHub() // Manually trigger fetch
                            isSyncing = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3B82F6), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isSyncing
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSyncing) "Syncing with Server..." else "Sync Data Manually")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Support Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = 4.dp,
            backgroundColor = Color.White
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Need Help or Vastu Consultation?", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        try {
                            // Opens the user's default email app
                            uriHandler.openUri("mailto:info@itwebsolutions.ca")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = "Contact")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contact Us")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Developer Branding
        Text(
            text = "Developed by IT Web Solutions, Brampton",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}
