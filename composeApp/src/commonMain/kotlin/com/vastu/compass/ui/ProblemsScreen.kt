package com.vastu.compass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.VastuGreen
import com.vastu.compass.data.VastuRepository

@Composable
fun ProblemsScreen() {
    val pkg by VastuRepository.dataFlow.collectAsState()
    val categories = pkg?.problemCategories ?: emptyList()
    val allProblems = pkg?.problems ?: emptyList()

    var searchQuery by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeigeBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "VASTU REMEDIES",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF3E2723)
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by problem, category, or remedy...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp)),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = VastuGreen,
                unfocusedBorderColor = Color.LightGray,
                backgroundColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            val filteredCategories = if (searchQuery.isBlank()) {
                categories
            } else {
                categories.filter { cat ->
                    cat.name.contains(searchQuery, ignoreCase = true) ||
                    allProblems.any { prob ->
                        prob.categoryId == cat.id && (
                            prob.title.contains(searchQuery, ignoreCase = true) ||
                            prob.cause.contains(searchQuery, ignoreCase = true) ||
                            prob.remedy.contains(searchQuery, ignoreCase = true) ||
                            prob.solution.contains(searchQuery, ignoreCase = true)
                        )
                    }
                }
            }

            items(filteredCategories) { category ->
                val isExpanded = expandedCategory == category.id || searchQuery.isNotBlank()
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            expandedCategory = if (expandedCategory == category.id) null else category.id
                        },
                    elevation = 2.dp,
                    backgroundColor = Color.White,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = category.name.uppercase(),
                            fontSize = 17.sp, 
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B) 
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                if (isExpanded) {
                    val catProblems = allProblems.filter { 
                        it.categoryId == category.id && (
                            searchQuery.isBlank() ||
                            it.title.contains(searchQuery, ignoreCase = true) ||
                            it.cause.contains(searchQuery, ignoreCase = true) ||
                            it.remedy.contains(searchQuery, ignoreCase = true) ||
                            it.solution.contains(searchQuery, ignoreCase = true)
                        )
                    }
                    
                    Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp)) {
                        catProblems.forEach { problem ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                backgroundColor = Color(0xFFF8FAFC),
                                elevation = 0.dp,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = problem.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    Text("Cause:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                    Text(problem.cause, fontSize = 16.sp, color = Color(0xFF475569), lineHeight = 24.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text("Vastu Remedy:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VastuGreen)
                                    Text(problem.remedy, fontSize = 16.sp, color = Color(0xFF334155), lineHeight = 24.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text("Optimal Solution:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                    Text(problem.solution, fontSize = 16.sp, color = Color(0xFF334155), lineHeight = 24.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
