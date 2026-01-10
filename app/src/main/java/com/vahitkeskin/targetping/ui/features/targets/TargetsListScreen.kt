package com.vahitkeskin.targetping.ui.features.targets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahitkeskin.targetping.domain.model.TargetLocation
import com.vahitkeskin.targetping.ui.components.CompactTargetItem
import com.vahitkeskin.targetping.ui.home.HomeViewModel

private val CyberTeal = Color(0xFF00E5FF)

@Composable
fun TargetsListScreen(
    viewModel: HomeViewModel,
    onNavigateToMap: (TargetLocation) -> Unit,
    onEditTarget: (TargetLocation) -> Unit
) {
    val targets by viewModel.targets.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Başlık
        Text(
            text = "TARGET MANAGEMENT",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "TOTAL: ${targets.size} | ACTIVE: ${targets.count { it.isActive }}",
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Liste
        if (targets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("NO DATA FOUND", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(targets, key = { it.id }) { target ->
                    // Kartın Kendisi (Swipe-to-Delete eklenebilir ama şimdilik basit tutuyoruz)
                    CompactTargetItem(
                        target = target,
                        userLocation = null, // Listede uzaklık hesabı zorunlu değil
                        onToggle = { viewModel.toggleTargetActive(target.id, !target.isActive) },
                        onClick = { onEditTarget(target) } // Tıklayınca Düzenlemeye gitsin
                    )
                }
            }
        }
    }
}