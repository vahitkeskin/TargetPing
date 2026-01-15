package com.vahitkeskin.targetping.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.google.maps.android.compose.MapType
import com.vahitkeskin.targetping.R

enum class MapStyleConfig(
    val title: String,
    @RawRes val jsonResId: Int?,
    @DrawableRes val previewImage: Int,
    val storageKey: String,
    val mapType: MapType = MapType.NORMAL
) {
    HYBRID("Uydu", null, R.drawable.map_style_theme_hybrid, "HYBRID", MapType.HYBRID),
    STANDARD("Standart", null, R.drawable.map_style_theme_standart, "STANDARD"),
    SILVER("Gümüş", R.raw.map_style_silver, R.drawable.map_style_theme_silver, "SILVER"),
    RETRO("Retro", R.raw.map_style_retro, R.drawable.map_style_theme_retro, "RETRO"),
    DARK("Karanlık", R.raw.map_style_dark, R.drawable.map_style_theme_dark, "DARK"),
    NIGHT("Gece", R.raw.map_style_night, R.drawable.map_style_theme_night, "NIGHT"),
    AUBERGINE(
        "Siber",
        R.raw.map_style_aubergine,
        R.drawable.map_style_theme_aubergine,
        "AUBERGINE"
    );

    companion object {
        fun fromKey(key: String): MapStyleConfig {
            return entries.find { it.storageKey == key } ?: STANDARD
        }
    }
}