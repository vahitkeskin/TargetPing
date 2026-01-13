package com.vahitkeskin.targetping.ui.components

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vahitkeskin.targetping.domain.model.TargetLocation
import com.vahitkeskin.targetping.ui.theme.PrimaryColor
import com.vahitkeskin.targetping.ui.theme.SurfaceColor

@Composable
fun CompactTargetItem(
    modifier: Modifier = Modifier, // Dışarıdan gelen modifier (Animasyon vb. için)
    target: TargetLocation,
    userLocation: Location?,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    // Mesafe Hesabı
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

    // Root element (Row) artık dışarıdan gelen modifier'ı kullanıyor
    Row(
        modifier = modifier // <--- DÜZELTME BURADA: Dışarıdan gelen modifier eklendi
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
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
                .background(if (target.isActive) PrimaryColor.copy(0.15f) else Color.Gray.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Radar,
                contentDescription = null,
                tint = if (target.isActive) PrimaryColor else Color.Gray
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Orta Bilgi Alanı
        Column(modifier = Modifier.weight(1f)) {
            // İsim (Taşmayı önlemek için maxLines eklendi)
            Text(
                text = target.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1, // Uzun isimleri kes
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Konum varsa mesafe
                if (userLocation != null) {
                    Icon(Icons.Rounded.Straighten, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = distanceStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = " • ", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }

                // Yarıçap
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
                checkedTrackColor = PrimaryColor,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Black,
                uncheckedBorderColor = Color.Gray
            ),
            modifier = Modifier.scale(0.8f)
        )
    }
}