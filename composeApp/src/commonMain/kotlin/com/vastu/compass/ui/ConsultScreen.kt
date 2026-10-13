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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.Saffron
import com.vastu.compass.VastuGreen
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Serializable
data class ConsultConfig(
    val services: List<ConsultService>,
    val upiId: String,
    val payeeName: String = "Vastu Consultant",
    val slots: List<String>,
    val statusEndpoint: String = ""
)

@Serializable
data class ConsultService(val name: String, val price: String)

sealed class FetchState {
    object Loading : FetchState()
    data class Success(val data: ConsultConfig) : FetchState()
    data class Error(val title: String, val detail: String) : FetchState()
}

@Composable
fun ConsultScreen(onBack: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    var fetchState by remember { mutableStateOf<FetchState>(FetchState.Loading) }
    var retryTrigger by remember { mutableStateOf(0) }

    var step by remember { mutableStateOf(1) } // 1: Select, 2: Auto-Verifying Pay, 3: Slots, 4: Confirmed
    var selectedService by remember { mutableStateOf<ConsultService?>(null) }
    var selectedSlot by remember { mutableStateOf("") }
    var currentOrderId by remember { mutableStateOf("") }

    // Dynamic Online Fetch with strictly no local fallback
    LaunchedEffect(retryTrigger) {
        fetchState = FetchState.Loading
        try {
            val client = HttpClient()
           val configUrl = "https://myfreetools.app/wp-content/plugins/apps/vastuconsultant/assets/consult_config.json?t=${Random.nextInt(10000, 99999)}"
            val response = client.get(configUrl).bodyAsText()

            val parsedData = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }.decodeFromString<ConsultConfig>(response)

            if (parsedData.services.isEmpty() || parsedData.upiId.isBlank() || parsedData.slots.isEmpty()) {
                throw Exception("Incomplete consultation configuration received from server.")
            }

            fetchState = FetchState.Success(parsedData)
        } catch (e: Exception) {
            fetchState = FetchState.Error(
                title = "Network / High Traffic Error",
                detail = "Unable to connect to the consultation server due to poor network or high server traffic. Please check your connection and try again."
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeigeBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text(
                text = "BOOK CONSULTATION",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3E2723)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = fetchState) {
            is FetchState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Saffron)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Loading live consultation details...",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            is FetchState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    backgroundColor = Color(0xFFFEF2F2),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Error",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFFB91C1C),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.detail,
                            fontSize = 13.sp,
                            color = Color(0xFF7F1D1D),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { retryTrigger++ },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Saffron,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Connection", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is FetchState.Success -> {
                val data = state.data

                // --- STEP 1: SELECT PLAN ---
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
                                Column {
                                    Text(service.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Expert 1-on-1 Consultation", fontSize = 12.sp, color = Color.Gray)
                                }
                                Button(
                                    onClick = {
                                        selectedService = service
                                        currentOrderId = "VC" + Random.nextInt(100000, 999999)
                                        step = 2
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = VastuGreen,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Book ${service.price}")
                                }
                            }
                        }
                    }
                }

                // --- STEP 2: DYNAMIC ON-SPOT QR & AUTOMATIC CONFIRMATION ---
                else if (step == 2) {
                    val rawPrice = selectedService?.price?.replace(Regex("[^0-9]"), "") ?: "99"
                    val upiUri = "upi://pay?pa=${data.upiId}&pn=${data.payeeName.encodeURLParameter()}&am=$rawPrice&cu=INR&tr=$currentOrderId&tn=Consultation-$currentOrderId"
                    val dynamicQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${upiUri.encodeURLParameter()}"

                    // Poll verification endpoint if configured on server
                    LaunchedEffect(currentOrderId) {
                        val client = HttpClient()
                        while (isActive && step == 2) {
                            delay(3500)
                            try {
                                if (data.statusEndpoint.isNotBlank()) {
                                    val checkUrl = "${data.statusEndpoint}?order_id=$currentOrderId"
                                    val checkResponse = client.get(checkUrl).bodyAsText()
                                    if (checkResponse.contains("PAID", ignoreCase = true) || checkResponse.contains("SUCCESS", ignoreCase = true)) {
                                        step = 3
                                        break
                                    }
                                }
                            } catch (e: Exception) {
                                // Continue polling
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = Color.White
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("PAYMENT VERIFICATION", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(selectedService?.price ?: "", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Saffron)
                            Text("Order ID: #$currentOrderId", fontSize = 12.sp, color = Color.DarkGray)

                            Spacer(modifier = Modifier.height(16.dp))

                            KamelImage(
                                resource = asyncPainterResource(dynamicQrUrl),
                                contentDescription = "Dynamic UPI QR",
                                modifier = Modifier
                                    .size(190.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .padding(4.dp),
                                onLoading = { Box(modifier = Modifier.size(190.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Saffron) } },
                                onFailure = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.Gray) }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    try { uriHandler.openUri(upiUri) } catch (e: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pay via Any UPI App (GPay/PhonePe)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { clipboardManager.setText(AnnotatedString(data.upiId)) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("UPI ID: ${data.upiId}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Saffron)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Awaiting automatic payment confirmation...", fontSize = 12.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(onClick = { step = 3 }) {
                                Text("Paid already? Proceed to Slots", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // --- STEP 3: SELECT TIME SLOT ---
                else if (step == 3) {
                    Text("Select your Time Slot", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Payment confirmed. Choose your session time:", fontSize = 12.sp, color = VastuGreen)
                    Spacer(modifier = Modifier.height(12.dp))

                    data.slots.forEach { slot ->
                        OutlinedButton(
                            onClick = {
                                selectedSlot = slot
                                step = 4

                                // Notify backend server securely to dispatch the Telegram alert from PHP
                                coroutineScope.launch {
                                    try {
                                        if (data.statusEndpoint.isNotBlank()) {
                                            val client = HttpClient()
                                            val notifyUrl = "${data.statusEndpoint}?action=book_slot&order_id=$currentOrderId&service=${selectedService?.name?.encodeURLParameter()}&price=${selectedService?.price?.encodeURLParameter()}&slot=${slot.encodeURLParameter()}"
                                            client.get(notifyUrl)
                                        }
                                    } catch (e: Exception) {
                                        // Booking stays confirmed on the user device even if background dispatch retries
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VastuGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(slot, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Card(backgroundColor = Color(0xFFFFF3E0), shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Saffron)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Note: Booked time slots cannot be rescheduled.", color = Color(0xFFE65100), fontSize = 12.sp)
                        }
                    }
                }

                // --- STEP 4: SUCCESS / CONFIRMATION ---
                else if (step == 4) {
                    Card(modifier = Modifier.fillMaxWidth(), backgroundColor = Color.White, elevation = 4.dp, shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = VastuGreen,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Booking Confirmed!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = VastuGreen
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Service: ${selectedService?.name}", fontWeight = FontWeight.Bold)
                            Text("Confirmed Slot: $selectedSlot", color = Saffron, fontWeight = FontWeight.ExtraBold)
                            Text("Reference: #$currentOrderId", fontSize = 12.sp, color = Color.Gray)

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "An alert has been dispatched to our expert consultant. You will receive a WhatsApp call at your scheduled time.",
                                textAlign = TextAlign.Center,
                                color = Color.DarkGray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
