package com.vastu.compass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ProblemItem(val title: String, val subtitle: String, val icon: ImageVector, val color: Color)

@Composable
fun ProblemsScreen() {
    val problems = listOf(
        ProblemItem("Marriage & Love", "Relationship harmony", Icons.Default.Favorite, Color(0xFFD84315)),
        ProblemItem("Children", "Solutions for children", Icons.Default.ChildCare, Color(0xFF2E7D32)),
        ProblemItem("Personal Problems", "Find solutions for life", Icons.Default.PersonOutline, Color(0xFF6A1B9A)),
        ProblemItem("Career", "Growth & job opportunities", Icons.Default.TrendingUp, Color(0xFF1565C0)),
        ProblemItem("Finance", "Attract wealth & prosperity", Icons.Default.AttachMoney, Color(0xFFF57F17)),
        ProblemItem("Education", "Learn Vastu principles", Icons.Default.MenuBook, Color(0xFF4527A0))
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3EEEA)).padding(16.dp)) {
        Text("PROBLEM WITH REMEDIES", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(problems) { problem ->
                Card(
                    modifier = Modifier.aspectRatio(0.9f),
                    shape = RoundedCornerShape(12.dp),
                    elevation = 2.dp,
                    backgroundColor = problem.color.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(problem.icon, contentDescription = problem.title, tint = problem.color, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(problem.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = problem.color, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(problem.subtitle, fontSize = 12.sp, color = Color(0xFF424242), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
