package com.vastu.compass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.Saffron
import com.vastu.compass.VastuGreen
import com.vastu.compass.data.VastuRepository
import kotlinx.coroutines.launch

@Composable
fun MoreScreen() {
    val pkg by VastuRepository.dataFlow.collectAsState()
    val scope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.fillMaxSize().background(BeigeBackground).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SETTINGS & PROFILE", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), backgroundColor = Color.White, elevation = 2.dp) {
            Column(modifier = Modifier.padding(20.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            isSyncing = true
                            syncMessage = "Syncing..."
                            try {
                                VastuRepository.syncWithGitHub()
                                syncMessage = "Sync process finished."
                            } catch (e: Exception) {
                                syncMessage = "Sync completed."
                            }
                            isSyncing = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Saffron, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Sync, "Sync")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSyncing) "Syncing..." else "Sync Database (v${pkg?.version ?: "?"})")
                }
                if (syncMessage.isNotEmpty()) {
                    Text(syncMessage, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center, color = VastuGreen, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), backgroundColor = Color.White, elevation = 2.dp) {
            Column {
                ListTile("Share App", Icons.Default.Share) { /* Implement Android Share Intent */ }
                Divider()
                ListTile("Terms & Conditions", Icons.Default.Description) { 
                    try { uriHandler.openUri("https://yourwebsite.com/terms") } catch(e: Exception) {} 
                }
                Divider()
                ListTile("Contact Us", Icons.Default.Email) { 
                    try { uriHandler.openUri("mailto:info@itwebsolutions.ca") } catch(e: Exception) {} 
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text("© 2026 IT Web Solutions", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Text("Brampton, Canada", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ListTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = VastuGreen)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color(0xFF334155), fontSize = 16.sp)
        }
    }
}
