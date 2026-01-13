package com.vahitkeskin.targetping.ui.features.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import com.vahitkeskin.targetping.ui.theme.*

@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit = {}
) {
    // --- GENEL ANİMASYON GEÇİŞLERİ ---
    val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")

    // 1. GRID AKIŞI (Arka plan için)
    // 0'dan 50px'e kadar sayar, sonra başa döner. Bu sonsuz akış illüzyonu yaratır.
    val gridOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f, // Izgara kare boyutu kadar
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ), label = "gridOffset"
    )

    // 2. RADAR DÖNÜŞÜ
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ), label = "rotation"
    )

    // 3. PULSE VE DİĞERLERİ
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    var textAlpha by remember { mutableStateOf(0f) }
    val lockScale = remember { Animatable(3f) }

    // --- ZAMANLAMA ---
    LaunchedEffect(true) {
        launch {
            lockScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(800, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))
            )
            delay(200)
            textAlpha = 1f
        }
        delay(2500)
        onAnimationFinished()
    }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        // ---------------------------------------------------------------------
        // KATMAN 1: HAREKETLİ TAKTİKSEL IZGARA (ARKA PLAN)
        // ---------------------------------------------------------------------
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val gridSize = 100f // Kare boyutu

            // Vignette (Kenarları Karartma) Fırçası
            val vignetteBrush = Brush.radialGradient(
                colors = listOf(Color.Transparent, DarkBackground),
                center = center,
                radius = size.minDimension / 1.2f
            )

            // A) DİKEY ÇİZGİLER (Sabit)
            // Ekran genişliğince çizgi sayısı
            val verticalLines = ceil(canvasWidth / gridSize).toInt()
            for (i in 0..verticalLines) {
                val x = i * gridSize
                // Merkeze yaklaştıkça daha görünür, kenarlarda silik
                val alpha = if (x > canvasWidth / 3 && x < canvasWidth * 2 / 3) 0.1f else 0.03f
                drawLine(
                    color = PrimaryColor.copy(alpha = alpha),
                    start = Offset(x, 0f),
                    end = Offset(x, canvasHeight),
                    strokeWidth = 1f
                )
            }

            // B) YATAY ÇİZGİLER (Hareketli - Waterfall Efekti)
            // gridOffsetY değerini kullanarak çizgileri aşağı kaydırıyoruz
            val horizontalLines = ceil(canvasHeight / gridSize).toInt()
            for (i in -1..horizontalLines) {
                val y = (i * gridSize) + gridOffsetY
                // Sadece ekran içindeyse çiz (Performans)
                if (y in 0f..canvasHeight) {
                    drawLine(
                        color = PrimaryColor.copy(alpha = 0.05f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1f
                    )
                }
            }

            // C) VIGNETTE EFEKTİ (Grid'in üzerine, Radar'ın altına)
            // Bu, ızgarayı kenarlarda karartarak derinlik hissi verir.
            drawRect(brush = vignetteBrush)
        }


        // ---------------------------------------------------------------------
        // KATMAN 2: RADAR, HEDEF VE ARAYÜZ (ÖN PLAN)
        // ---------------------------------------------------------------------
        Canvas(modifier = Modifier.size(220.dp)) {
            val center = this.center
            val radius = size.minDimension / 2

            // 1. RADAR TARAMASI (Dönen Yeşil Alan)
            rotate(rotation) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.75f to Color.Transparent,
                        1.0f to PrimaryColor.copy(alpha = 0.4f)
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = true,
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(center.x - radius, center.y - radius)
                )
            }

            // 2. SABİT HALKALAR
            drawCircle(
                color = PrimaryColor.copy(alpha = 0.3f),
                radius = radius,
                style = Stroke(width = 1.5f)
            )
            drawCircle(
                color = PrimaryColor.copy(alpha = 0.1f),
                radius = radius * 0.6f,
                style = Stroke(width = 1.5f)
            )

            // 3. MERKEZ HEDEF (Kırmızı Yanıp Sönen Nokta)
            drawCircle(
                color = AlertRed,
                radius = 6.dp.toPx() * pulse,
                style = androidx.compose.ui.graphics.drawscope.Fill
            )
            // Hedefin etrafına hafif bir "Glow" (Parıltı) efekti
            drawCircle(
                color = AlertRed.copy(alpha = 0.3f),
                radius = 12.dp.toPx() * pulse,
                style = androidx.compose.ui.graphics.drawscope.Fill
            )

            // 4. HEDEF KİLİTLEME KÖŞELERİ (Crosshair)
            val crossSize = radius * 1.2f * lockScale.value
            val cornerLength = 30.dp.toPx()
            val strokeWidth = 3.dp.toPx()

            // Çizim fonksiyonu tekrarı azaltmak için (local helper)
            fun drawCorner(offsetX: Float, offsetY: Float, line1End: Offset, line2End: Offset) {
                val start = Offset(center.x + offsetX, center.y + offsetY)
                drawLine(PrimaryColor, start, line1End, strokeWidth, StrokeCap.Round)
                drawLine(PrimaryColor, start, line2End, strokeWidth, StrokeCap.Round)
            }

            // Sol Üst
            drawCorner(
                -crossSize, -crossSize,
                Offset(center.x - crossSize, center.y - crossSize + cornerLength),
                Offset(center.x - crossSize + cornerLength, center.y - crossSize)
            )
            // Sağ Üst
            drawCorner(
                crossSize, -crossSize,
                Offset(center.x + crossSize, center.y - crossSize + cornerLength),
                Offset(center.x + crossSize - cornerLength, center.y - crossSize)
            )
            // Sol Alt
            drawCorner(
                -crossSize, crossSize,
                Offset(center.x - crossSize, center.y + crossSize - cornerLength),
                Offset(center.x - crossSize + cornerLength, center.y + crossSize)
            )
            // Sağ Alt
            drawCorner(
                crossSize, crossSize,
                Offset(center.x + crossSize, center.y + crossSize - cornerLength),
                Offset(center.x + crossSize - cornerLength, center.y + crossSize)
            )

            // 5. METİN VE SİSTEM BİLGİSİ
            if (textAlpha > 0f) {
                val text = "TARGET PING"
                val style = TextStyle(
                    color = Color.White.copy(alpha = textAlpha),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )

                val textResult = textMeasurer.measure(text, style)
                drawText(
                    textResult,
                    topLeft = Offset(
                        center.x - textResult.size.width / 2,
                        center.y + radius + 24.dp.toPx()
                    )
                )

                // Alt bilgi: Rastgele koordinat gibi görünen dekoratif yazı
                val subText = "SİSTEM BAŞLATILDI"
                val subStyle = TextStyle(
                    color = PrimaryColor.copy(alpha = textAlpha * 0.6f),
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, // Monospace daha teknik durur
                    letterSpacing = 1.sp
                )
                val subResult = textMeasurer.measure(subText, subStyle)

                drawText(
                    subResult,
                    topLeft = Offset(
                        center.x - subResult.size.width / 2,
                        center.y + radius + 50.dp.toPx()
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun SplashScreenPreview() {
    SplashScreen()
}