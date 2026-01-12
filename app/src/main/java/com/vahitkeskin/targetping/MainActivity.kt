package com.vahitkeskin.targetping

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vahitkeskin.targetping.ui.home.MainScreen
import com.vahitkeskin.targetping.ui.theme.TargetPingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Status bar (Üst) ve Navigation bar (Alt) ikonlarını BEYAZ yapar.
        // Arka planlarını tamamen şeffaf yapar.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            TargetPingTheme {
                // Artık navigasyonu ve gizli modu yöneten MainScreen açılıyor
                MainScreen()
            }
        }
    }
}