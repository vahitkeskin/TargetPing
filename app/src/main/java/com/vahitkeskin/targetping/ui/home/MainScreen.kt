package com.vahitkeskin.targetping.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vahitkeskin.targetping.ui.features.add_edit.AddEditScreen
import com.vahitkeskin.targetping.ui.features.logs.ActivityLogScreen
import com.vahitkeskin.targetping.ui.features.map.MapScreen
import com.vahitkeskin.targetping.ui.features.settings.SettingsScreen
import com.vahitkeskin.targetping.ui.features.splash.SplashScreen
import com.vahitkeskin.targetping.ui.features.targets.TargetsListScreen
import com.vahitkeskin.targetping.ui.navigation.Screen
import com.vahitkeskin.targetping.ui.theme.PrimaryColor
import com.vahitkeskin.targetping.ui.theme.SurfaceColor
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0.dp), // Tam ekran (Edge-to-Edge)
        bottomBar = { /* BottomBar, DashboardScreen içinde yönetiliyor */ }
    ) { paddingValues ->
        // paddingValues kullanılmıyor çünkü tam ekran istiyoruz,
        // ancak Box içinde padding vermek gerekirse burası kullanılabilir.

        NavHost(
            navController = navController,
            startDestination = Screen.Splash, // DEĞİŞİKLİK: Başlangıç Splash oldu
            modifier = Modifier.fillMaxSize()
        ) {

            // 1. SPLASH EKRANI (YENİ)
            composable<Screen.Splash> {
                SplashScreen(
                    onAnimationFinished = {
                        // Animasyon bittiğinde Dashboard'a git
                        navController.navigate(Screen.Dashboard) {
                            // Geri tuşuna basınca Splash'e dönmemesi için stack'ten siliyoruz
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                )
            }

            // 2. DASHBOARD (ANA EKRAN)
            composable<Screen.Dashboard> {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAdd = { id ->
                        navController.navigate(Screen.AddEdit(targetId = id))
                    }
                )
            }

            // 3. EKLEME / DÜZENLEME EKRANI
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
    // Harita sayfasında (0) swipe'ı engelle, diğerlerinde serbest bırak
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
                            // Işınlanma (Animasyonsuz geçiş)
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
                    // Doğrudan geçiş (Animasyonsuz/Işınlanma)
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
            .background(SurfaceColor.copy(alpha = 0.95f))
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
    // Seçiliyse PrimaryColor, değilse Gri
    val color = if (isSelected) PrimaryColor else Color.Gray

    // Tıklama efektini kaldırmak için gerekli etkileşim kaynağı
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Burayı null yaparak efekti/gölgeyi kapatıyoruz
                onClick = onClick
            )
            .padding(horizontal = 8.dp)
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