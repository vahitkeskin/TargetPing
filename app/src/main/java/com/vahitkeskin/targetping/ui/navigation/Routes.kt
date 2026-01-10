package com.vahitkeskin.targetping.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Dashboard : Screen

    @Serializable
    data object Map : Screen

    @Serializable
    data object List : Screen

    @Serializable
    data class AddEdit(val targetId: String? = null) : Screen
}