package com.vastu.compass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.VastuGreen
import com.vastu.compass.data.VastuRepository

@Composable
fun ZonesScreen(onItemClick: (String) -> Unit) {
    val pkg by VastuRepository.dataFlow.collectAsState()
    val items = pkg?.items ?: emptyList()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(items) {
        listOf("All") + items.map { it.category }.filter { it.isNotBlank() }.distinct()
    }

    val filteredItems = remember(items, searchQuery, selectedCategory) {
        items.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeigeBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "ZONES & UTILITIES",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF3E2723)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search bedroom, kitchen, water...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = VastuGreen,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                backgroundColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        if (categories.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) VastuGreen else Color.White,
                        elevation = if (isSelected) 2.dp else 1.dp,
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 3-Column Utilities Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 72.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredItems) { item ->
                Card(
                    modifier = Modifier
                        .aspectRatio(0.88f)
                        .clickable { onItemClick(item.id) },
                    shape = RoundedCornerShape(12.dp),
                    elevation = 2.dp,
                    backgroundColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ItemIcon(
                            iconUrl = item.iconUrl.ifBlank { null },
                            fallbackKey = item.iconKey.ifBlank { item.id },
                            contentDescription = item.title,
                            modifier = Modifier.size(34.dp),
                            tint = Color(0xFF8D6E63)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.title,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF334155),
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
