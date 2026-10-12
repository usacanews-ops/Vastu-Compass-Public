package com.vastu.compass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.Saffron
import com.vastu.compass.VastuGreen
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Data models for the online fetch
@Serializable
data class ConsultConfig(
    val services: List<ConsultService>,
    val upiId: String,
    val qrCodeUrl: String, // You will use this when you add an image loading library like Kamel/Coil
    val slots: List<String>
)

@Serializable
data class ConsultService(val name: String, val price: String)

sealed class FetchState {
    object Loading : FetchState()
    data class Success(val data: ConsultConfig) : FetchState()
    data class Error(val message: String) : FetchState()
}

@Composable
fun ConsultScreen(onBack: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    var fetchState by remember { mutableStateOf<FetchState>(FetchState.Loading) }
    
    var step by remember { mutableStateOf(1) } // 1: Select, 2: Pay, 3: Slots, 4: Done
    var selectedService by remember { mutableStateOf<ConsultService?>(null) }
    var selectedSlot by remember { mutableStateOf("") }

    // Strictly Online Fetch (No Local Fallback)
    LaunchedEffect(Unit) {
        try {
            val client = HttpClient()
            // TODO: Replace this URL with your actual live server/gist JSON link
            val url = "https://gist.githubusercontent.com/usacanews-ops/eee6afde8394e822cff9a4a75dc854b9/raw/f89597c5e6939216b12dfe8c7be3bcd4cd31de5c/consult_config.json"
            
            // For right now, this URL will fail and trigger your custom Error UI. 
            // When you want to see the UI succeed, uncomment the mock data below:
            
            /*
            val mockJson = """
                {
                  "services": [
                    {"name": "20 Min Consultation", "price": "₹99"},
                    {"name": "30 Min Consultation", "price": "₹149"},
                    {"name": "60 Min Consultation", "price": "₹299"},
                    {"name": "Complete House Vastu", "price": "₹999"}
                  ],
                  "upiId": "iwsvastuconsultant@axis",
                  "qrCodeUrl": "https://yourdomain.com/qr.png",
                  "slots": ["Today, 4:00 PM", "Today, 6:30 PM", "Tomorrow, 10:00 AM"]
                }
            """.trimIndent()
            val parsedData = Json { ignoreUnknownKeys = true }.decodeFromString<ConsultConfig>(mockJson)
            fetchState = FetchState.Success(parsedData)
            */
            
            // Real network fetch attempt
            val response = client.get(url).bodyAsText()
            val parsedData = Json { ignoreUnknownKeys = true }.decodeFromString<ConsultConfig>(response)
            fetchState = FetchState.Success(parsedData)
            
        } catch (e: Exception) {
            fetchState = FetchState.Error(e.message ?: "Unknown server error")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BeigeBackground).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("BOOK CONSULTATION", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
        }
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = fetchState) {
            is FetchState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(top = 50.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Saffron)
                }
            }
            is FetchState.Error -> {
                Card(modifier = Modifier.fillMaxWidth(), backgroundColor = Color(0xFFFEF2F2), elevation = 0.dp, shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.WifiOff, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Connectivity/Server Error", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFB91C1C))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(state.message, fontSize = 12.sp, color = Color(0xFFDC2626), textAlign = TextAlign.Center)
                    }
                }
            }
            is FetchState.Success -> {
                val data = state.data

                if (step == 1) {
                    Text("Select a Service", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    data.services.forEach { service ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            elevation = 2.dp,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(service.name, fontWeight = FontWeight.Medium)
                                Button(
                                    onClick = { selectedService = service; step = 2 },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = VastuGreen, contentColor = Color.White)
                                ) {
                                    Text("Book ${service.price}")
                                }
                            }
                        }
                    }
                } else if (step == 2) {
                    // PAYMENT GATE
                    Text("Complete Payment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(modifier = Modifier.fillMaxWidth(), elevation = 4.dp, shape = RoundedCornerShape(12.dp), backgroundColor = Color.White) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Amount: ${selectedService?.price}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Saffron)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // QR Code Placeholder (Until Image loading library is added)
                            Box(modifier = Modifier.size(150.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "QR", modifier = Modifier.size(64.dp), tint = Color(0xFF64748B))
                                    Text("Scan via any UPI App", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("OR PAY USING UPI ID", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Copiable UPI ID
                            Surface(
                                color = Color(0xFFEFF6FF), 
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { clipboardManager.setText(AnnotatedString(data.upiId)) }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(data.upiId, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF1E40AF), modifier = Modifier.size(18.dp))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = { step = 3 },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = VastuGreen, contentColor = Color.White)
                            ) {
                                Text("I Have Paid Successfully", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (step == 3) {
                    // SLOTS SECTION (Only revealed after clicking "I have paid")
                    Text("Select your Time Slot", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    data.slots.forEach { slot ->
                        OutlinedButton(
                            onClick = { 
                                selectedSlot = slot
                                step = 4 
                                // TODO: Trigger Telegram Bot Notification Here
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VastuGreen)
                        ) {
                            Text(slot, fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(backgroundColor = Color(0xFFFFF3E0)) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Saffron)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Note: Booked time slots cannot be rescheduled.", color = Color(0xFFE65100), fontSize = 13.sp)
                        }
                    }
                } else if (step == 4) {
                    // SUCCESS
                    Card(modifier = Modifier.fillMaxWidth(), backgroundColor = Color.White, elevation = 4.dp) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = VastuGreen, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Booking Request Sent!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = VastuGreen)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Service: ${selectedService?.name}", fontWeight = FontWeight.Bold)
                            Text("Slot: $selectedSlot", color = Saffron, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("You will receive a WhatsApp call at your selected time. Please be ready with your questions, app screenshots, and a paper & pen to note down remedies.", textAlign = TextAlign.Center, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}
