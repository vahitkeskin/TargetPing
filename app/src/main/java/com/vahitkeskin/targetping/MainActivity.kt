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

        // KRİTİK AYAR: Hem üst hem alt barı tamamen şeffaf yapıyoruz.
        // SystemBarStyle.dark kullanıyoruz ki ikonlar (saat vs) beyaz olsun.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            TargetPingTheme {
                MainScreen()
            }
        }
    }
}