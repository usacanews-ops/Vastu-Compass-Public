package com.vastu.compass.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.Saffron
import com.vastu.compass.VastuGreen
import com.vastu.compass.data.VastuRepository
import com.vastu.compass.util.ShareHelper
import kotlinx.coroutines.launch

@Composable
fun MoreScreen() {
    val pkg by VastuRepository.dataFlow.collectAsState()
    val products by VastuRepository.productsFlow.collectAsState()
    val isAutoSyncing by VastuRepository.isSyncing.collectAsState()
    val scope = rememberCoroutineScope()
    var syncMessage by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    val shareHelper = remember { ShareHelper() }

    val termsUrl = "https://myfreetools.app/wp-content/plugins/apps/vastuconsultant/TermsandConditions.php"
    val privacyUrl = "https://myfreetools.app/wp-content/plugins/apps/vastuconsultant/privacy_policy.php"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeigeBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SETTINGS & INFO",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.height(20.dp))

        // 1. Database Sync Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color.White,
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            syncMessage = "Syncing..."
                            syncMessage = VastuRepository.syncWithLiveServer()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Saffron,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isAutoSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Syncing Live Database...")
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = "Sync")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync Database (v${pkg?.version ?: "0"})")
                    }
                }

                if (syncMessage.isNotEmpty()) {
                    Text(
                        text = syncMessage,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                        color = if (syncMessage.contains("Failed")) Color(0xFFDC2626) else VastuGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Navigation Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color.White,
            elevation = 2.dp
        ) {
            Column {
                ListTile(
                    title = "Share App",
                    icon = Icons.Default.Share
                ) {
                    shareHelper.shareApp(
                        text = "Check out Vastu Compass with 16 zones, 32 Padas, and expert solutions: https://myfreetools.app/wp-content/plugins/apps/vastuconsultant/",
                        title = "Share Vastu Compass App"
                    )
                }
                Divider(color = Color(0xFFF1F5F9))

                ListTile(
                    title = "Terms & Conditions",
                    icon = Icons.Default.Description
                ) {
                    try { uriHandler.openUri(termsUrl) } catch (_: Exception) {}
                }
                Divider(color = Color(0xFFF1F5F9))

                ListTile(
                    title = "Privacy Policy",
                    icon = Icons.Default.PrivacyTip
                ) {
                    try { uriHandler.openUri(privacyUrl) } catch (_: Exception) {}
                }
                Divider(color = Color(0xFFF1F5F9))

                ListTile(
                    title = "Contact Support",
                    icon = Icons.Default.Email
                ) {
                    try { uriHandler.openUri("mailto:info@itwebsolutions.ca") } catch (_: Exception) {}
                }
            }
        }

        // 3. Products Section
        if (products.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Saffron, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VASTU REMEDY PRODUCTS",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            products.forEach { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = Color.White,
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = product.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(1f)
                            )
                            if (product.price.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFECFDF5)
                                ) {
                                    Text(
                                        text = product.price,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = VastuGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (product.category.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = product.category.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Saffron
                            )
                        }

                        if (product.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = product.description,
                                fontSize = 12.5.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 17.sp
                            )
                        }

                        if (product.buyUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    try { uriHandler.openUri(product.buyUrl) } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = VastuGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View Remedy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Footer
        Text("© 2026 IT Web Solutions", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
        Text("Brampton, Canada", fontSize = 11.sp, color = Color(0xFF94A3B8))
        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
fun ListTile(title: String, icon: ImageVector, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = VastuGreen, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color(0xFF334155), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(20.dp))
        }
    }
}
