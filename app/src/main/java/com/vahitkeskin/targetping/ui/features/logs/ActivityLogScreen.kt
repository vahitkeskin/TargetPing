package com.vahitkeskin.targetping.ui.features.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahitkeskin.targetping.data.local.entity.LogEntity
import com.vahitkeskin.targetping.data.local.entity.LogEventType
import com.vahitkeskin.targetping.ui.features.add_edit.GlassCard
import com.vahitkeskin.targetping.ui.home.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

private val CyberTeal = Color(0xFF00E5FF)
private val AlertRed = Color(0xFFFF2A68)
private val DarkBackground = Color(0xFF0A0A0A)

@Composable
fun ActivityLogScreen(viewModel: HomeViewModel) {
    // Room'dan gelen canlı veriyi dinliyoruz
    val logs by viewModel.logs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        // --- BAŞLIK ALANI ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.History, null, tint = CyberTeal)
                Spacer(Modifier.width(8.dp))
                Text(
                    "SİSTEM GÜNLÜĞÜ",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // BUTON GRUBU
            Row {
                // 1. TEST SİMÜLASYON BUTONU (addTestLog BURADA KULLANILIYOR)
                IconButton(onClick = { viewModel.addTestLog() }) {
                    Icon(
                        imageVector = Icons.Rounded.BugReport, // Test/Debug ikonu
                        contentDescription = "Test Verisi Ekle",
                        tint = CyberTeal.copy(alpha = 0.7f)
                    )
                }

                // 2. TEMİZLEME BUTONU
                if (logs.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Günlüğü Temizle",
                            tint = AlertRed.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Toplam Kayıt: ${logs.size}",
            color = Color.Gray,
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(Modifier.height(16.dp))

        // --- LİSTE ALANI ---
        if (logs.isEmpty()) {
            EmptyLogState(onTestClick = { viewModel.addTestLog() })
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    LogItemCard(log)
                }
            }
        }
    }
}

@Composable
fun LogItemCard(log: LogEntity) {
    val isEntry = log.eventType == LogEventType.ENTRY
    val iconColor = if (isEntry) CyberTeal else AlertRed
    val icon = if (isEntry) Icons.Rounded.Login else Icons.Rounded.Logout

    // Tarih Formatlama
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateString = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }
    val fullDate = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(log.timestamp)) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. ZAMAN DAMGASI (Sol Taraf)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(60.dp)
            ) {
                Text(
                    text = dateString,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = fullDate,
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp
                )
            }

            // Dikey Çizgi
            Divider(
                modifier = Modifier
                    .height(40.dp)
                    .padding(horizontal = 12.dp)
                    .width(1.dp),
                color = Color.White.copy(alpha = 0.1f)
            )

            // 2. İKON VE DETAY (Orta ve Sağ)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isEntry) "BÖLGEYE GİRİŞ" else "BÖLGEDEN ÇIKIŞ",
                        color = iconColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    if (log.eventType == LogEventType.SYSTEM) {
                        Text("(SİSTEM)", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = log.targetName,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }

            // Durum İkonu
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(32.dp)
                    .background(iconColor.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small)
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun EmptyLogState(onTestClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.3f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Sistem Temiz",
                color = Color.Gray,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Henüz kaydedilmiş bir aktivite yok.",
                color = Color.Gray.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(24.dp))

            // BOŞ EKRANDA DA TEST BUTONU OLSUN
            Button(
                onClick = onTestClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Test Kaydı Oluştur", color = CyberTeal)
            }
        }
    }
}

// Divider Helper
@Composable
fun Divider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}