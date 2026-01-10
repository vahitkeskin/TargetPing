package com.vahitkeskin.targetping.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vahitkeskin.targetping.ui.features.add_edit.AddEditScreen
import com.vahitkeskin.targetping.ui.features.map.MapScreen
import com.vahitkeskin.targetping.ui.features.targets.TargetsListScreen
import com.vahitkeskin.targetping.ui.navigation.Screen
import kotlinx.coroutines.launch

private val CyberTeal = Color(0xFF00E5FF)
private val GlassBackground = Color(0xFF1E1E1E).copy(alpha = 0.95f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isBottomBarVisible = currentDestination?.hasRoute<Screen.Dashboard>() == true

    Scaffold(
        containerColor = Color.Black,
        // DİKKAT: Ana Scaffold'a da insets'leri sıfırla diyoruz.
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            // Burası boş, Dashboard içinde yönetiyoruz.
        }
    ) { paddingValues ->
        // paddingValues'u bilerek kullanmıyoruz (yoksayıyoruz)
        // böylece içerik ekranın en tepesinden başlıyor.

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
    onNavigateToAdd: (String?) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    // MANTIK: Eğer şu anki sayfa 0 (Harita) ise kaydırmayı (userScroll) KAPAT.
    // Değilse (yani Liste ise) AÇ.
    val isScrollEnabled = pagerState.currentPage != 0

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            GlassBottomNavigation(
                currentPage = pagerState.currentPage,
                onTabSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                }
            )
        }
    ) { padding ->

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // KRİTİK AYAR BURASI:
            userScrollEnabled = isScrollEnabled
        ) { page ->
            when (page) {
                0 -> {
                    // HARİTA
                    MapScreen(
                        viewModel = viewModel,
                        onNavigateToAdd = { _ -> onNavigateToAdd(null) }
                    )
                }
                1 -> {
                    // LİSTE
                    Box(modifier = Modifier.fillMaxSize()) {
                        TargetsListScreen(
                            viewModel = viewModel,
                            onNavigateToMap = { scope.launch { pagerState.animateScrollToPage(0) } },
                            onEditTarget = { target -> onNavigateToAdd(target.id) }
                        )
                    }
                }
            }
        }
    }
}

// --- GlassBottomNavigation ve CyberNavItem KODLARI AYNIDIR ---
// (Önceki cevaptaki kodların aynısını buraya ekleyebilirsin)
// Yer kaplamaması için tekrar yazmıyorum, kopyaladığın son hali geçerlidir.
@Composable
fun GlassBottomNavigation(
    currentPage: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
            .height(70.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(GlassBackground)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CyberNavItem(Icons.Rounded.Map, "HARİTA", currentPage == 0) { onTabSelected(0) }
            Box(Modifier.width(1.dp).height(24.dp).background(Color.White.copy(0.1f)))
            CyberNavItem(Icons.Rounded.List, "LİSTE", currentPage == 1) { onTabSelected(1) }
        }
    }
}

@Composable
fun CyberNavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) CyberTeal else Color.Gray
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxHeight().clip(CircleShape).clickable { onClick() }.padding(horizontal = 24.dp)
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size(26.dp))
        if (isSelected) { Spacer(Modifier.height(4.dp)); Text(label, style = MaterialTheme.typography.labelSmall, color = color) }
    }
}