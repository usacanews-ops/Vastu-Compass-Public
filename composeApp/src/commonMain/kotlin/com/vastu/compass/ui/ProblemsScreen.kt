package com.vastu.compass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.VastuGreen
import com.vastu.compass.data.VastuRepository

@Composable
fun ProblemsScreen() {
    val pkg by VastuRepository.dataFlow.collectAsState()
    val categories = pkg?.problemCategories ?: emptyList()
    var searchQuery by remember { mutableStateOf("") }

    val filteredProblems = remember(searchQuery, pkg) {
        val allProblems = pkg?.problems ?: emptyList()
        if (searchQuery.isBlank()) {
            allProblems
        } else {
            allProblems.filter { problem ->
                val catName = categories.find { it.id == problem.categoryId }?.name.orEmpty()
                problem.title.contains(searchQuery, ignoreCase = true) ||
                problem.cause.contains(searchQuery, ignoreCase = true) ||
                problem.solution.contains(searchQuery, ignoreCase = true) ||
                problem.remedy.contains(searchQuery, ignoreCase = true) ||
                catName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BeigeBackground).padding(16.dp)
    ) {
        Text("VASTU PROBLEMS", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search problems, zones, or remedies...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = VastuGreen) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = Color.White,
                focusedBorderColor = VastuGreen,
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredProblems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No problems match your search.", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredProblems) { problem ->
                    val catName = categories.find { it.id == problem.categoryId }?.name ?: "GENERAL"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = Color.White,
                        elevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = catName.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = VastuGreen,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = problem.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A),
                                lineHeight = 26.sp
                            )
                            
                            Spacer(modifier = Modifier.height(14.dp))

                            Text("Root Cause:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Text(problem.cause, fontSize = 16.sp, color = Color(0xFF334155), lineHeight = 24.sp)
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F8F5),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Vastu Solution:", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = VastuGreen)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(problem.remedy, fontSize = 16.sp, color = Color(0xFF1B4D3E), lineHeight = 24.sp, modifier = Modifier.padding(bottom = 6.dp))
                                    Text(problem.solution, fontSize = 16.sp, color = Color(0xFF1B4D3E), lineHeight = 24.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
