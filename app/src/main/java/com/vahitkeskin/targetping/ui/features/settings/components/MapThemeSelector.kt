package com.vahitkeskin.targetping.ui.features.settings.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vahitkeskin.targetping.domain.model.MapStyleConfig
import com.vahitkeskin.targetping.ui.theme.PrimaryColor // Kendi temanızdan

@Composable
fun MapThemeSelector(
    currentStyle: MapStyleConfig,
    onStyleSelected: (MapStyleConfig) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp), // Kartlar arası boşluk
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(MapStyleConfig.entries) { style ->
                MapStyleItem(
                    style = style,
                    isSelected = style == currentStyle,
                    onClick = { onStyleSelected(style) }
                )
            }
        }
    }
}

@Composable
private fun MapStyleItem(
    style: MapStyleConfig,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Seçilince %10 büyüme animasyonu
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1f, label = "scaleAnim")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        // --- GÖRSEL ALANI ---
        Box(
            modifier = Modifier
                .size(72.dp) // Daire boyutu
                .clip(CircleShape)
                // Eğer seçiliyse etrafına PrimaryColor border ekle
                .then(
                    if (isSelected) Modifier.border(3.dp, PrimaryColor, CircleShape)
                    else Modifier.border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            // 1. HARİTA RESMİ
            Image(
                painter = painterResource(id = style.previewImage),
                contentDescription = style.title,
                contentScale = ContentScale.Crop, // Resmi daireye doldur
                modifier = Modifier.fillMaxSize()
            )

            // 2. SEÇİLİ EFEKTİ (Overlay + Icon)
            if (isSelected) {
                // Resmin üzerine yarı saydam siyah katman atalım ki TİK işareti parlasın
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )

                // Tik İkonu
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = PrimaryColor,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .padding(6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- BAŞLIK ---
        Text(
            text = style.title,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) PrimaryColor else Color.White.copy(alpha = 0.7f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}