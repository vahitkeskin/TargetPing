package com.vahitkeskin.targetping.ui.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    data object Splash : Screen()

    @Serializable
    object Dashboard : Screen()

    @Serializable
    data class AddEdit(val targetId: String?) : Screen()

    // Düzeltme: object tanımlarında : Screen() parantezleri eklendi
    @Serializable
    object Logs : Screen()

    @Serializable
    object Settings : Screen()
}