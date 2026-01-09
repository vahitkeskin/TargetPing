package com.vahitkeskin.targetping.utils

import android.content.Context
import android.content.res.Resources
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Jetpack Compose içerisindeki Navigation Bar yüksekliğini Dp cinsinden verir.
 * Kullanımı: val navHeight = rememberNavigationBarHeight()
 */
@Composable
fun rememberNavigationBarHeight(): Dp {
    return WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}

/**
 * Alternatif: Eğer bir Context üzerinden (Composable dışından) almak istersen.
 * Not: Bu yöntem Edge-to-Edge modunda bazen 0 dönebilir veya hatalı olabilir.
 * Compose içindeysen üstteki 'rememberNavigationBarHeight' fonksiyonunu kullan.
 */
fun Context.getNavigationBarHeightDp(): Dp {
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        val px = resources.getDimensionPixelSize(resourceId)
        (px / resources.displayMetrics.density).dp
    } else {
        0.dp
    }
}

/**
 * Px (Piksel) değerini Dp'ye çeviren Extension
 */
val Int.toDp: Dp
    get() = (this / Resources.getSystem().displayMetrics.density).dp

/**
 * Dp değerini Px'e çeviren Extension
 */
val Dp.toPx: Float
    get() = this.value * Resources.getSystem().displayMetrics.density