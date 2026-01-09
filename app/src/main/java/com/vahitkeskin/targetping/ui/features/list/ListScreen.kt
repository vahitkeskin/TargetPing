package com.vahitkeskin.targetping.ui.features.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vahitkeskin.targetping.ui.components.TargetListItem
import com.vahitkeskin.targetping.ui.home.HomeViewModel

@Composable
fun ListScreen(
    viewModel: HomeViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAdd: () -> Unit
) {
    val targets by viewModel.targets.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Rounded.Add, null) }
        },
        containerColor = Color.Black // Arkaplan
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Başlık Alanı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Targets",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White
                )
                IconButton(onClick = onNavigateToMap) {
                    Icon(Icons.Rounded.Map, null, tint = Color.White)
                }
            }

            // Liste
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(targets, key = { it.id }) { target ->
                    TargetListItem(
                        target = target,
                        onClick = { onNavigateToDetail(target.id) }
                    )
                }
            }
        }

        // Alt tarafa hafif bir gölge (fade) ekleyelim ki liste sonsuza gidiyormuş gibi dursun
        Box(
            modifier = Modifier
                //.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(100.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        )
    }
}