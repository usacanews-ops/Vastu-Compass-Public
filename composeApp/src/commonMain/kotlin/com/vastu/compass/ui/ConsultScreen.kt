package com.vastu.compass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle // Added missing import
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.Saffron
import com.vastu.compass.VastuGreen

@Composable
fun ConsultScreen(onBack: () -> Unit) {
    var step by remember { mutableStateOf(1) } // 1: Select, 2: Slots, 3: Confirm
    var selectedService by remember { mutableStateOf("") }
    
    // In production, these will be fetched via Ktor from your server
    val services = listOf(
        "20 Min Consultation" to "₹99",
        "30 Min Consultation" to "₹149",
        "60 Min Consultation" to "₹299",
        "Complete House Vastu" to "₹999"
    )

    Column(modifier = Modifier.fillMaxSize().background(BeigeBackground).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("BOOK CONSULTATION", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (step == 1) {
            Text("Select a Service", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            services.forEach { (name, price) ->
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
                        Text(name, fontWeight = FontWeight.Medium)
                        Button(
                            onClick = { selectedService = name; step = 2 },
                            colors = ButtonDefaults.buttonColors(backgroundColor = VastuGreen, contentColor = Color.White)
                        ) {
                            Text("Book $price")
                        }
                    }
                }
            }
        } else if (step == 2) {
            Text("Available Slots for $selectedService", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Mock slots
            listOf("Today, 4:00 PM", "Today, 6:30 PM", "Tomorrow, 10:00 AM").forEach { slot ->
                OutlinedButton(
                    onClick = { step = 3 },
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
                    Text("Payment will be made via UPI. Note: Booked time slots cannot be rescheduled.", color = Color(0xFFE65100), fontSize = 13.sp)
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth(), backgroundColor = Color.White, elevation = 4.dp) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = VastuGreen, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Booking Confirmed!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = VastuGreen)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("You will receive a WhatsApp call at your selected time. Please be ready with your questions, app screenshots, and a paper & pen to note down remedies.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.DarkGray)
                }
            }
        }
    }
}
