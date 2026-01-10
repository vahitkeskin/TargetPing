package com.vahitkeskin.targetping.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate // Canvas içi döndürme için
import androidx.compose.ui.graphics.graphicsLayer // Modifier için döndürme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahitkeskin.targetping.domain.model.TargetLocation

private val CyberTeal = Color(0xFF00E5FF)
private val AlertRed = Color(0xFFFF2A68)
private val DarkSurface = Color(0xFF1E1E1E).copy(alpha = 0.9f)

@Composable
fun CompassOverlay(
    target: TargetLocation,
    userLocation: Location?,
    onStopNavigation: () -> Unit
) {
    val context = LocalContext.current

    // Sensör verileri
    var azimuth by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, gravity, 0, event.values.size)
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
                }
                val R = FloatArray(9)
                val I = FloatArray(9)
                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(R, orientation)
                    var azim = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    if (azim < 0) azim += 360f
                    azimuth = azimuth * 0.9f + azim * 0.1f
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        onDispose { sensorManager.unregisterListener(sensorEventListener) }
    }

    // Mesafe ve Açı
    val distance = remember(userLocation) {
        if (userLocation == null) 0f
        else {
            val res = FloatArray(1)
            Location.distanceBetween(userLocation.latitude, userLocation.longitude, target.latitude, target.longitude, res)
            res[0]
        }
    }

    val bearingToTarget = remember(userLocation) {
        if (userLocation == null) 0f
        else {
            val targetLoc = Location("target").apply { latitude = target.latitude; longitude = target.longitude }
            userLocation.bearingTo(targetLoc)
        }
    }

    // Oku döndür
    val arrowRotation = bearingToTarget - azimuth
    val animatedRotation by animateFloatAsState(targetValue = arrowRotation, label = "compass")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 150.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(DarkSurface)
                .border(1.dp, CyberTeal.copy(0.3f), RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TACTICAL NAV", color = CyberTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                IconButton(onClick = onStopNavigation, modifier = Modifier.size(24.dp).background(AlertRed.copy(0.2f), CircleShape)) {
                    Icon(Icons.Rounded.Close, null, tint = AlertRed, modifier = Modifier.padding(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = CyberTeal.copy(0.1f), style = Stroke(width = 2.dp.toPx()))
                    for (i in 0..3) {
                        // Canvas içinde DrawScope rotate kullanıyoruz
                        rotate(i * 90f) {
                            drawLine(
                                color = CyberTeal.copy(0.5f),
                                start = Offset(size.width / 2, 0f),
                                end = Offset(size.width / 2, 10.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Rounded.Navigation,
                    contentDescription = null,
                    tint = CyberTeal,
                    modifier = Modifier
                        .size(80.dp)
                        // HATA BURADAYDI: graphicsLayer kullanarak çözdük
                        .graphicsLayer {
                            rotationZ = animatedRotation
                        }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("DISTANCE TO TARGET", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("${distance.toInt()} M", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text(target.name.uppercase(), color = CyberTeal, fontWeight = FontWeight.Bold)
        }
    }
}