package com.vahitkeskin.targetping.ui.home

import androidx.compose.animation.Crossfade
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vahitkeskin.targetping.ui.features.add_edit.AddEditScreen
import com.vahitkeskin.targetping.ui.features.map.MapScreen
import com.vahitkeskin.targetping.ui.features.stealth.CalculatorScreen
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
    val isStealthMode by viewModel.isStealthMode.collectAsState()
    val navController = rememberNavController()

    Crossfade(targetState = isStealthMode, label = "StealthTransition") { stealthActive ->
        if (stealthActive) {
            // Gizli Mod
            CalculatorScreen(
                onUnlock = { viewModel.setStealthMode(false) }
            )
        } else {
            // Normal Mod
            Scaffold(
                containerColor = Color.Black,
                // KRİTİK AYAR 1: İçeriğin en tepeden (Status bar arkasından) başlamasını sağlar
                contentWindowInsets = WindowInsets(0.dp),
                bottomBar = { /* Dashboard içinde yönetiliyor */ }
            ) { paddingValues ->
                // paddingValues bilerek kullanılmıyor (Fullscreen)
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
                            },
                            onPanic = { viewModel.setStealthMode(true) }
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: HomeViewModel,
    onNavigateToAdd: (String?) -> Unit,
    onPanic: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val isScrollEnabled = pagerState.currentPage != 0

    // Scaffold yerine Box kullanıyoruz ki tam kontrol bizde olsun.
    // Böylece harita ve liste tam ekran yayılırken, BottomBar onların "üzerinde" yüzer.
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. İÇERİK (Pager)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = isScrollEnabled
        ) { page ->
            when (page) {
                0 -> {
                    // HARİTA (Tam Ekran)
                    MapScreen(
                        viewModel = viewModel,
                        onNavigateToAdd = { _ -> onNavigateToAdd(null) }
                        // MapScreen içine onPanic eklediysen buraya: onPanic = onPanic
                    )
                }
                1 -> {
                    // LİSTE (Tam Ekran - Status Bar arkasından başlar)
                    // NOT: TargetsListScreen içinde Scaffold varsa onun da
                    // contentWindowInsets = WindowInsets(0.dp) olduğundan emin ol.
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

        // 2. ALT MENÜ (Glass Bottom Bar)
        // Box içinde en alta hizalıyoruz.
        GlassBottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            currentPage = pagerState.currentPage,
            onTabSelected = { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
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
            // KRİTİK AYAR 2: Navigation Bar (Sistem alt çizgisi) kadar yukarı iter.
            // Böylece Bottom Bar sistem çizgisinin üstünde kalır.
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp) // Görsel boşluk
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
        modifier = Modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 24.dp)
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size(26.dp))
        if (isSelected) {
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}