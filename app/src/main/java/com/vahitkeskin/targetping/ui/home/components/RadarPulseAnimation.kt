package com.vahitkeskin.targetping.ui.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.vahitkeskin.targetping.ui.theme.*

@Composable
fun RadarPulseAnimation(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    color: Color = PrimaryColor
) {
    if (!isPlaying) return

    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    // Halka genişleme animasyonu
    val scale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "scale"
    )

    // Halka opaklık animasyonu (dışa gittikçe kaybolur)
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "alpha"
    )

    // Dönen tarayıcı çubuğu
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ), label = "rotation"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Genişleyen halka 1
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = size.minDimension / 4 * scale,
                style = Stroke(width = 4f)
            )
        }

        // Dönen Tarayıcı (Sweep Gradient)
        Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
            val brush = Brush.sweepGradient(
                colors = listOf(Color.Transparent, color.copy(alpha = 0.5f))
            )
            drawCircle(brush = brush, radius = size.minDimension / 2)
        }
    }
}