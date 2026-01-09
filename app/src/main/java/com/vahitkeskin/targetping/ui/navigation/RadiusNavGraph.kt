package com.vahitkeskin.targetping.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vahitkeskin.targetping.ui.features.add_edit.AddEditScreen
import com.vahitkeskin.targetping.ui.features.list.ListScreen
import com.vahitkeskin.targetping.ui.features.map.MapScreen
import com.vahitkeskin.targetping.ui.home.HomeViewModel

@Composable
fun RadiusNavGraph() {
    val navController = rememberNavController()
    // ViewModel'i graph seviyesinde tutuyoruz ki veriler ekranlar arası korunsum
    val sharedViewModel: HomeViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Map
    ) {
        // 1. HARİTA EKRANI
        composable<Screen.Map> {
            MapScreen(
                viewModel = sharedViewModel,
                onNavigateToList = { navController.navigate(Screen.List) },
                onNavigateToAdd = { latLng ->
                    // Konum seçerek eklemeye git
                    // Burada lat/lng parametre olarak geçilebilir veya VM'de tutulabilir
                    navController.navigate(Screen.AddEdit())
                }
            )
        }

        // 2. LİSTE EKRANI
        composable<Screen.List> {
            ListScreen(
                viewModel = sharedViewModel,
                onNavigateToMap = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.AddEdit(targetId = id)) },
                onNavigateToAdd = { navController.navigate(Screen.AddEdit()) }
            )
        }

        // 3. EKLEME / DÜZENLEME EKRANI
        composable<Screen.AddEdit> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.AddEdit>()
            AddEditScreen(
                viewModel = sharedViewModel,
                targetId = args.targetId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}