package com.vahitkeskin.targetping.ui.features.stealth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorScreen(
    onUnlock: () -> Unit
) {
    var input by remember { mutableStateOf("0") }
    val buttons = listOf(
        listOf("7", "8", "9", "/"),
        listOf("4", "5", "6", "x"),
        listOf("1", "2", "3", "-"),
        listOf("C", "0", "=", "+")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Ekran
        Text(
            text = input,
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        // Tuşlar
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { label ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(
                                if (label == "=") Color(0xFFFF9800) else Color(0xFF333333),
                                CircleShape
                            )
                            .clickable {
                                when (label) {
                                    "C" -> input = "0"
                                    "=" -> {
                                        if (input == "1234") { // ŞİFRE BURADA
                                            onUnlock()
                                        } else {
                                            input = "Error"
                                        }
                                    }
                                    else -> {
                                        if (input == "0" || input == "Error") input = label
                                        else input += label
                                    }
                                }
                            }
                    ) {
                        Text(text = label, color = Color.White, fontSize = 24.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}