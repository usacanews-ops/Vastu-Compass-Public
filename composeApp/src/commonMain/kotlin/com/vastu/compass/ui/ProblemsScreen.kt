package com.vastu.compass.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.data.VastuRepository
import com.vastu.compass.model.ProblemCategory

@Composable
fun ProblemsScreen() {
    val pkg by VastuRepository.dataFlow.collectAsState()
    var selectedCategory by remember { mutableStateOf<ProblemCategory?>(null) }

    if (selectedCategory == null) {
        // MAIN CATEGORIES VIEW
        val categories = pkg?.problemCategories ?: emptyList()
        Column(modifier = Modifier.fillMaxSize().background(BeigeBackground).padding(16.dp)) {
            Text("PROBLEM CATEGORIES", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3E2723))
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(categories.size) { index ->
                    val cat = categories[index]
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedCategory = cat },
                        shape = RoundedCornerShape(12.dp),
                        elevation = 2.dp,
                        backgroundColor = Color.White
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(cat.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    } else {
        // DETAIL VIEW (Matches "1000386520.jpg")
        val allProblems = pkg?.problems ?: emptyList()
        val categoryProblems = allProblems.filter { it.categoryId == selectedCategory?.id }
        var searchQuery by remember { mutableStateOf("") }
        var expandedProblemId by remember { mutableStateOf<String?>(null) }

        // Pastel color rotation matching your screenshot
        val pastelColors = listOf(
            Color(0xFFFFF0F5), // Light Pink
            Color(0xFFE8F5E9), // Light Green
            Color(0xFFFFF3E0), // Light Orange/Peach
            Color(0xFFE3F2FD), // Light Blue
            Color(0xFFFFF8E1), // Light Yellow
            Color(0xFFF3E5F5)  // Light Purple
        )

        Column(modifier = Modifier.fillMaxSize().background(BeigeBackground).padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { selectedCategory = null },
                    modifier = Modifier.background(Color.White, RoundedCornerShape(50)).size(40.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color(0xFF3E2723))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(selectedCategory!!.name.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723), modifier = Modifier.weight(1f))
                Icon(Icons.Default.CompassCalibration, contentDescription = "Compass Logo", tint = Color(0xFFD4AF37), modifier = Modifier.size(32.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)),
                placeholder = { Text("Search problems...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color(0xFFD4AF37)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Expandable List
            val filteredList = categoryProblems.filter { it.title.contains(searchQuery, ignoreCase = true) }
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                itemsIndexed(filteredList) { index, item ->
                    val isExpanded = expandedProblemId == item.id
                    val bgColor = pastelColors[index % pastelColors.size]
                    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgColor)
                            .clickable { expandedProblemId = if (isExpanded) null else item.id }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                tint = Color.DarkGray,
                                modifier = Modifier.rotate(rotationState)
                            )
                        }

                        // Expandable Content: Causes & Remedies
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
                                Divider(color = Color.Black.copy(alpha = 0.05f), modifier = Modifier.padding(bottom = 12.dp))
                                
                                if (item.cause.isNotBlank()) {
                                    Text("Root Cause:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                    Text(item.cause, fontSize = 13.sp, color = Color(0xFF334155), modifier = Modifier.padding(bottom = 8.dp))
                                }
                                if (item.remedy.isNotBlank()) {
                                    Text("Remedy / Treatment:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                    Text(item.remedy, fontSize = 13.sp, color = Color(0xFF334155), modifier = Modifier.padding(bottom = 8.dp))
                                }
                                if (item.solution.isNotBlank()) {
                                    Text("Ideal Solution:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                    Text(item.solution, fontSize = 13.sp, color = Color(0xFF334155))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
