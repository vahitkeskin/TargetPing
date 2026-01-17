package com.vahitkeskin.targetping.ui.features.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import com.vahitkeskin.targetping.ui.home.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.vahitkeskin.targetping.ui.theme.*

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
                Icon(Icons.Rounded.History, null, tint = PrimaryColor)
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
                        tint = PrimaryColor.copy(alpha = 0.7f)
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
    val statusColor = when (log.eventType) {
        LogEventType.ENTRY -> PrimaryColor
        LogEventType.EXIT -> AlertRed
        else -> Color(0xFFFFC107)
    }

    val icon = if (isEntry) Icons.Rounded.Login else Icons.Rounded.Logout

    // Formatlayıcılar
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val secondsFormat =
        remember { SimpleDateFormat(":ss", Locale.getDefault()) } // ':' başa eklendi
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    val date = Date(log.timestamp)
    val timeStr = remember(log.timestamp) { timeFormat.format(date) }
    val secondsStr = remember(log.timestamp) { secondsFormat.format(date) }
    val dateStr = remember(log.timestamp) { dateFormat.format(date) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E).copy(alpha = 0.6f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // 1. SOL STATUS ÇUBUĞU
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(statusColor)
            )

            // 2. İÇERİK
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // --- KESİN ÇÖZÜM ALANI ---
                Column(
                    modifier = Modifier
                        .width(88.dp) // Genişlik optimize edildi (88dp idealdir)
                        .fillMaxHeight()
                        .padding(start = 10.dp), // İç boşluk biraz kısıldı
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    // TEK TEXT İÇİNDE STİL AYRIŞTIRMA (SpanStyle)
                    // Bu yöntem kaymayı %100 engeller.
                    Text(
                        text = buildAnnotatedString {
                            // Saat Kısmı (Büyük ve Beyaz)
                            withStyle(
                                style = SpanStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    letterSpacing = (-0.5).sp
                                )
                            ) {
                                append(timeStr)
                            }
                            // Saniye Kısmı (Küçük ve Gri)
                            withStyle(
                                style = SpanStyle(
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            ) {
                                append(secondsStr)
                            }
                        },
                        maxLines = 1,
                        softWrap = false // Alt satıra geçmeyi yasaklar
                    )

                    // Tarih
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = Color.Gray.copy(alpha = 0.6f),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Dikey Ayırıcı
                Divider(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .height(32.dp)
                        .width(1.dp),
                    color = Color.White.copy(alpha = 0.1f)
                )

                // ORTA BİLGİ
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = log.targetName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isEntry) "ERİŞİM İZNİ" else "BAĞLANTI KESİLDİ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = statusColor
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "#${log.id}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 9.sp
                            ),
                            color = Color.Gray
                        )
                    }
                }

                // SAĞ İKON
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(statusColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
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
                Text("Test Kaydı Oluştur", color = PrimaryColor)
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