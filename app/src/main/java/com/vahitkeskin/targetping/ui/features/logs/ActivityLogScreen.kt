package com.vahitkeskin.targetping.ui.features.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahitkeskin.targetping.ui.features.add_edit.GlassCard
import com.vahitkeskin.targetping.ui.home.HomeViewModel

private val CyberTeal = Color(0xFF00E5FF)
private val AlertRed = Color(0xFFFF2A68)
private val DarkBackground = Color(0xFF0A0A0A)

@Composable
fun ActivityLogScreen(viewModel: HomeViewModel) {
    val logs by viewModel.activityLogs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Başlık (Status Bar altı)
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.History, null, tint = CyberTeal)
            Spacer(Modifier.width(8.dp))
            Text(
                "SİSTEM GÜNLÜĞÜ",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp), // BottomBar için boşluk
            verticalArrangement = Arrangement.spacedBy(0.dp) // Timeline çizgisi için
        ) {
            items(logs) { log ->
                TimelineItem(
                    title = log.title,
                    message = log.message,
                    time = log.time,
                    isPositive = log.isPositive
                )
            }
        }
    }
}

@Composable
fun TimelineItem(title: String, message: String, time: String, isPositive: Boolean) {
    val color = if (isPositive) CyberTeal else AlertRed

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // 1. ZAMAN ÇİZGİSİ
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Çizgi
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(Color.White.copy(0.1f))
            )
            // Nokta
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, Color.Black.copy(0.5f), CircleShape)
            )
            // Çizgi
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(Color.White.copy(0.1f))
            )
        }

        // 2. İÇERİK KARTI
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Rounded.Login else Icons.Rounded.Logout,
                        contentDescription = null,
                        tint = color.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(time, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Text(message, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}