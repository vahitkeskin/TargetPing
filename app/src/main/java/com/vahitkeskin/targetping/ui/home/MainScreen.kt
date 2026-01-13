package com.vahitkeskin.targetping.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // Font boyutu ayarı için eklendi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vahitkeskin.targetping.ui.features.add_edit.AddEditScreen
import com.vahitkeskin.targetping.ui.features.logs.ActivityLogScreen
import com.vahitkeskin.targetping.ui.features.map.MapScreen
import com.vahitkeskin.targetping.ui.features.settings.SettingsScreen
import com.vahitkeskin.targetping.ui.features.targets.TargetsListScreen
import com.vahitkeskin.targetping.ui.navigation.Screen
import com.vahitkeskin.targetping.utils.uninstallSelf
import kotlinx.coroutines.launch

private val CyberTeal = Color(0xFF00E5FF)
private val GlassBackground = Color(0xFF1E1E1E).copy(alpha = 0.95f)

@Composable
fun MainScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    // Hesap Makinesi yok, direkt uygulama açılıyor.
    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0.dp), // Tam ekran (Edge-to-Edge)
        bottomBar = { /* BottomBar, DashboardScreen içinde yönetiliyor */ }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard,
            modifier = Modifier.fillMaxSize()
        ) {
            composable<Screen.Dashboard> {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAdd = { id ->
                        navController.navigate(Screen.AddEdit(targetId = id))
                    }
                )
            }

            composable<Screen.AddEdit> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.AddEdit>()
                AddEditScreen(
                    viewModel = viewModel,
                    targetId = args.targetId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: HomeViewModel,
    onNavigateToAdd: (String?) -> Unit,
) {
    // 4 Sayfalı Yapı
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val isScrollEnabled = pagerState.currentPage != 0

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. İÇERİK (Pager)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = isScrollEnabled
        ) { page ->
            when (page) {
                0 -> {
                    // İZİN LOJİKLERİ BURADA (MapScreen içinde) KORUNUYOR
                    MapScreen(
                        viewModel = viewModel,
                        onNavigateToAdd = { _ -> onNavigateToAdd(null) }
                    )
                }
                1 -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TargetsListScreen(
                            viewModel = viewModel,
                            onNavigateToMap = { scope.launch { pagerState.scrollToPage(0) } },
                            onEditTarget = { target -> onNavigateToAdd(target.id) }
                        )
                    }
                }
                2 -> ActivityLogScreen(viewModel)
                3 -> SettingsScreen(viewModel)
            }
        }

        // 2. ALT MENÜ
        GlassBottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            currentPage = pagerState.currentPage,
            onTabSelected = { index ->
                scope.launch {
                    // ESKİSİ: pagerState.animateScrollToPage(index)
                    // YENİSİ: Doğrudan geçiş (Animasyonsuz/Işınlanma)
                    pagerState.scrollToPage(index)
                }
            }
        )
    }
}

@Composable
fun GlassBottomNavigation(
    modifier: Modifier = Modifier,
    currentPage: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(GlassBackground)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CyberNavItem(Icons.Rounded.Map, "HARİTA", currentPage == 0) { onTabSelected(0) }
            CyberNavItem(Icons.Rounded.List, "LİSTE", currentPage == 1) { onTabSelected(1) }
            CyberNavItem(Icons.Rounded.History, "GÜNLÜK", currentPage == 2) { onTabSelected(2) }
            CyberNavItem(Icons.Rounded.Settings, "AYARLAR", currentPage == 3) { onTabSelected(3) }
        }
    }
}

@Composable
fun CyberNavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    // Seçiliyse CyberTeal, değilse Gri
    val color = if (isSelected) CyberTeal else Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 8.dp) // Alanı biraz daralttık ki sığsın
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = color
        )
    }
}