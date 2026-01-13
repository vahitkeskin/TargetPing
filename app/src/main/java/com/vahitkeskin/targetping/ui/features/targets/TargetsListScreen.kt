package com.vahitkeskin.targetping.ui.features.targets

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahitkeskin.targetping.domain.model.TargetLocation
import com.vahitkeskin.targetping.ui.components.CompactTargetItem
import com.vahitkeskin.targetping.ui.home.HomeViewModel
import com.vahitkeskin.targetping.ui.theme.AlertRed
import com.vahitkeskin.targetping.ui.theme.PrimaryColor
import kotlinx.coroutines.launch
import com.vahitkeskin.targetping.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TargetsListScreen(
    viewModel: HomeViewModel,
    onNavigateToMap: (TargetLocation) -> Unit,
    onEditTarget: (TargetLocation) -> Unit
) {
    val targets by viewModel.targets.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- YEREL STATE'LER (Arama ve Sıralama) ---
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.NAME) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Listeyi filtrele ve sırala
    val filteredTargets = remember(targets, searchQuery, sortOption) {
        targets.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }.sortedWith(
            when (sortOption) {
                SortOption.NAME -> compareBy { it.name }
                SortOption.ACTIVE -> compareByDescending { it.isActive }
                SortOption.RADIUS -> compareByDescending { it.radiusMeters }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent, // Arka plan gradient olacak
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0.dp) // Edge-to-Edge
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkBackground, Color.Black)
                    )
                )
                .padding(padding) // Scaffold padding'i uygula ama üstten Spacer ile ayarla
        ) {
            // 1. ÜST BOŞLUK (Status Bar için)
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            // 2. BAŞLIK VE İSTATİSTİK
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HEDEFLER", // TÜRKÇE
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Sıralama Butonu
                    // ... (Önceki kodlar)

// Sıralama Butonu
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Rounded.Sort, null, tint = PrimaryColor)
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            // containerColor parametresini sildik ve modifier ile arka plan verdik:
                            modifier = Modifier.background(SurfaceColor)
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label, color = Color.White) },
                                    onClick = {
                                        sortOption = option
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOption == option) {
                                            Icon(Icons.Rounded.Check, null, tint = PrimaryColor)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "${filteredTargets.size} SONUÇ | ${targets.count { it.isActive }} AKTİF", // TÜRKÇE
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }

            // 3. ARAMA ÇUBUĞU
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Hedef ara...", color = Color.Gray) }, // TÜRKÇE
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Gray) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, null, tint = Color.Gray)
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryColor,
                    unfocusedBorderColor = Color.DarkGray,
                    cursorColor = PrimaryColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // 4. LİSTE
            if (filteredTargets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.SearchOff,
                            contentDescription = null,
                            tint = Color.DarkGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Hedef bulunamadı", color = Color.Gray) // TÜRKÇE
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredTargets,
                        key = { it.id }
                    ) { target ->
                        // Swipe-to-Delete Kutusu
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    // Silme İşlemi ve Undo Snackbar
                                    viewModel.deleteTarget(target.id)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "${target.name} silindi", // TÜRKÇE
                                            actionLabel = "GERİ AL", // TÜRKÇE
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.addTarget(
                                                target.name,
                                                target.latitude,
                                                target.longitude,
                                                target.radiusMeters
                                            )
                                        }
                                    }
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color =
                                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                                        AlertRed else Color.Transparent

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(color)
                                        .padding(end = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Rounded.Delete, null, tint = Color.White)
                                }
                            },
                            content = {
                                CompactTargetItem(
                                    modifier = Modifier.animateItemPlacement(tween(300)), // Animasyon
                                    target = target,
                                    userLocation = null, // Eğer ViewModel'de varsa buraya ekle
                                    onToggle = {
                                        viewModel.toggleTargetActive(
                                            target.id,
                                            !target.isActive
                                        )
                                    },
                                    onClick = { onEditTarget(target) }
                                )
                            },
                            enableDismissFromStartToEnd = false // Sadece sağdan sola kaydırma
                        )
                    }
                }
            }
        }
    }
}

// Sıralama Seçenekleri Enum'ı (TÜRKÇE)
enum class SortOption(val label: String) {
    NAME("İsim (A-Z)"),
    ACTIVE("Durum (Önce Aktif)"),
    RADIUS("Yarıçap (Önce Büyük)")
}