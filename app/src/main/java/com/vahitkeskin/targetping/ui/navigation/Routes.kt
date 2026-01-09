package com.vahitkeskin.targetping.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Map : Screen // Ana Harita

    @Serializable
    data object List : Screen // Liste Ekranı

    @Serializable
    data class AddEdit(val targetId: String? = null) : Screen // Ekleme/Düzenleme (ID opsiyonel)
}