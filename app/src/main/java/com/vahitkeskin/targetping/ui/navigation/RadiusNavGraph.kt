package com.vahitkeskin.targetping.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vahitkeskin.targetping.ui.home.AddEditScreen
import com.vahitkeskin.targetping.ui.features.list.ListScreen
import com.vahitkeskin.targetping.ui.features.map.MapScreen
import com.vahitkeskin.targetping.ui.features.targets.TargetsListScreen
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
                // HATA DÜZELTİLDİ: onNavigateToList parametresi SİLİNDİ.
                // Artık listeye gitmek için alt menüyü kullanıyoruz.
                onNavigateToAdd = { latLng ->
                    // Yeni bir hedef eklemek için (ID yok)
                    navController.navigate(Screen.AddEdit(targetId = null))
                }
            )
        }

        // 2. LİSTE EKRANI
        composable<Screen.List> {
            // İSİM DÜZELTİLDİ: ListScreen -> TargetsListScreen
            TargetsListScreen(
                viewModel = sharedViewModel,
                onNavigateToMap = {
                    // Haritaya geri dön
                    navController.navigate(Screen.Map) {
                        popUpTo(Screen.Map) { inclusive = true }
                    }
                },
                onEditTarget = { target ->
                    // Düzenlemek için ID gönderiyoruz
                    navController.navigate(Screen.AddEdit(targetId = target.id))
                }
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