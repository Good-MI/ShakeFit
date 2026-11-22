package com.example.shakefit.ui.screens.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shakefit.R
import com.example.shakefit.ui.screens.history.HistoryRoute
import com.example.shakefit.ui.screens.history.HistoryViewModel
import com.example.shakefit.ui.screens.stats.StatsRoute
import com.example.shakefit.ui.screens.stats.StatsViewModel

/**
 * ActivityScreen: Wadah Tab 3 (Aktivitas) yang membagi tampilan menjadi segmen "Riwayat" dan "Statistik".
 * Sesuai DesignShakefit.md Bagian 5.2.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    historyViewModel: HistoryViewModel,
    statsViewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        // Tab Row Segmen
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Text(
                        text = stringResource(R.string.tab_history),
                        fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                    )
                },
                icon = { Icon(Icons.Filled.DateRange, contentDescription = null) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Text(
                        text = stringResource(R.string.tab_stats),
                        fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                    )
                },
                icon = { Icon(Icons.Filled.Star, contentDescription = null) }
            )
        }

        // Tampilan Segmen Terpilih
        when (selectedTabIndex) {
            0 -> HistoryRoute(viewModel = historyViewModel, modifier = Modifier.fillMaxSize())
            1 -> StatsRoute(viewModel = statsViewModel, modifier = Modifier.fillMaxSize())
        }
    }
}
