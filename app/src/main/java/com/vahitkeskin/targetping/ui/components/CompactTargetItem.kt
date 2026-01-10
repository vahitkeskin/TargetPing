package com.vahitkeskin.targetping.ui.components

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vahitkeskin.targetping.domain.model.TargetLocation

// Renkler
private val CyberTeal = Color(0xFF00E5FF)
private val CardBackground = Color(0xFF1E1E1E)

@Composable
fun CompactTargetItem(
    target: TargetLocation,
    userLocation: Location?, // Uzaklık hesaplamak için (Haritada var, listede null olabilir)
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    // Mesafe Hesabı (Eğer konum bilgisi geldiyse)
    val distanceStr = remember(userLocation, target) {
        if (userLocation == null) "---"
        else {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude,
                userLocation.longitude,
                target.latitude,
                target.longitude,
                results
            )
            val dist = results[0].toInt()
            if (dist > 1000) "${String.format("%.1f", dist / 1000f)} KM" else "$dist M"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sol İkon Kutusu
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (target.isActive) CyberTeal.copy(0.1f) else Color.Gray.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Radar,
                contentDescription = null,
                tint = if (target.isActive) CyberTeal else Color.Gray
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Orta Bilgi Alanı
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = target.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Eğer konum varsa mesafeyi göster
                if (userLocation != null) {
                    Icon(Icons.Rounded.Straighten, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = distanceStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTeal,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = " • ", color = Color.Gray)
                }

                // Yarıçap Bilgisi
                Text(
                    text = "RAD: ${target.radiusMeters}M",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        // Sağ Switch Butonu
        Switch(
            checked = target.isActive,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = CyberTeal,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Black,
                uncheckedBorderColor = Color.Gray
            ),
            modifier = Modifier.scale(0.8f)
        )
    }
}