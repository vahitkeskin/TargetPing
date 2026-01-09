package com.vahitkeskin.targetping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.vahitkeskin.targetping.ui.navigation.RadiusNavGraph
import com.vahitkeskin.targetping.ui.theme.RadiusAlertTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Sihirli Kod: Sistem barlarının arkasına çizim yapmamızı sağlar
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            RadiusAlertTheme {
                // 2. Status Bar ikon renklerini ayarla
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = !isSystemInDarkTheme()

                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent, // Tamamen saydam
                        darkIcons = useDarkIcons
                    )
                }

                // 3. Navigasyon Başlatıcı
                RadiusNavGraph()
            }
        }
    }
}